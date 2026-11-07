import { appendFile, mkdir } from 'node:fs/promises';
import path from 'node:path';
import { randomUUID } from 'node:crypto';
import { AsyncLocalStorage } from 'node:async_hooks';
import { env } from '../config.js';
import { addLogToBatch } from './logBatchingService.js';

type LogLevel = 'TRACE' | 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';

type RequestContext = { requestId: string };

export const requestContext = new AsyncLocalStorage<RequestContext>();

function formatArgs(template: string, args: unknown[]): string {
  let i = 0;
  return template.replace(/\{\}/g, () => {
    const value = args[i++];
    return value === undefined ? '{}' : String(value);
  });
}

async function writeLog(level: LogLevel, template: string, args: unknown[]): Promise<void> {
  const requestId = requestContext.getStore()?.requestId ?? null;
  const message = {
    id: randomUUID().slice(0, 8),
    level,
    timestamp: new Date().toISOString(),
    requestId,
    event: formatArgs(template, args),
    error: level === 'ERROR' ? formatArgs(template, args) : null,
  };

  await mkdir(env.LOG_DIR, { recursive: true });
  const file = path.join(env.LOG_DIR, `${new Date().toISOString().slice(0, 10)}.log`);
  await appendFile(file, `${JSON.stringify(message)}\n`, 'utf8');

  if (requestId) {
    await addLogToBatch(message);
  }
}

export const log = {
  info: (template: string, ...args: unknown[]) => void writeLog('INFO', template, args),
  debug: (template: string, ...args: unknown[]) => void writeLog('DEBUG', template, args),
  warn: (template: string, ...args: unknown[]) => void writeLog('WARN', template, args),
  error: (template: string, ...args: unknown[]) => void writeLog('ERROR', template, args),
};
