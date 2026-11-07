import { env } from '../config.js';
import { prisma } from '../models/db.js';

export const EMBEDDING_DIM = 768;
export const GEMINI_EMBEDDING_MODEL_LABEL = 'gemini-text-embedding-004';
export const HASH_EMBEDDING_MODEL_LABEL = 'hash-fallback';

export type EmbeddingResult = {
  vectorLiteral: string;
  model: string;
};

export type SimilarChunk = {
  id: number;
  sourceType: string;
  sourceName: string;
  content: string;
  requestId: string | null;
  jobId: number | null;
  handler: string | null;
  distance: number;
  similarity: number;
};

function fallbackVector(text: string): number[] {
  const values: number[] = [];
  for (let i = 0; i < EMBEDDING_DIM; i++) {
    const code = text.charCodeAt(i % Math.max(text.length, 1)) || 0;
    values.push((code % 97) / 97);
  }
  return values;
}

export function toVectorLiteral(values: number[]): string {
  return `[${values.join(',')}]`;
}

function normalizeDimension(values: number[]): number[] {
  if (values.length === EMBEDDING_DIM) {
    return values;
  }
  if (values.length > EMBEDDING_DIM) {
    return values.slice(0, EMBEDDING_DIM);
  }
  return [...values, ...Array(EMBEDDING_DIM - values.length).fill(0)];
}

async function embedOneWithGemini(text: string): Promise<number[] | null> {
  if (!env.GEMINI_API_KEY) {
    return null;
  }

  const url = `${env.GEMINI_BASE_URL.replace(/\/$/, '')}/models/${env.GEMINI_EMBEDDING_MODEL}:embedContent?key=${env.GEMINI_API_KEY}`;
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      content: { parts: [{ text }] },
    }),
  });

  if (!response.ok) {
    return null;
  }

  const body = (await response.json()) as {
    embedding?: { values?: number[] };
  };
  const values = body.embedding?.values;
  if (!values?.length) {
    return null;
  }
  return normalizeDimension(values);
}

/** Batch embed via Gemini batchEmbedContents; falls back to sequential / hash. */
export async function buildEmbeddings(texts: string[]): Promise<EmbeddingResult[]> {
  if (texts.length === 0) {
    return [];
  }

  if (env.GEMINI_API_KEY) {
    const url = `${env.GEMINI_BASE_URL.replace(/\/$/, '')}/models/${env.GEMINI_EMBEDDING_MODEL}:batchEmbedContents?key=${env.GEMINI_API_KEY}`;
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        requests: texts.map((text) => ({
          model: `models/${env.GEMINI_EMBEDDING_MODEL}`,
          content: { parts: [{ text }] },
        })),
      }),
    });

    if (response.ok) {
      const body = (await response.json()) as {
        embeddings?: { values?: number[] }[];
      };
      if (body.embeddings?.length === texts.length) {
        return body.embeddings.map((item) => ({
          vectorLiteral: toVectorLiteral(normalizeDimension(item.values ?? [])),
          model: GEMINI_EMBEDDING_MODEL_LABEL,
        }));
      }
    }
  }

  const results: EmbeddingResult[] = [];
  for (const text of texts) {
    const gemini = await embedOneWithGemini(text);
    if (gemini) {
      results.push({
        vectorLiteral: toVectorLiteral(gemini),
        model: GEMINI_EMBEDDING_MODEL_LABEL,
      });
    } else {
      results.push({
        vectorLiteral: toVectorLiteral(fallbackVector(text)),
        model: HASH_EMBEDDING_MODEL_LABEL,
      });
    }
  }
  return results;
}

export async function buildEmbedding(text: string): Promise<EmbeddingResult> {
  const [result] = await buildEmbeddings([text]);
  return result;
}

export async function saveEmbedding(input: {
  sourceType: string;
  sourceName: string;
  content: string;
  requestId: string | null;
  jobId?: number | null;
  level?: string | null;
  handler?: string | null;
  embedding: string;
  embeddingModel: string;
}): Promise<void> {
  // Keep hash fallbacks out of the searchable index to avoid polluting Gemini space.
  if (input.embeddingModel === HASH_EMBEDDING_MODEL_LABEL) {
    return;
  }

  await prisma.$executeRawUnsafe(
    `INSERT INTO rag_embeddings
      (source_type, source_name, content, request_id, job_id, level, handler, embedding_model, created_at, embedding)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW(), $9::vector)`,
    input.sourceType,
    input.sourceName,
    input.content.slice(0, 4000),
    input.requestId,
    input.jobId ?? null,
    input.level ?? null,
    input.handler ?? null,
    input.embeddingModel,
    input.embedding,
  );
}

