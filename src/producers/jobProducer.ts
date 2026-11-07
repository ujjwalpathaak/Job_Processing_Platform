import { exchangeForCategory, getRabbitChannel, queueForCategory } from '../helpers/rabbit.js';
import type { JobMessage } from '../models/jobMessage.js';
export async function publishJob(message: JobMessage): Promise<void> {
  const ch = await getRabbitChannel();
  const exchange = exchangeForCategory(message.jobCategory);
  const queue = queueForCategory(message.jobCategory);
  const payload = Buffer.from(JSON.stringify(message));
  ch.publish(exchange, queue.routingKey, payload, {
    contentType: 'application/json',
    persistent: true,
  });
}
