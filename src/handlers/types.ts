import type { JobMessage } from '../models/jobMessage.js';
import type { JobCategory, JobHandlerType } from '../models/enums.js';

export type JobHandler = {
  identify: () => JobHandlerType;
  category: () => JobCategory;
  retries: () => number;
  backoff: () => string[];
  process: (message: JobMessage) => Promise<void> | void;
};
