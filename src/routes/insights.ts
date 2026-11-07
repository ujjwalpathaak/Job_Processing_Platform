import { Router } from 'express';
import { insightsHandler } from '../controllers/insightsController.js';

export const insightsRouter = Router();

insightsRouter.post('/insights', (req, res, next) => {
  void insightsHandler(req, res).catch(next);
});
