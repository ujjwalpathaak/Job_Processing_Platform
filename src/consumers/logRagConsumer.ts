import type { ConsumeMessage } from 'amqplib';
import { rabbitTopology } from '../config.js';
import { getRabbitChannel } from '../helpers/rabbit.js';
import { log } from '../services/logService.js';
import { buildEmbeddings, saveEmbedding } from '../services/ragEmbeddingService.js';

type LogItem = {
  id: string;
  level: string;
  timestamp: string;
  requestId: string | null;
  jobId?: number | null;
  handler?: string | null;
  event: string;
  error: string | null;
};

export async function startLogRagConsumer(): Promise<void> {
  const ch = await getRabbitChannel();
  await ch.consume(rabbitTopology.logRagQueue, (msg) => {
    if (!msg) return;
    void handleBatch(ch, msg).catch((err) => {
      log.error(
        'event=log.rag.consumer_failed reason={}',
        err instanceof Error ? err.message : err,
      );
      ch.nack(msg, false, false);
    });
  });
  log.info('event=worker.listening queue={}', rabbitTopology.logRagQueue);
}

async function handleBatch(
  ch: Awaited<ReturnType<typeof getRabbitChannel>>,
  msg: ConsumeMessage,
): Promise<void> {
  const batch = JSON.parse(msg.content.toString()) as LogItem[];
  const texts = batch.map((item) => {
    const parts = [
      item.level,
      item.jobId != null ? `jobId=${item.jobId}` : null,
      item.handler ? `handler=${item.handler}` : null,
      item.event,
      item.error ? `error=${item.error}` : null,
    ].filter(Boolean);
    return parts.join(' ');
  });

  const embeddings = await buildEmbeddings(texts);

  for (let i = 0; i < batch.length; i++) {
    const item = batch[i];
    const embedding = embeddings[i];
    await saveEmbedding({
      sourceType: 'log',
      sourceName: item.handler || item.event.slice(0, 100) || item.level,
      content: texts[i],
      requestId: item.requestId,
      jobId: item.jobId ?? null,
      level: item.level,
      handler: item.handler ?? null,
      embedding: embedding.vectorLiteral,
      embeddingModel: embedding.model,
    });
  }
  ch.ack(msg);
}
