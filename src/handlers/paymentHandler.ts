import { JobCategory, JobHandlerType } from '../models/enums.js';
import type { JobHandler } from './types.js';

export const paymentHandler: JobHandler = {
  identify: () => JobHandlerType.PAYMENT,
  category: () => JobCategory.CRITICAL,
  retries: () => 3,
  backoff: () => ['5s', '30s', '60s'],
  process: (message) => {
    if (Math.random() < 0.25) {
      throw new Error('Payment gateway error');
    }
    console.log(`Payment processed for job ${message.jobId}`);
  },
};