export type FindSimilarOptions = {
  limit?: number;
  since?: Date;
  jobIds?: number[];
  handlers?: string[];
  geminiOnly?: boolean;
};

export async function findSimilar(
  embedding: string,
  options: FindSimilarOptions = {},
): Promise<SimilarChunk[]> {
  const limit = options.limit ?? 8;
  const clauses: string[] = ['1=1'];
  const params: unknown[] = [embedding];
  let idx = 2;

  if (options.geminiOnly !== false) {
    clauses.push(`embedding_model = $${idx}`);
    params.push(GEMINI_EMBEDDING_MODEL_LABEL);
    idx += 1;
  }
  if (options.since) {
    clauses.push(`created_at >= $${idx}`);
    params.push(options.since);
    idx += 1;
  }
  if (options.jobIds?.length) {
    clauses.push(`job_id = ANY($${idx}::bigint[])`);
    params.push(options.jobIds);
    idx += 1;
  }
  if (options.handlers?.length) {
    clauses.push(`handler = ANY($${idx}::text[])`);
    params.push(options.handlers);
    idx += 1;
  }

  params.push(limit);
  const limitParam = `$${idx}`;

  const rows = await prisma.$queryRawUnsafe<
    {
      id: bigint;
      source_type: string;
      source_name: string;
      content: string;
      request_id: string | null;
      job_id: bigint | null;
      handler: string | null;
      distance: number;
    }[]
  >(
    `SELECT id, source_type, source_name, content, request_id, job_id, handler,
            (embedding <-> $1::vector) AS distance
     FROM rag_embeddings
     WHERE ${clauses.join(' AND ')}
     ORDER BY embedding <-> $1::vector
     LIMIT ${limitParam}`,
    ...params,
  );

  return rows.map((row) => {
    const distance = Number(row.distance);
    return {
      id: Number(row.id),
      sourceType: row.source_type,
      sourceName: row.source_name,
      content: row.content,
      requestId: row.request_id,
      jobId: row.job_id != null ? Number(row.job_id) : null,
      handler: row.handler,
      distance,
      similarity: 1 / (1 + distance),
    };
  });
}

export async function findKeywordCandidates(input: {
  tokens: string[];
  since?: Date;
  jobIds?: number[];
  limit?: number;
}): Promise<SimilarChunk[]> {
  if (input.tokens.length === 0) {
    return [];
  }

  const limit = input.limit ?? 20;
  const patterns = input.tokens.slice(0, 8).map((t) => `%${t}%`);
  const clauses: string[] = [`embedding_model = $1`];
  const params: unknown[] = [GEMINI_EMBEDDING_MODEL_LABEL];
  let idx = 2;

  if (input.since) {
    clauses.push(`created_at >= $${idx}`);
    params.push(input.since);
    idx += 1;
  }
  if (input.jobIds?.length) {
    clauses.push(`job_id = ANY($${idx}::bigint[])`);
    params.push(input.jobIds);
    idx += 1;
  }

  const likeClauses = patterns.map((p) => {
    const clause = `content ILIKE $${idx}`;
    params.push(p);
    idx += 1;
    return clause;
  });
  clauses.push(`(${likeClauses.join(' OR ')})`);
  params.push(limit);

  const rows = await prisma.$queryRawUnsafe<
    {
      id: bigint;
      source_type: string;
      source_name: string;
      content: string;
      request_id: string | null;
      job_id: bigint | null;
      handler: string | null;
    }[]
  >(
    `SELECT id, source_type, source_name, content, request_id, job_id, handler
     FROM rag_embeddings
     WHERE ${clauses.join(' AND ')}
     ORDER BY created_at DESC
     LIMIT $${idx}`,
    ...params,
  );

  return rows.map((row) => ({
    id: Number(row.id),
    sourceType: row.source_type,
    sourceName: row.source_name,
    content: row.content,
    requestId: row.request_id,
    jobId: row.job_id != null ? Number(row.job_id) : null,
    handler: row.handler,
    distance: 1,
    similarity: 0,
  }));
}
