import { JobCategory, JobHandlerType } from '../models/enums.js';
import { log } from '../services/logService.js';
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
    log.info('event=handler.email.sent jobId={}', message.jobId);
  },
};
