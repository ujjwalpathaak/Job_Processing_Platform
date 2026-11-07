import { JobCategory, JobHandlerType } from '../models/enums.js';
import type { JobHandler } from './types.js';

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export const crmSyncHandler: JobHandler = {
  identify: () => JobHandlerType.CRM_SYNC,
  category: () => JobCategory.EXTERNAL,
  retries: () => 2,
  backoff: () => ['5s', '60s'],
  process: async (_message) => {
    await sleep(300);
    throw new Error('CRM sync timed out');
  },
};
