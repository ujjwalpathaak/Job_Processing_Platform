import { Router } from 'express';
import {
  createJobHandler,
  getJobHandler,
  listJobsHandler,
  listJobUpdatesHandler,
} from '../controllers/jobsController.js';

export const jobsRouter = Router();

jobsRouter.post('/new', (req, res, next) => {
  void createJobHandler(req, res).catch(next);
});
jobsRouter.get('/', (req, res, next) => {
  void listJobsHandler(req, res).catch(next);
});
jobsRouter.get('/updates', (req, res, next) => {
  void listJobUpdatesHandler(req, res).catch(next);
});
jobsRouter.get('/:id', (req, res, next) => {
  void getJobHandler(req, res).catch(next);
});
