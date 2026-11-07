import type { Channel, ConsumeMessage } from 'amqplib';
import { rabbitTopology } from '../config.js';
import { getHandler, processWithValidation } from '../handlers/handlerRegistry.js';
import { exchangeForCategory, getRabbitChannel, queueForCategory } from '../helpers/rabbit.js';
import { getRetryCount, RETRY_HEADER, shouldRetry } from '../helpers/retry.js';
import type { JobMessage } from '../models/jobMessage.js';
import { JobCategory, JobStatus } from '../models/enums.js';
import { getJobById } from '../services/jobService.js';
import { updateJobStatus } from '../services/jobStatusService.js';

function parseMessage(msg: ConsumeMessage): JobMessage {
  return JSON.parse(msg.content.toString()) as JobMessage;
}

async function handleJobMessage(ch: Channel, msg: ConsumeMessage): Promise<void> {
  const jobMessage = parseMessage(msg);
  let job = await getJobById(jobMessage.jobId);
  if (!job) {
    ch.nack(msg, false, false);
    return;
  }

  const handler = getHandler(job.jobHandler);
  job = await updateJobStatus(job, JobStatus.PROCESSING);
  console.log(`${job.id} - STARTING TO PROCESS - consumer: ${jobMessage.jobCategory}`);

  try {
    await processWithValidation(jobMessage);
    await updateJobStatus(job, JobStatus.PROCESSED);
    ch.ack(msg);
    console.log(`${job.id} - FINISHED PROCESSING - consumer: ${jobMessage.jobCategory}`);
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : String(err);
    job = await updateJobStatus(job, JobStatus.ERROR, errorMessage);
    console.log(`${job.id} - FAILED TO PROCESS - consumer: ${jobMessage.jobCategory}`);

    const retryCount = getRetryCount(msg);
    if (!shouldRetry(handler.retries(), retryCount)) {
      const exchange = exchangeForCategory(jobMessage.jobCategory);
      const queueConfig = queueForCategory(jobMessage.jobCategory);
      ch.publish(exchange, queueConfig.dlqRoutingKey, msg.content, {
        contentType: 'application/json',
        persistent: true,
      });
      ch.ack(msg);
      await updateJobStatus(job, JobStatus.DEAD, errorMessage);
      console.log(`${job.id} - MARKED AS DEAD - consumer: ${jobMessage.jobCategory}`);
      return;
    }

    const delayKey = handler.backoff()[retryCount];
    const retry = rabbitTopology.retries[delayKey as keyof typeof rabbitTopology.retries];
    if (!retry) {
      ch.nack(msg, false, false);
      return;
    }

    ch.sendToQueue(retry.queue, msg.content, {
      contentType: 'application/json',
      persistent: true,
      headers: {
        ...msg.properties.headers,
        [RETRY_HEADER]: retryCount + 1,
      },
    });
    ch.ack(msg);
    await updateJobStatus(job, JobStatus.RETRY, errorMessage);
    console.log(`${job.id} - RETRY - consumer: ${jobMessage.jobCategory}, queue: ${retry.queue}`);
  }
}

export async function startCategoryConsumers(): Promise<void> {
  const ch = await getRabbitChannel();
  await ch.prefetch(5);

  for (const category of Object.values(JobCategory)) {
    const queue = queueForCategory(category).queue;
    await ch.consume(queue, (msg) => {
      if (!msg) return;
      void handleJobMessage(ch, msg).catch((err) => {
        console.error(`Consumer error on ${queue}:`, err);
        try {
          ch.nack(msg, false, false);
        } catch {
          // ignore
        }
      });
    });
    console.log(`Listening on ${queue}`);
  }
}

export async function startRetryRouter(): Promise<void> {
  const ch = await getRabbitChannel();
  await ch.consume(rabbitTopology.retryRouteQueue, (msg) => {
    if (!msg) return;
    try {
      const jobMessage = parseMessage(msg);
      const exchange = exchangeForCategory(jobMessage.jobCategory);
      const queue = queueForCategory(jobMessage.jobCategory);
      ch.publish(exchange, queue.routingKey, msg.content, {
        contentType: 'application/json',
        persistent: true,
        headers: msg.properties.headers,
      });
      ch.ack(msg);
    } catch (err) {
      console.error('Retry router failed:', err);
      ch.nack(msg, false, false);
    }
  });
  console.log(`Listening on ${rabbitTopology.retryRouteQueue}`);
}
