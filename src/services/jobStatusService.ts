import { prisma } from '../models/db.js';
import { ALLOWED_TRANSITIONS, type JobStatus } from '../models/enums.js';
import type { Job } from '@prisma/client';

export async function updateJobStatus(
  job: Job,
  newStatus: JobStatus,
  errorMessage: string | null = null,
): Promise<Job> {
  const expected = job.status as JobStatus;
  const allowed = ALLOWED_TRANSITIONS[expected];
  if (!allowed.includes(newStatus)) {
    throw new Error(`Invalid transition from ${expected} to ${newStatus}`);
  }

  const result = await prisma.job.updateMany({
    where: { id: job.id, status: expected },
    data: {
      status: newStatus,
      errorMessage,
    },
  });

  if (result.count === 0) {
    console.log(`Skipping status update because job ${job.id} moved from ${expected} already`);
    const current = await prisma.job.findUniqueOrThrow({ where: { id: job.id } });
    return current;
  }

  await prisma.jobStateHistory.create({
    data: {
      jobId: job.id,
      status: newStatus,
      errorMessage,
    },
  });

  return prisma.job.findUniqueOrThrow({ where: { id: job.id } });
}
