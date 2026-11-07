import { JobCategory, JobHandlerType } from '../models/enums.js';
import type { JobHandler } from './types.js';

export const emailHandler: JobHandler = {
  identify: () => JobHandlerType.EMAIL,
  category: () => JobCategory.STANDARD,
  retries: () => 2,
  backoff: () => ['5s', '30s'],
  process: (message) => {
    if (Math.random() < 0.3) {
      throw new Error('Invalid emailId');
    }
    console.log(`Email sent for job ${message.jobId}`);
  },
};
