import { config as loadDotenv } from 'dotenv';
import { z } from 'zod';

loadDotenv();

const envSchema = z.object({
  PORT: z.coerce.number().int().positive().default(3000),
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
  DATABASE_URL: z.string().min(1),
  REDIS_HOST: z.string().default('localhost'),
  REDIS_PORT: z.coerce.number().int().positive().default(6379),
  RABBITMQ_URL: z.string().default('amqp://guest:guest@localhost:5672'),
  LOG_BATCH_SIZE: z.coerce.number().int().positive().default(10),
  LOG_DIR: z.string().default('logs'),
  GEMINI_API_KEY: z.string().optional().default(''),
  GEMINI_BASE_URL: z.string().default('https://generativelanguage.googleapis.com/v1beta'),
  GEMINI_MODEL: z.string().default('gemini-2.0-flash'),
  GEMINI_EMBEDDING_MODEL: z.string().default('text-embedding-004'),
  GEMINI_SYSTEM_PROMPT: z
    .string()
    .default(
      'You produce operational insights using retrieved context from logs, jobs, and documentation.',
    ),
});

const parsed = envSchema.safeParse(process.env);

if (!parsed.success) {
  console.error('Invalid environment configuration:', parsed.error.flatten().fieldErrors);
  process.exit(1);
}

export const env = parsed.data;

export const rabbitTopology = {
  exchanges: {
    STANDARD: 'standard.exchange',
    CRITICAL: 'critical.exchange',
    EXTERNAL: 'external.exchange',
  },
  queues: {
    STANDARD: {
      queue: 'standard.queue',
      routingKey: 'standard.queue.key',
      dlq: 'standard.dlq',
      dlqRoutingKey: 'standard.dlq.key',
    },
    CRITICAL: {
      queue: 'critical.queue',
      routingKey: 'critical.queue.key',
      dlq: 'critical.dlq',
      dlqRoutingKey: 'critical.dlq.key',
    },
    EXTERNAL: {
      queue: 'external.queue',
      routingKey: 'external.queue.key',
      dlq: 'external.dlq',
      dlqRoutingKey: 'external.dlq.key',
    },
  },
  retries: {
    '5s': { queue: 'retry.5s.queue', routingKey: 'retry.5s.key', ttl: 5000 },
    '30s': { queue: 'retry.30s.queue', routingKey: 'retry.30s.key', ttl: 30000 },
    '60s': { queue: 'retry.60s.queue', routingKey: 'retry.60s.key', ttl: 60000 },
    '300s': { queue: 'retry.300s.queue', routingKey: 'retry.300s.key', ttl: 300000 },
    '1800s': { queue: 'retry.1800s.queue', routingKey: 'retry.1800s.key', ttl: 1800000 },
  },
  retryRouteQueue: 'retry.route.queue',
  logRagQueue: 'log.rag.queue',
} as const;
