import type { ConsumeMessage } from 'amqplib';
import { rabbitTopology } from '../config.js';
import { getRabbitChannel } from '../helpers/rabbit.js';
import { buildEmbedding, saveEmbedding } from '../services/ragEmbeddingService.js';

type LogItem = {
  id: string;
  level: string;
  timestamp: string;
  requestId: string | null;
  event: string;
  error: string | null;
};

export async function startLogRagConsumer(): Promise<void> {
  const ch = await getRabbitChannel();
  await ch.consume(rabbitTopology.logRagQueue, (msg) => {
    if (!msg) return;
    void handleBatch(ch, msg).catch((err) => {
      console.error('Log RAG consumer failed:', err);
      ch.nack(msg, false, false);
    });
  });
  console.log(`Listening on ${rabbitTopology.logRagQueue}`);
}

async function handleBatch(
  ch: Awaited<ReturnType<typeof getRabbitChannel>>,
  msg: ConsumeMessage,
): Promise<void> {
  const batch = JSON.parse(msg.content.toString()) as LogItem[];
  for (const item of batch) {
    const text = `${item.level} ${item.event}${item.error ? ` error=${item.error}` : ''}`;
    const embedding = await buildEmbedding(text);
    await saveEmbedding({
      sourceType: 'log',
      sourceName: item.event.slice(0, 100) || item.level,
      content: text,
      requestId: item.requestId,
      embedding,
    });
  }
  ch.ack(msg);
}
