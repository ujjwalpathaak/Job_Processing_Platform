import { describe, expect, it } from 'vitest';
import { createJobEnvelopeSchema } from '../src/models/jobPayloads.js';

describe('createJobEnvelopeSchema', () => {
  it('accepts a valid EMAIL payload', () => {
    const result = createJobEnvelopeSchema.safeParse({
      type: 'EMAIL',
      data: {
        to: 'user@example.com',
        from: 'noreply@example.com',
        subject: 'Welcome',
        content: 'Hello',
      },
    });
    expect(result.success).toBe(true);
  });

  it('rejects EMAIL without subject', () => {
    const result = createJobEnvelopeSchema.safeParse({
      type: 'EMAIL',
      data: {
        to: 'user@example.com',
        from: 'noreply@example.com',
        subject: '',
        content: 'Hello',
      },
    });
    expect(result.success).toBe(false);
  });

  it('accepts PAYMENT and rejects bad currency', () => {
    expect(
      createJobEnvelopeSchema.safeParse({
        type: 'PAYMENT',
        data: {
          orderId: 'ord_1',
          amount: 10,
          currency: 'USD',
          customerId: 'cus_1',
        },
      }).success,
    ).toBe(true);

    expect(
      createJobEnvelopeSchema.safeParse({
        type: 'PAYMENT',
        data: {
          orderId: 'ord_1',
          amount: 10,
          currency: 'usd',
          customerId: 'cus_1',
        },
      }).success,
    ).toBe(false);
  });

  it('rejects unknown type', () => {
    const result = createJobEnvelopeSchema.safeParse({
      type: 'UNKNOWN',
      data: {},
    });
    expect(result.success).toBe(false);
  });
});
