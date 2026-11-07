export const JobStatus = {
  SCHEDULED: 'SCHEDULED',
  PUBLISHED: 'PUBLISHED',
  PROCESSING: 'PROCESSING',
  PROCESSED: 'PROCESSED',
  RETRY: 'RETRY',
  ERROR: 'ERROR',
  DEAD: 'DEAD',
} as const;

export type JobStatus = (typeof JobStatus)[keyof typeof JobStatus];

export const JobCategory = {
  STANDARD: 'STANDARD',
  CRITICAL: 'CRITICAL',
  EXTERNAL: 'EXTERNAL',
} as const;

export type JobCategory = (typeof JobCategory)[keyof typeof JobCategory];

export const JobHandlerType = {
  EMAIL: 'EMAIL',
  REPORT_GENERATION: 'REPORT_GENERATION',
  NOTIFICATION_CLEANUP: 'NOTIFICATION_CLEANUP',
  WEBHOOK_TRIGGER: 'WEBHOOK_TRIGGER',
  CRM_SYNC: 'CRM_SYNC',
  PAYMENT: 'PAYMENT',
  REFUND: 'REFUND',
} as const;

export type JobHandlerType = (typeof JobHandlerType)[keyof typeof JobHandlerType];

export const ALLOWED_TRANSITIONS: Record<JobStatus, readonly JobStatus[]> = {
  SCHEDULED: [JobStatus.PUBLISHED, JobStatus.DEAD],
  PUBLISHED: [JobStatus.PROCESSING, JobStatus.DEAD],
  PROCESSING: [JobStatus.PROCESSED, JobStatus.RETRY, JobStatus.ERROR, JobStatus.DEAD],
  RETRY: [JobStatus.PROCESSING, JobStatus.DEAD],
  ERROR: [JobStatus.RETRY, JobStatus.DEAD],
  PROCESSED: [],
  DEAD: [],
};
