import { env } from '../config.js';
import { getRedis } from '../helpers/redis.js';
import { publishLogRagBatch } from '../producers/logRagProducer.js';

export type LogMessage = {
  id: string;
  level: string;
  timestamp: string;
  requestId: string | null;
  jobId?: number | null;
  handler?: string | null;
  event: string;
  error: string | null;
};

function keyFor(scope: string): string {
  return `log-batch:${scope}`;
}

function batchScope(message: LogMessage): string | null {
  if (message.requestId) {
    return `req:${message.requestId}`;
  }
  if (message.jobId != null) {
    return `job:${message.jobId}`;
  }
  return null;
}

async function ensureRedis() {
  const redis = getRedis();
  if (redis.status !== 'ready') {
    await redis.connect();
  }
  return redis;
}

async function flushKey(key: string): Promise<void> {
  const redis = await ensureRedis();
  const items = await redis.lrange(key, 0, -1);
  if (items.length === 0) {
    return;
  }
  await redis.del(key);
  const payload = items.map((item: string) => JSON.parse(item) as LogMessage);
  await publishLogRagBatch(payload);
}

/** Push a structured log into Redis; flush when batch size is reached. */
export async function addLogToBatch(message: LogMessage): Promise<void> {
  const scope = batchScope(message);
  if (!scope) {
    return;
  }

  const redis = await ensureRedis();
  const key = keyFor(scope);
  await redis.rpush(key, JSON.stringify(message));
  const size = await redis.llen(key);
  if (size >= env.LOG_BATCH_SIZE) {
    await flushKey(key);
  }
}

/** Flush remaining logs for an HTTP request (call on response finish). */
export async function flushRequestBatch(requestId: string): Promise<void> {
  if (!requestId) {
    return;
  }
  await flushKey(keyFor(`req:${requestId}`));
}

/** Flush remaining logs for a job (call after job handling completes). */
export async function flushJobBatch(jobId: number): Promise<void> {
  await flushKey(keyFor(`job:${jobId}`));
}
