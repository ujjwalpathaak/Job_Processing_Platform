import type { Request, Response } from 'express';
import { failure, success } from '../models/apiResponse.js';
import { generateInsights } from '../services/aiInsightsService.js';

export async function insightsHandler(req: Request, res: Response): Promise<void> {
  try {
    const body = (req.body ?? {}) as {
      hours?: number;
      logLines?: number;
      jobLimit?: number;
      question?: string;
      includeRawContext?: boolean;
    };
    const data = await generateInsights(body);
    res.json(success('Insights generated', data));
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Failed to generate insights';
    res.status(500).json(failure(message));
  }
}
