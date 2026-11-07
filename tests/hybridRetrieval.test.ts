import { describe, expect, it } from 'vitest';
import {
  fuseRankings,
  keywordScore,
  tokenize,
  type RankedChunk,
} from '../src/helpers/hybridRetrieval.js';

function chunk(id: number, content: string, keywordScoreValue = 0, vectorScore = 0): RankedChunk {
  return {
    id,
    content,
    sourceType: 'log',
    sourceName: 'test',
    requestId: null,
    jobId: id,
    handler: 'EMAIL',
    vectorScore,
    keywordScore: keywordScoreValue,
    score: 0,
  };
}

describe('hybridRetrieval', () => {
  it('tokenizes and drops stop words', () => {
    expect(tokenize('What failed in the payment handler?')).toEqual(
      expect.arrayContaining(['failed', 'payment', 'handler']),
    );
  });

  it('scores keyword overlap', () => {
    const tokens = tokenize('payment gateway error');
    expect(keywordScore(tokens, 'ERROR payment gateway timeout')).toBeGreaterThan(0.5);
    expect(keywordScore(tokens, 'email sent successfully')).toBeLessThan(0.5);
  });

  it('fuses vector and keyword rankings', () => {
    const vector = [chunk(1, 'a', 0, 0.9), chunk(2, 'b', 0, 0.8)];
    const keyword = [chunk(2, 'b', 0.9, 0), chunk(3, 'c', 0.7, 0)];
    const fused = fuseRankings(vector, keyword, 3);
    expect(fused.map((c) => c.id)).toContain(2);
    expect(fused[0].score).toBeGreaterThan(0);
  });
});
