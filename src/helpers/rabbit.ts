import amqp, { type Channel, type ChannelModel } from 'amqplib';
import { env, rabbitTopology } from '../config.js';
import type { JobCategory } from '../models/enums.js';

let connection: ChannelModel | null = null;
let channel: Channel | null = null;

export async function getRabbitChannel(): Promise<Channel> {
  if (channel) {
    return channel;
  }

  connection = await amqp.connect(env.RABBITMQ_URL);
  channel = await connection.createChannel();
  await declareTopology(channel);
  return channel;
}

export async function declareTopology(ch: Channel): Promise<void> {
  const retryRoute = rabbitTopology.retryRouteQueue;
  await ch.assertExchange('retry.delay.exchange', 'direct', { durable: true });
  await ch.assertQueue(retryRoute, { durable: true });
  await ch.bindQueue(retryRoute, 'retry.delay.exchange', 'retry.route');

  for (const [category, exchange] of Object.entries(rabbitTopology.exchanges)) {
    await ch.assertExchange(exchange, 'direct', { durable: true });
    const queueConfig = rabbitTopology.queues[category as JobCategory];

    await ch.assertQueue(queueConfig.dlq, { durable: true });
    await ch.bindQueue(queueConfig.dlq, exchange, queueConfig.dlqRoutingKey);

    await ch.assertQueue(queueConfig.queue, {
      durable: true,
      arguments: {
        'x-dead-letter-exchange': exchange,
        'x-dead-letter-routing-key': queueConfig.dlqRoutingKey,
      },
    });
    await ch.bindQueue(queueConfig.queue, exchange, queueConfig.routingKey);
  }

  for (const retry of Object.values(rabbitTopology.retries)) {
    await ch.assertQueue(retry.queue, {
      durable: true,
      arguments: {
        'x-message-ttl': retry.ttl,
        'x-dead-letter-exchange': 'retry.delay.exchange',
        'x-dead-letter-routing-key': 'retry.route',
      },
    });
  }

  await ch.assertQueue(rabbitTopology.logRagQueue, { durable: true });
}

export async function closeRabbit(): Promise<void> {
  try {
    await channel?.close();
  } catch {
    // ignore
  }
  try {
    await connection?.close();
  } catch {
    // ignore
  }
  channel = null;
  connection = null;
}

export function queueForCategory(category: JobCategory) {
  return rabbitTopology.queues[category];
}

export function exchangeForCategory(category: JobCategory) {
  return rabbitTopology.exchanges[category];
}
