import { JobCategory, JobHandlerType } from '../models/enums.js';
import type { JobHandler } from './types.js';

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export const webhookTriggerHandler: JobHandler = {
  identify: () => JobHandlerType.WEBHOOK_TRIGGER,
  category: () => JobCategory.EXTERNAL,
  retries: () => 3,
  backoff: () => ['5s', '30s', '60s'],
  process: async (message) => {
    await sleep(150);
    if (Math.random() < 0.35) {
      throw new Error('Webhook returned 502');
    }
    console.log(`Webhook triggered for job ${message.jobId}`);
  },
};
