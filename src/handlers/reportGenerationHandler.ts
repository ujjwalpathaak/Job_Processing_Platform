import { JobCategory, JobHandlerType } from '../models/enums.js';
import { log } from '../services/logService.js';
import type { JobHandler } from './types.js';

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export const reportGenerationHandler: JobHandler = {
  identify: () => JobHandlerType.REPORT_GENERATION,
  category: () => JobCategory.STANDARD,
  retries: () => 1,
  backoff: () => ['30s'],
  process: async (message) => {
    await sleep(200);
    if (Math.random() < 0.2) {
      throw new Error('Report generation failed');
    }
    log.info('event=handler.report.generated jobId={}', message.jobId);
  },
};
