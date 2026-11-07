import { env } from '../config.js';
import {
  fuseRankings,
  keywordScore,
  tokenize,
  type RankedChunk,
} from '../helpers/hybridRetrieval.js';
import {
  geminiInsightsSchema,
  type GeminiInsights,
  type InsightsRequest,
} from '../models/insightsSchema.js';
import { prisma } from '../models/db.js';
import { JobStatus } from '../models/enums.js';
import {
  buildEmbedding,
  findKeywordCandidates,
  findSimilar,
} from './ragEmbeddingService.js';

function clamp(value: number | undefined, fallback: number, max: number): number {
  if (value == null || Number.isNaN(value)) return fallback;
  return Math.min(Math.max(1, value), max);
}

function extractJsonObject(text: string): unknown {
  const fenced = text.match(/```(?:json)?\s*([\s\S]*?)```/i);
  const raw = fenced?.[1]?.trim() ?? text.trim();
  const start = raw.indexOf('{');
  const end = raw.lastIndexOf('}');
  if (start === -1 || end === -1 || end <= start) {
    throw new Error('No JSON object in model response');
  }
  return JSON.parse(raw.slice(start, end + 1));
}

async function callGeminiJson(prompt: string): Promise<GeminiInsights | null> {
  if (!env.GEMINI_API_KEY) {
    return null;
  }

  const url = `${env.GEMINI_BASE_URL.replace(/\/$/, '')}/models/${env.GEMINI_MODEL}:generateContent?key=${env.GEMINI_API_KEY}`;
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      systemInstruction: {
        parts: [
          {
            text: `${env.GEMINI_SYSTEM_PROMPT}
Return ONLY a JSON object with keys: summary (string), findings (string[]), recommendations (string[]), suspectedHandlers (string[]).
Cite concrete job ids and handlers when possible. Do not wrap in markdown unless necessary.`,
          },
        ],
      },
      contents: [{ role: 'user', parts: [{ text: prompt }] }],
      generationConfig: {
        responseMimeType: 'application/json',
      },
    }),
  });

  if (!response.ok) {
    return null;
  }

  const body = (await response.json()) as {
    candidates?: { content?: { parts?: { text?: string }[] } }[];
  };
  const text = body.candidates?.[0]?.content?.parts?.map((p) => p.text ?? '').join('') ?? '';
  if (!text) {
    return null;
  }

  try {
    return geminiInsightsSchema.parse(extractJsonObject(text));
  } catch {
    return null;
  }
}

function buildRuleBasedInsights(input: {
  jobsAnalyzed: number;
  hours: number;
  failedJobs: { id: number; handler: string; error: string | null }[];
  jobsByStatus: Record<string, number>;
  jobsByHandler: Record<string, number>;
}): GeminiInsights {
  const failed = input.failedJobs;
  const topHandlers = Object.entries(input.jobsByHandler)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 3)
    .map(([handler]) => handler);

  return {
    summary: `Analyzed ${input.jobsAnalyzed} jobs in the last ${input.hours}h. ${failed.length} failed/dead jobs found. Status mix: ${JSON.stringify(input.jobsByStatus)}.`,
    findings: [
      `Jobs by status: ${JSON.stringify(input.jobsByStatus)}`,
      `Top handlers: ${JSON.stringify(input.jobsByHandler)}`,
      failed.length
        ? `${failed.length} failed/dead jobs need attention`
        : 'No failed/dead jobs in the window',
    ],
    recommendations: failed.length
      ? [
          'Inspect failed job error messages and recent retries',
          'Check RabbitMQ DLQs and worker consumer logs for those handlers',
        ]
      : ['System looks healthy in the selected window'],
    suspectedHandlers: failed.length
      ? [...new Set(failed.map((j) => j.handler))]
      : topHandlers,
  };
}

function countLogLevels(chunks: RankedChunk[]): Record<string, number> {
  const levels: Record<string, number> = {};
  for (const chunk of chunks) {
    const match = chunk.content.match(/\b(TRACE|DEBUG|INFO|WARN|ERROR)\b/);
    const level = match?.[1] ?? 'UNKNOWN';
    levels[level] = (levels[level] ?? 0) + 1;
  }
  return levels;
}

