import { rabbitTopology } from '../config.js';
import { getRabbitChannel } from '../helpers/rabbit.js';

export async function publishLogRagBatch(payload: unknown): Promise<void> {
  const ch = await getRabbitChannel();
  ch.sendToQueue(rabbitTopology.logRagQueue, Buffer.from(JSON.stringify(payload)), {
    contentType: 'application/json',
    persistent: true,
  });
}
