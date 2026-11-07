import type { JobCategory, JobHandlerType } from './enums.js';

export type JobMessage = {
  jobId: number;
  jobCategory: JobCategory;
  jobHandler: JobHandlerType;
  data: Record<string, unknown>;
};
