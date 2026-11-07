import type { Request, Response } from 'express';
import { failure, success } from '../models/apiResponse.js';
import { createJobEnvelopeSchema } from '../models/jobPayloads.js';
import {
  createJob,
  getJobDashboardById,
  getJobsForDashboard,
  getJobsForDashboardPaginated,
  type JobQueryOptions,
} from '../services/jobService.js';
import { log } from '../services/logService.js';

const ALLOWED_SORT_BY = ['createdAt', 'updatedAt', 'status', 'jobHandler', 'jobCategory'];
const ALLOWED_SORT_ORDER = ['asc', 'desc'];

function blankToNull(value: unknown): string | null {
  if (typeof value !== 'string' || value.trim() === '') return null;
  return value.trim();
}

function parseQuery(req: Request): { error?: string; options?: JobQueryOptions; page?: number; limit?: number } {
  const sortBy = blankToNull(req.query.sortBy);
  const sortOrder = blankToNull(req.query.sortOrder)?.toLowerCase() ?? null;
  const pageRaw = req.query.page != null ? Number(req.query.page) : null;
  const limitRaw = req.query.limit != null ? Number(req.query.limit) : null;

  if (sortBy && !ALLOWED_SORT_BY.includes(sortBy)) {
    return { error: `Invalid 'sortBy'. Allowed values: ${ALLOWED_SORT_BY.join(', ')}` };
  }
  if (sortOrder && !ALLOWED_SORT_ORDER.includes(sortOrder)) {
    return { error: `Invalid 'sortOrder'. Allowed values: ${ALLOWED_SORT_ORDER.join(', ')}` };
  }
  if (pageRaw != null && (Number.isNaN(pageRaw) || pageRaw < 1)) {
    return { error: "Invalid 'page'. It must be an integer greater than or equal to 1" };
  }
  if (limitRaw != null && (Number.isNaN(limitRaw) || limitRaw < 1 || limitRaw > 200)) {
    return { error: "Invalid 'limit'. It must be an integer between 1 and 200" };
  }
  if (pageRaw != null && limitRaw == null) {
    return { error: "Query param 'limit' is required when 'page' is provided" };
  }

  const page = pageRaw ?? undefined;
  const limit = limitRaw ?? undefined;
  return {
    options: {
      handler: blankToNull(req.query.handler),
      status: blankToNull(req.query.status),
      category: blankToNull(req.query.category),
      search: blankToNull(req.query.search),
      sortBy,
      sortOrder: (sortOrder as 'asc' | 'desc' | null) ?? null,
      limit: limit ?? null,
      offset: page != null && limit != null ? (page - 1) * limit : null,
    },
    page,
    limit,
  };
}

export async function createJobHandler(req: Request, res: Response): Promise<void> {
  const parsed = createJobEnvelopeSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json(failure('Invalid job payload', parsed.error.flatten()));
    return;
  }

  try {
    const jobId = await createJob(parsed.data.type, parsed.data.data);
    res.json(success('Job is queued', jobId));
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Failed to create job';
    log.error('event=api.job.create_failed reason={}', message);
    res.status(400).json(failure(message));
  }
}

export async function listJobsHandler(req: Request, res: Response): Promise<void> {
  log.info('event=api.job.list_received page={}, limit={}', req.query.page, req.query.limit);
  const parsed = parseQuery(req);
  if (parsed.error || !parsed.options) {
    res.status(400).json(failure(parsed.error ?? 'Invalid query'));
    return;
  }

  if (parsed.page != null && parsed.limit != null) {
    const result = await getJobsForDashboardPaginated(parsed.options, parsed.page, parsed.limit);
    res.json(success('Jobs fetched successfully', result));
    return;
  }

  const jobs = await getJobsForDashboard(parsed.options);
  res.json(success('Jobs fetched successfully', jobs));
}

export async function listJobUpdatesHandler(req: Request, res: Response): Promise<void> {
  const sinceRaw = blankToNull(req.query.since);
  if (!sinceRaw) {
    res.status(400).json(failure("Query param 'since' is required"));
    return;
  }

  const since = new Date(sinceRaw);
  if (Number.isNaN(since.getTime())) {
    res.status(400).json(failure("Invalid 'since' timestamp"));
    return;
  }

  const parsed = parseQuery(req);
  if (parsed.error || !parsed.options) {
    res.status(400).json(failure(parsed.error ?? 'Invalid query'));
    return;
  }

  if (parsed.page != null && parsed.limit != null) {
    const result = await getJobsForDashboardPaginated(
      parsed.options,
      parsed.page,
      parsed.limit,
      since,
    );
    res.json(success('Updated jobs fetched successfully', result));
    return;
  }

  const jobs = await getJobsForDashboard(parsed.options, since);
  res.json(success('Updated jobs fetched successfully', jobs));
}

export async function getJobHandler(req: Request, res: Response): Promise<void> {
  const id = Number(req.params.id);
  if (!Number.isFinite(id)) {
    res.status(400).json(failure('Invalid job id'));
    return;
  }
  const job = await getJobDashboardById(id);
  if (!job) {
    res.status(404).json(failure('Job not found'));
    return;
  }
  res.json(success('Job fetched successfully', job));
}
