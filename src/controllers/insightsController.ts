import type { Request, Response } from 'express';
import { failure, success } from '../models/apiResponse.js';
import { insightsRequestSchema } from '../models/insightsSchema.js';
import { generateInsights } from '../services/aiInsightsService.js';

export async function insightsHandler(req: Request, res: Response): Promise<void> {
  try {
    const parsed = insightsRequestSchema.safeParse(req.body ?? {});
    if (!parsed.success) {
      res.status(400).json(failure('Invalid insights request', parsed.error.flatten()));
      return;
    }
    const data = await generateInsights(parsed.data);
    res.json(success('Insights generated', data));
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Failed to generate insights';
    res.status(500).json(failure(message));
  }
}
