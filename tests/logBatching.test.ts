import { describe, expect, it } from 'vitest';

// Pure helper behavior mirrored from logBatchingService scope rules
function batchScope(message: {
  requestId: string | null;
  jobId?: number | null;
}): string | null {
  if (message.requestId) return `req:${message.requestId}`;
  if (message.jobId != null) return `job:${message.jobId}`;
  return null;
}

describe('log batch scope', () => {
  it('prefers requestId over jobId', () => {
    expect(batchScope({ requestId: 'abc', jobId: 7 })).toBe('req:abc');
  });

  it('uses jobId when requestId is missing', () => {
    expect(batchScope({ requestId: null, jobId: 42 })).toBe('job:42');
  });

  it('skips uncorrelated logs', () => {
    expect(batchScope({ requestId: null, jobId: null })).toBeNull();
  });
});
