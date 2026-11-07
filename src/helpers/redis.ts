import { Redis as RedisClient } from 'ioredis';
import { env } from '../config.js';

let client: RedisClient | null = null;

export function getRedis(): RedisClient {
  if (!client) {
    client = new RedisClient({
      host: env.REDIS_HOST,
      port: env.REDIS_PORT,
      maxRetriesPerRequest: 3,
      lazyConnect: true,
    });
  }
  return client;
}

export async function closeRedis(): Promise<void> {
  if (client) {
    await client.quit();
    client = null;
  }
}
