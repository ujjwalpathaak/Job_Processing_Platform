import cors from 'cors';
import express, { type NextFunction, type Request, type Response } from 'express';
import path from 'node:path';
import { randomUUID } from 'node:crypto';
import { failure } from './models/apiResponse.js';
import { insightsRouter } from './routes/insights.js';
import { jobsRouter } from './routes/jobs.js';
import { flushRequestBatch } from './services/logBatchingService.js';
import { log, requestContext } from './services/logService.js';
import { prisma } from './models/db.js';

export function createApp() {
  const app = express();

  app.use(cors());
  app.use(express.json({ limit: '1mb' }));
  app.use(express.static(path.join(process.cwd(), 'public')));

  app.use((req, res, next) => {
    const incoming = req.header('x-request-id');
    const requestId = incoming && incoming.trim() !== '' ? incoming : randomUUID();
    res.setHeader('X-Request-Id', requestId);

    let flushed = false;
    const flush = () => {
      if (flushed) return;
      flushed = true;
      void flushRequestBatch(requestId).catch((err) => {
        log.error('event=log.batch.flush_failed reason={}', err instanceof Error ? err.message : err);
      });
    };
    res.on('finish', flush);
    res.on('close', flush);

    requestContext.run({ requestId }, () => next());
  });

  app.get('/health', async (_req, res) => {
    try {
      await prisma.$queryRaw`SELECT 1`;
      res.json({ status: 'ok' });
    } catch (err) {
      res.status(503).json({
        status: 'degraded',
        error: err instanceof Error ? err.message : String(err),
      });
    }
  });

  app.use('/jobs', jobsRouter);
  app.use('/api', insightsRouter);

  app.use((err: unknown, _req: Request, res: Response, _next: NextFunction) => {
    const message = err instanceof Error ? err.message : 'Internal server error';
    log.error('event=api.unhandled_error reason={}', message);
    res.status(500).json(failure(message));
  });

  return app;
}
