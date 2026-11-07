import { createWriteStream, mkdirSync } from 'node:fs';
import path from 'node:path';
import { randomUUID } from 'node:crypto';
import { AsyncLocalStorage } from 'node:async_hooks';
import pino, { type Logger } from 'pino';
import { env } from '../config.js';
import { addLogToBatch, type LogMessage } from './logBatchingService.js';

export type LogBindings = {
  requestId?: string;
  jobId?: number;
  handler?: string;
  category?: string;
};

export const requestContext = new AsyncLocalStorage<LogBindings>();

function formatArgs(template: string, args: unknown[]): string {
  let i = 0;
  return template.replace(/\{\}/g, () => {
    const value = args[i++];
    return value === undefined ? '{}' : String(value);
  });
}

function dailyLogPath(): string {
  mkdirSync(env.LOG_DIR, { recursive: true });
  return path.join(env.LOG_DIR, `${new Date().toISOString().slice(0, 10)}.log`);
}

const fileStream = createWriteStream(dailyLogPath(), { flags: 'a' });

const baseLogger: Logger = pino(
  {
    level: env.NODE_ENV === 'test' ? 'silent' : 'info',
    base: { service: 'job-processing-platform' },
    timestamp: pino.stdTimeFunctions.isoTime,
    mixin() {
      return requestContext.getStore() ?? {};
    },
  },
  pino.multistream([
    { stream: process.stdout },
    { stream: fileStream },
  ]),
);

/** Index warnings/errors and job/handler lifecycle events only — keep the RAG corpus clean. */
function shouldIndexForRag(level: string, event: string, bindings: LogBindings): boolean {
  if (!bindings.requestId && bindings.jobId == null) {
    return false;
  }
  if (level === 'WARN' || level === 'ERROR') {
    return true;
  }
  return event.includes('event=job.') || event.includes('event=handler.');
}

async function enqueueRag(
  level: string,
  event: string,
  bindings: LogBindings,
  isError: boolean,
): Promise<void> {
  if (!shouldIndexForRag(level, event, bindings)) {
    return;
  }

  const message: LogMessage = {
    id: randomUUID().slice(0, 8),
    level,
    timestamp: new Date().toISOString(),
    requestId: bindings.requestId ?? null,
    jobId: bindings.jobId ?? null,
    handler: bindings.handler ?? null,
    event,
    error: isError ? event : null,
  };

  await addLogToBatch(message);
}

export type AppLogger = {
  info: (template: string, ...args: unknown[]) => void;
  debug: (template: string, ...args: unknown[]) => void;
  warn: (template: string, ...args: unknown[]) => void;
  error: (template: string, ...args: unknown[]) => void;
  child: (bindings: LogBindings) => AppLogger;
  raw: Logger;
};

function createFacade(logger: Logger, staticBindings: LogBindings = {}): AppLogger {
  const emit = (level: 'info' | 'debug' | 'warn' | 'error', template: string, args: unknown[]) => {
    const event = formatArgs(template, args);
    const bindings: LogBindings = {
      ...(requestContext.getStore() ?? {}),
      ...staticBindings,
    };
    logger[level]({ event, ...staticBindings }, event);
    void enqueueRag(level.toUpperCase(), event, bindings, level === 'error');
  };

  return {
    info: (template, ...args) => emit('info', template, args),
    debug: (template, ...args) => emit('debug', template, args),
    warn: (template, ...args) => emit('warn', template, args),
    error: (template, ...args) => emit('error', template, args),
    child: (bindings) => createFacade(logger.child(bindings), { ...staticBindings, ...bindings }),
    raw: logger,
  };
}

/** App logger: Pino for output, Redis/RAG sink for correlated events. */
export const log = createFacade(baseLogger);

export function runWithLogContext<T>(bindings: LogBindings, fn: () => T): T {
  const parent = requestContext.getStore() ?? {};
  return requestContext.run({ ...parent, ...bindings }, fn);
}
