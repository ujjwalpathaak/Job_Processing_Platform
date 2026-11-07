import { JobCategory, JobHandlerType } from '../models/enums.js';
import type { JobHandler } from './types.js';

export const refundHandler: JobHandler = {
  identify: () => JobHandlerType.REFUND,
  category: () => JobCategory.CRITICAL,
  retries: () => 3,
  backoff: () => ['5s', '30s', '60s'],
  process: (message) => {
    if (Math.random() < 0.25) {
      throw new Error('Refund gateway error');
    }
    console.log(`Refund processed for job ${message.jobId}`);
  },
};
