import { env } from '../config.js';
import { prisma } from '../models/db.js';

const EMBEDDING_DIM = 768;

function fallbackVector(text: string): number[] {
  const values: number[] = [];
  for (let i = 0; i < EMBEDDING_DIM; i++) {
    const code = text.charCodeAt(i % Math.max(text.length, 1)) || 0;
    values.push((code % 97) / 97);
  }
  return values;
}

function toVectorLiteral(values: number[]): string {
  return `[${values.join(',')}]`;
}

async function embedWithGemini(text: string): Promise<number[] | null> {
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
    console.error(`Gemini embedding failed: ${response.status}`);
    return null;
  }

  const body = (await response.json()) as {
    embedding?: { values?: number[] };
  };
  const values = body.embedding?.values;
  if (!values?.length) {
    return null;
  }
  if (values.length === EMBEDDING_DIM) {
    return values;
  }
  if (values.length > EMBEDDING_DIM) {
    return values.slice(0, EMBEDDING_DIM);
  }
  return [...values, ...Array(EMBEDDING_DIM - values.length).fill(0)];
}

export async function buildEmbedding(text: string): Promise<string> {
  const values = (await embedWithGemini(text)) ?? fallbackVector(text);
  return toVectorLiteral(values);
}

export async function saveEmbedding(input: {
  sourceType: string;
  sourceName: string;
  content: string;
  requestId: string | null;
  embedding: string;
}): Promise<void> {
  await prisma.$executeRawUnsafe(
    `INSERT INTO rag_embeddings (source_type, source_name, content, request_id, created_at, embedding)
     VALUES ($1, $2, $3, $4, NOW(), $5::vector)`,
    input.sourceType,
    input.sourceName,
    input.content.slice(0, 4000),
    input.requestId,
    input.embedding,
  );
}

export async function findSimilar(embedding: string, limit: number) {
  const rows = await prisma.$queryRawUnsafe<
    { id: bigint; source_type: string; source_name: string; content: string; request_id: string | null }[]
  >(
    `SELECT id, source_type, source_name, content, request_id
     FROM rag_embeddings
     ORDER BY embedding <-> $1::vector
     LIMIT $2`,
    embedding,
    limit,
  );

  return rows.map((row) => ({
    id: Number(row.id),
    sourceType: row.source_type,
    sourceName: row.source_name,
    content: row.content,
    requestId: row.request_id,
    similarity: 0,
  }));
}
