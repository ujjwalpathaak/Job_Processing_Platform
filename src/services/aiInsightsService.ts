import { readFile, readdir } from 'node:fs/promises';
import path from 'node:path';
import { env } from '../config.js';
import { prisma } from '../models/db.js';
import { JobStatus } from '../models/enums.js';
import { buildEmbedding, findSimilar } from './ragEmbeddingService.js';

type InsightsRequest = {
  hours?: number;
  logLines?: number;
  jobLimit?: number;
  question?: string;
  includeRawContext?: boolean;
};

function clamp(value: number | undefined, fallback: number, max: number): number {
  if (value == null || Number.isNaN(value)) return fallback;
  return Math.min(Math.max(1, value), max);
}

async function callGemini(prompt: string): Promise<string | null> {
  if (!env.GEMINI_API_KEY) {
    return null;
  }
  const url = `${env.GEMINI_BASE_URL.replace(/\/$/, '')}/models/${env.GEMINI_MODEL}:generateContent?key=${env.GEMINI_API_KEY}`;
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      systemInstruction: { parts: [{ text: env.GEMINI_SYSTEM_PROMPT }] },
      contents: [{ role: 'user', parts: [{ text: prompt }] }],
    }),
  });
  if (!response.ok) {
    console.error(`Gemini chat failed: ${response.status}`);
    return null;
  }
  const body = (await response.json()) as {
    candidates?: { content?: { parts?: { text?: string }[] } }[];
  };
  return body.candidates?.[0]?.content?.parts?.map((p) => p.text ?? '').join('') ?? null;
}

async function readRecentLogs(logLines: number): Promise<string[]> {
  try {
    const files = (await readdir(env.LOG_DIR)).filter((f) => f.endsWith('.log')).sort().reverse();
    const lines: string[] = [];
    for (const file of files) {
      const content = await readFile(path.join(env.LOG_DIR, file), 'utf8');
      const fileLines = content.trim().split('\n').filter(Boolean).reverse();
      for (const line of fileLines) {
        lines.push(line);
        if (lines.length >= logLines) {
          return lines;
        }
      }
    }
    return lines;
  } catch {
    return [];
  }
}

export async function generateInsights(request: InsightsRequest = {}) {
  const hours = clamp(request.hours, 24, 720);
  const logLines = clamp(request.logLines, 120, 1000);
  const jobLimit = clamp(request.jobLimit, 100, 500);
  const since = new Date(Date.now() - hours * 60 * 60 * 1000);

  const jobs = await prisma.job.findMany({
    where: { updatedAt: { gte: since } },
    orderBy: { updatedAt: 'desc' },
    take: jobLimit,
  });

  const logs = await readRecentLogs(logLines);
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
    .slice(0, 10)
    .map((j) => ({
      id: Number(j.id),
      status: j.status,
      handler: j.jobHandler,
      error: j.errorMessage,
    }));

  const question = request.question?.trim() || 'What operational issues should I look at right now?';
  const embedding = await buildEmbedding(question);
  const retrieved = await findSimilar(embedding, 8);

  const contextBlock = [
    `Jobs analyzed: ${jobs.length}`,
    `Status counts: ${JSON.stringify(jobsByStatus)}`,
    `Failed jobs: ${JSON.stringify(failedJobs)}`,
    `Recent logs: ${logs.slice(0, 20).join('\n')}`,
    `Retrieved chunks: ${retrieved.map((c) => c.content).join('\n')}`,
  ].join('\n\n');

  const aiText = await callGemini(`${question}\n\nContext:\n${contextBlock}`);
  const usedAi = Boolean(aiText);

  const summary =
    aiText ??
    `Analyzed ${jobs.length} jobs in the last ${hours}h. ${failedJobs.length} failed/dead jobs found. Status mix: ${JSON.stringify(jobsByStatus)}.`;

  const findings = [
    `Jobs by status: ${JSON.stringify(jobsByStatus)}`,
    `Top handlers: ${JSON.stringify(jobsByHandler)}`,
    failedJobs.length
      ? `${failedJobs.length} failed/dead jobs need attention`
      : 'No failed/dead jobs in the window',
  ];

  const recommendations = failedJobs.length
    ? ['Inspect DEAD/ERROR jobs and retry policy', 'Check RabbitMQ DLQs and consumer logs']
    : ['System looks healthy in the selected window'];

  return {
    summary,
    answer: aiText ?? summary,
    findings,
    recommendations,
    metrics: {
      jobsAnalyzed: jobs.length,
      logsAnalyzed: logs.length,
      jobsByStatus,
      jobsByHandler,
      jobsByCategory,
      logLevels: {},
      topEvents: [],
      topErrors: failedJobs
        .filter((j) => j.error)
        .slice(0, 5)
        .map((j) => ({ message: j.error, count: 1 })),
    },
    source: {
      generatedAt: new Date().toISOString(),
      usedAiModel: usedAi,
      model: usedAi ? env.GEMINI_MODEL : 'rule-based',
      hours,
      logLinesRequested: logLines,
      jobLimitRequested: jobLimit,
      logFiles: [],
      retrievalEnabled: true,
      retrievedChunks: retrieved.length,
    },
    context: request.includeRawContext
      ? {
          failedJobs,
          recentErrors: failedJobs,
          sampleLogs: logs.slice(0, 20),
          retrievedChunks: retrieved,
        }
      : undefined,
  };
}
