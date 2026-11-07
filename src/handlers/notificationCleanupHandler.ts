import { JobCategory, JobHandlerType } from '../models/enums.js';
import type { JobHandler } from './types.js';

export const notificationCleanupHandler: JobHandler = {
  identify: () => JobHandlerType.NOTIFICATION_CLEANUP,
  category: () => JobCategory.STANDARD,
  retries: () => 0,
  backoff: () => [],
  process: (message) => {
    if (message.data == null) {
      throw new Error('Missing cleanup data');
    }
    console.log(`Notification cleanup for job ${message.jobId}`);
  },
};
