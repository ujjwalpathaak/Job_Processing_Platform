import { describe, expect, it } from 'vitest';
import { ALLOWED_TRANSITIONS, JobStatus } from '../src/models/enums.js';

describe('ALLOWED_TRANSITIONS', () => {
  it('allows happy path', () => {
    expect(ALLOWED_TRANSITIONS[JobStatus.SCHEDULED]).toContain(JobStatus.PUBLISHED);
    expect(ALLOWED_TRANSITIONS[JobStatus.PUBLISHED]).toContain(JobStatus.PROCESSING);
    expect(ALLOWED_TRANSITIONS[JobStatus.PROCESSING]).toContain(JobStatus.PROCESSED);
  });

  it('blocks terminal transitions', () => {
    expect(ALLOWED_TRANSITIONS[JobStatus.PROCESSED]).toEqual([]);
    expect(ALLOWED_TRANSITIONS[JobStatus.DEAD]).toEqual([]);
  });
});
