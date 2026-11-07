import { Prisma } from '@prisma/client';
import { getHandler } from '../handlers/handlerRegistry.js';
import { prisma } from '../models/db.js';
import type { JobMessage } from '../models/jobMessage.js';
import {
  JobHandlerType,
  JobStatus,
  type JobCategory,
  type JobStatus as JobStatusType,
} from '../models/enums.js';
import { publishJob } from '../producers/jobProducer.js';
import { updateJobStatus } from './jobStatusService.js';

export type JobQueryOptions = {
  handler?: string | null;
  status?: string | null;
  category?: string | null;
  search?: string | null;
  sortBy?: string | null;
  sortOrder?: 'asc' | 'desc' | null;
  limit?: number | null;
  offset?: number | null;
};

function serializeJob(job: {
  id: bigint;
  status: string;
  jobCategory: string;
  jobHandler: string;
  createdAt: Date;
  updatedAt: Date;
  data: Prisma.JsonValue;
  errorMessage: string | null;
}) {
  return {
    id: Number(job.id),
    status: job.status,
    jobCategory: job.jobCategory,
    jobHandler: job.jobHandler,
    createdAt: job.createdAt.toISOString(),
    updatedAt: job.updatedAt.toISOString(),
    data: job.data,
    errorMessage: job.errorMessage,
  };
}

async function attachHistory(jobIds: bigint[]) {
  if (jobIds.length === 0) {
    return new Map<string, { status: string; timestamp: string; error: string | null }[]>();
  }
  const rows = await prisma.jobStateHistory.findMany({
    where: { jobId: { in: jobIds } },
    orderBy: { createdAt: 'asc' },
  });
  const map = new Map<string, { status: string; timestamp: string; error: string | null }[]>();
  for (const row of rows) {
    const key = row.jobId.toString();
    const list = map.get(key) ?? [];
    list.push({
      status: row.status,
      timestamp: row.createdAt.toISOString(),
      error: row.errorMessage,
    });
    map.set(key, list);
  }
  return map;
}

function buildWhere(options: JobQueryOptions, since?: Date | null): Prisma.JobWhereInput {
  const where: Prisma.JobWhereInput = {};

  if (options.handler) {
    if (!(options.handler in JobHandlerType)) {
      return { id: -1n };
    }
    where.jobHandler = options.handler as keyof typeof JobHandlerType;
  }
  if (options.status) {
    if (!(options.status in JobStatus)) {
      return { id: -1n };
    }
    where.status = options.status as JobStatusType;
  }
  if (options.category) {
    where.jobCategory = options.category as JobCategory;
  }
  if (options.search) {
    where.OR = [
      { errorMessage: { contains: options.search, mode: 'insensitive' } },
      { jobHandler: { equals: options.search as never } },
    ];
  }
  if (since) {
    where.updatedAt = { gte: since };
  }
  return where;
}

const SORT_FIELDS = new Set(['createdAt', 'updatedAt', 'status', 'jobHandler', 'jobCategory']);

export async function createJob(type: keyof typeof JobHandlerType, data: Record<string, unknown>) {
  const handler = getHandler(type);
  let job = await prisma.job.create({
    data: {
      status: JobStatus.SCHEDULED,
      jobCategory: handler.category(),
      jobHandler: handler.identify(),
      data: data as Prisma.InputJsonValue,
    },
  });

  await prisma.jobStateHistory.create({
    data: { jobId: job.id, status: JobStatus.SCHEDULED, errorMessage: null },
  });

  const message: JobMessage = {
    jobId: Number(job.id),
    jobCategory: handler.category(),
    jobHandler: handler.identify(),
    data,
  };

  await publishJob(message);
  job = await updateJobStatus(job, JobStatus.PUBLISHED);
  return Number(job.id);
}

export async function getJobById(id: number) {
  return prisma.job.findUnique({ where: { id: BigInt(id) } });
}

export async function getJobDashboardById(id: number) {
  const job = await prisma.job.findUnique({ where: { id: BigInt(id) } });
  if (!job) {
    return null;
  }
  const history = await attachHistory([job.id]);
  return {
    ...serializeJob(job),
    history: history.get(job.id.toString()) ?? [],
  };
}

export async function getJobsForDashboard(options: JobQueryOptions, since?: Date | null) {
  const where = buildWhere(options, since);
  const sortBy = options.sortBy && SORT_FIELDS.has(options.sortBy) ? options.sortBy : 'createdAt';
  const sortOrder = options.sortOrder === 'asc' ? 'asc' : 'desc';

  const jobs = await prisma.job.findMany({
    where,
    orderBy: { [sortBy]: sortOrder },
    take: options.limit ?? undefined,
    skip: options.offset ?? undefined,
  });

  const history = await attachHistory(jobs.map((j) => j.id));
  return jobs.map((job) => ({
    ...serializeJob(job),
    history: history.get(job.id.toString()) ?? [],
  }));
}

export async function getJobsForDashboardPaginated(
  options: JobQueryOptions,
  page: number,
  limit: number,
  since?: Date | null,
) {
  const where = buildWhere(options, since);
  const total = await prisma.job.count({ where });
  const items = await getJobsForDashboard(
    { ...options, limit, offset: (page - 1) * limit },
    since,
  );
  const totalPages = Math.ceil(total / limit) || 0;
  return {
    items,
    meta: {
      total,
      page,
      limit,
      totalPages,
      hasMore: page < totalPages,
    },
  };
}
