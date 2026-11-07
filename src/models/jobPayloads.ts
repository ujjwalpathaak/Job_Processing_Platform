import { z } from 'zod';
import { JobHandlerType } from './enums.js';

const emailDataSchema = z.object({
  to: z.string().email(),
  from: z.string().email(),
  subject: z.string().min(1),
  content: z.string().min(1),
});

const reportDataSchema = z
  .object({
    reportType: z.string().min(1),
    format: z.enum(['pdf', 'csv', 'xlsx']),
    fromDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
    toDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
  })
  .refine((d) => d.fromDate <= d.toDate, {
    message: 'fromDate must be less than or equal to toDate',
    path: ['fromDate'],
  });

const notificationCleanupDataSchema = z.object({
  olderThanDays: z.number().int().min(1).max(365),
  channel: z.enum(['email', 'sms', 'push', 'all']),
});

const webhookDataSchema = z.object({
  url: z.string().url().refine((u) => u.startsWith('http://') || u.startsWith('https://'), {
    message: 'url must be http or https',
  }),
  method: z.enum(['GET', 'POST', 'PUT', 'PATCH']),
  headers: z.record(z.string(), z.string()).optional(),
  body: z.record(z.string(), z.unknown()).optional(),
});

const crmSyncDataSchema = z.object({
  crmSystem: z.string().min(1),
  entity: z.string().min(1),
  externalId: z.string().min(1),
  payload: z.record(z.string(), z.unknown()),
});

const paymentDataSchema = z.object({
  orderId: z.string().min(1),
  amount: z.number().positive(),
  currency: z.string().regex(/^[A-Z]{3}$/),
  customerId: z.string().min(1),
});

const refundDataSchema = z.object({
  paymentId: z.string().min(1),
  orderId: z.string().min(1),
  amount: z.number().positive(),
  reason: z.string().min(1),
});

export const handlerDataSchemas = {
  [JobHandlerType.EMAIL]: emailDataSchema,
  [JobHandlerType.REPORT_GENERATION]: reportDataSchema,
  [JobHandlerType.NOTIFICATION_CLEANUP]: notificationCleanupDataSchema,
  [JobHandlerType.WEBHOOK_TRIGGER]: webhookDataSchema,
  [JobHandlerType.CRM_SYNC]: crmSyncDataSchema,
  [JobHandlerType.PAYMENT]: paymentDataSchema,
  [JobHandlerType.REFUND]: refundDataSchema,
} as const;

export const createJobEnvelopeSchema = z
  .object({
    type: z.enum([
      JobHandlerType.EMAIL,
      JobHandlerType.REPORT_GENERATION,
      JobHandlerType.NOTIFICATION_CLEANUP,
      JobHandlerType.WEBHOOK_TRIGGER,
      JobHandlerType.CRM_SYNC,
      JobHandlerType.PAYMENT,
      JobHandlerType.REFUND,
    ]),
    data: z.record(z.string(), z.unknown()),
  })
  .superRefine((value, ctx) => {
    const schema = handlerDataSchemas[value.type];
    const result = schema.safeParse(value.data);
    if (!result.success) {
      for (const issue of result.error.issues) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['data', ...issue.path],
          message: issue.message,
        });
      }
    }
  });

export type CreateJobEnvelope = z.infer<typeof createJobEnvelopeSchema>;

export function validateHandlerData(type: JobHandlerType, data: unknown) {
  return handlerDataSchemas[type].safeParse(data);
}
