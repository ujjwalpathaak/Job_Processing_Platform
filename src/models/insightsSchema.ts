import { z } from 'zod';

export const geminiInsightsSchema = z.object({
  summary: z.string().min(1),
  findings: z.array(z.string()).default([]),
  recommendations: z.array(z.string()).default([]),
  suspectedHandlers: z.array(z.string()).default([]),
});

export type GeminiInsights = z.infer<typeof geminiInsightsSchema>;

export const insightsRequestSchema = z.object({
  hours: z.number().optional(),
  logLines: z.number().optional(),
  jobLimit: z.number().optional(),
  question: z.string().optional(),
  includeRawContext: z.boolean().optional(),
  jobId: z.number().int().positive().optional(),
});

export type InsightsRequest = z.infer<typeof insightsRequestSchema>;
