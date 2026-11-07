import type { ConsumeMessage } from 'amqplib';

export const RETRY_HEADER = 'x-retry-count';

export function getRetryCount(msg: ConsumeMessage): number {
  const raw = msg.properties.headers?.[RETRY_HEADER];
  const value = typeof raw === 'number' ? raw : Number(raw ?? 0);
  return Number.isFinite(value) ? value : 0;
}

export function shouldRetry(maxRetries: number, retryCount: number): boolean {
  return retryCount < maxRetries;
}
