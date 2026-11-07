import { env } from '../config.js';
import { getRedis } from '../helpers/redis.js';
import { publishLogRagBatch } from '../producers/logRagProducer.js';

type LogMessage = {
  id: string;
  level: string;
  timestamp: string;
  requestId: string | null;
  event: string;
  error: string | null;
};

function keyFor(requestId: string): string {
  return `log-batch:${requestId}`;
}

export async function addLogToBatch(message: LogMessage): Promise<void> {
  if (!message.requestId) {
    return;
  }

  const redis = getRedis();
  if (redis.status !== 'ready') {
    await redis.connect();
  }

  const key = keyFor(message.requestId);
  await redis.rpush(key, JSON.stringify(message));
  const size = await redis.llen(key);
  if (size >= env.LOG_BATCH_SIZE) {
    const items = await redis.lrange(key, 0, -1);
    await redis.del(key);
    const payload = items.map((item: string) => JSON.parse(item) as LogMessage);
    await publishLogRagBatch(payload);
  }
}