function topEvents(chunks: RankedChunk[], limit = 5): { event: string; count: number }[] {
  const counts = new Map<string, number>();
  for (const chunk of chunks) {
    const match = chunk.content.match(/event=([a-z0-9._-]+)/i);
    const key = match?.[1] ?? chunk.sourceName;
    counts.set(key, (counts.get(key) ?? 0) + 1);
  }
  return [...counts.entries()]
    .map(([event, count]) => ({ event, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, limit);
}

export async function generateInsights(request: InsightsRequest = {}) {
  const hours = clamp(request.hours, 24, 720);
  const jobLimit = clamp(request.jobLimit, 100, 500);
  const since = new Date(Date.now() - hours * 60 * 60 * 1000);
  const question =
    request.question?.trim() ||
    (request.jobId
      ? `What went wrong with job ${request.jobId}?`
      : 'What operational issues should I look at right now?');

  const jobs = await prisma.job.findMany({
    where: request.jobId
      ? { id: BigInt(request.jobId) }
      : { updatedAt: { gte: since } },
    orderBy: { updatedAt: 'desc' },
    take: request.jobId ? 1 : jobLimit,
  });

  const jobsByStatus: Record<string, number> = {};
  const jobsByHandler: Record<string, number> = {};
  const jobsByCategory: Record<string, number> = {};
  for (const job of jobs) {
    jobsByStatus[job.status] = (jobsByStatus[job.status] ?? 0) + 1;
    jobsByHandler[job.jobHandler] = (jobsByHandler[job.jobHandler] ?? 0) + 1;
    jobsByCategory[job.jobCategory] = (jobsByCategory[job.jobCategory] ?? 0) + 1;
  }

  const failedJobs = jobs
    .filter((j) => j.status === JobStatus.ERROR || j.status === JobStatus.DEAD)
    .slice(0, 20)
    .map((j) => ({
      id: Number(j.id),
      status: j.status,
      handler: j.jobHandler,
      error: j.errorMessage,
    }));

  const focusJobIds = request.jobId
    ? [request.jobId]
    : failedJobs.map((j) => j.id);

  const focusHandlers = [
    ...new Set(
      (request.jobId
        ? jobs.map((j) => j.jobHandler)
        : failedJobs.map((j) => j.handler)
      ).filter(Boolean),
    ),
  ];

  const queryTokens = tokenize(question);
  const queryEmbedding = await buildEmbedding(question);

  const vectorHits = await findSimilar(queryEmbedding.vectorLiteral, {
    limit: 20,
    since,
    jobIds: focusJobIds.length ? focusJobIds : undefined,
    handlers: focusJobIds.length ? undefined : focusHandlers.length ? focusHandlers : undefined,
    geminiOnly: true,
  });

  // If job-scoped vector search is empty, widen to handlers / global window.
  const widenedVector =
    vectorHits.length > 0
      ? vectorHits
      : await findSimilar(queryEmbedding.vectorLiteral, {
          limit: 20,
          since,
          geminiOnly: true,
        });

  const keywordDocs = await findKeywordCandidates({
    tokens: queryTokens,
    since,
    jobIds: focusJobIds.length ? focusJobIds : undefined,
    limit: 20,
  });

  const errorDocs: RankedChunk[] = failedJobs
    .filter((j) => j.error)
    .map((j, index) => ({
      id: -1000 - index,
      content: `ERROR jobId=${j.id} handler=${j.handler} error=${j.error}`,
      sourceType: 'job_error',
      sourceName: j.handler,
      requestId: null,
      jobId: j.id,
      handler: j.handler,
      vectorScore: 0,
      keywordScore: keywordScore(queryTokens, `${j.handler} ${j.error}`),
      score: 0,
    }))
    .filter((d) => d.keywordScore > 0 || focusJobIds.includes(d.jobId!));

  const vectorRanked: RankedChunk[] = widenedVector.map((hit) => ({
    id: hit.id,
    content: hit.content,
    sourceType: hit.sourceType,
    sourceName: hit.sourceName,
    requestId: hit.requestId,
    jobId: hit.jobId,
    handler: hit.handler,
    vectorScore: hit.similarity,
    keywordScore: keywordScore(queryTokens, hit.content),
    score: 0,
  }));

  const keywordRanked: RankedChunk[] = [
    ...keywordDocs.map((hit) => ({
      id: hit.id,
      content: hit.content,
      sourceType: hit.sourceType,
      sourceName: hit.sourceName,
      requestId: hit.requestId,
      jobId: hit.jobId,
      handler: hit.handler,
      vectorScore: 0,
      keywordScore: keywordScore(queryTokens, hit.content),
      score: 0,
    })),
    ...errorDocs,
  ].sort((a, b) => b.keywordScore - a.keywordScore);

  const retrieved = fuseRankings(vectorRanked, keywordRanked, 8);

  const contextBlock = [
    `Question: ${question}`,
    `Jobs analyzed: ${jobs.length}`,
    `Status counts: ${JSON.stringify(jobsByStatus)}`,
    `Failed jobs: ${JSON.stringify(failedJobs)}`,
    `Retrieved evidence:\n${retrieved
      .map(
        (c, i) =>
          `[${i + 1}] jobId=${c.jobId ?? '-'} handler=${c.handler ?? '-'} score=${c.score.toFixed(4)} :: ${c.content}`,
      )
      .join('\n')}`,
  ].join('\n\n');

  const ai = await callGeminiJson(contextBlock);
  const usedAi = Boolean(ai);
  const grounded =
    ai ??
    buildRuleBasedInsights({
      jobsAnalyzed: jobs.length,
      hours,
      failedJobs,
      jobsByStatus,
      jobsByHandler,
    });

  const topErrors = Object.entries(
    failedJobs.reduce<Record<string, number>>((acc, job) => {
      if (!job.error) return acc;
      acc[job.error] = (acc[job.error] ?? 0) + 1;
      return acc;
    }, {}),
  )
    .map(([message, count]) => ({ message, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5);

  return {
    summary: grounded.summary,
    answer: grounded.summary,
    findings: grounded.findings,
    recommendations: grounded.recommendations,
    suspectedHandlers: grounded.suspectedHandlers,
    metrics: {
      jobsAnalyzed: jobs.length,
      logsAnalyzed: retrieved.length,
      jobsByStatus,
      jobsByHandler,
      jobsByCategory,
      logLevels: countLogLevels(retrieved),
      topEvents: topEvents(retrieved),
      topErrors,
    },
    source: {
      generatedAt: new Date().toISOString(),
      usedAiModel: usedAi,
      model: usedAi ? env.GEMINI_MODEL : 'rule-based',
      hours,
      logLinesRequested: request.logLines ?? null,
      jobLimitRequested: jobLimit,
      focusJobIds,
      retrievalEnabled: true,
      retrievedChunks: retrieved.length,
      hybridRetrieval: true,
      embeddingModel: queryEmbedding.model,
    },
    context: request.includeRawContext
      ? {
          failedJobs,
          recentErrors: failedJobs,
          retrievedChunks: retrieved,
        }
      : undefined,
  };
}
