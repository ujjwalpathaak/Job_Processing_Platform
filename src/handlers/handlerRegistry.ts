import { JobHandlerType } from '../models/enums.js';
import { validateHandlerData } from '../models/jobPayloads.js';
import type { JobMessage } from '../models/jobMessage.js';
import { crmSyncHandler } from './crmSyncHandler.js';
import { emailHandler } from './emailHandler.js';
import { notificationCleanupHandler } from './notificationCleanupHandler.js';
import { paymentHandler } from './paymentHandler.js';
import { refundHandler } from './refundHandler.js';
import { reportGenerationHandler } from './reportGenerationHandler.js';
import type { JobHandler } from './types.js';
import { webhookTriggerHandler } from './webhookTriggerHandler.js';

const handlers: Record<JobHandlerType, JobHandler> = {
  [JobHandlerType.EMAIL]: emailHandler,
  [JobHandlerType.REPORT_GENERATION]: reportGenerationHandler,
  [JobHandlerType.NOTIFICATION_CLEANUP]: notificationCleanupHandler,
  [JobHandlerType.WEBHOOK_TRIGGER]: webhookTriggerHandler,
  [JobHandlerType.CRM_SYNC]: crmSyncHandler,
  [JobHandlerType.PAYMENT]: paymentHandler,
  [JobHandlerType.REFUND]: refundHandler,
};

export function getHandler(type: JobHandlerType): JobHandler {
  const handler = handlers[type];
  if (!handler) {
    throw new Error(`Unknown handler type: ${type}`);
  }
  return handler;
}

export async function processWithValidation(message: JobMessage): Promise<void> {
  const parsed = validateHandlerData(message.jobHandler, message.data);
  if (!parsed.success) {
    throw new Error(`Invalid job data for ${message.jobHandler}: ${parsed.error.message}`);
  }
  const handler = getHandler(message.jobHandler);
  await handler.process(message);
}
