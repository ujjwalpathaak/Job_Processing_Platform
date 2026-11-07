import { JobCategory, JobHandlerType } from '../models/enums.js';
import { log } from '../services/logService.js';
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
    log.info('event=handler.payment.processed jobId={}', message.jobId);
  },
};
