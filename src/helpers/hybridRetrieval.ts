export type RankedChunk = {
  id: number;
  content: string;
  sourceType: string;
  sourceName: string;
  requestId: string | null;
  jobId: number | null;
  handler: string | null;
  vectorScore: number;
  keywordScore: number;
  score: number;
};

const STOP_WORDS = new Set([
  'a', 'an', 'the', 'and', 'or', 'of', 'to', 'in', 'on', 'for', 'is', 'are', 'was', 'were',
  'what', 'which', 'with', 'from', 'this', 'that', 'it', 'at', 'as', 'be', 'by', 'should',
  'i', 'me', 'my', 'we', 'our', 'you', 'your',
]);

export function tokenize(text: string): string[] {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9_\-.]+/g, ' ')
    .split(/\s+/)
    .filter((t) => t.length > 2 && !STOP_WORDS.has(t));
}

export function keywordScore(queryTokens: string[], text: string): number {
  if (queryTokens.length === 0 || !text) {
    return 0;
  }
  const hay = text.toLowerCase();
  let hits = 0;
  for (const token of queryTokens) {
    if (hay.includes(token)) {
      hits += 1;
    }
  }
  return hits / queryTokens.length;
}

/** Reciprocal Rank Fusion merge of vector + keyword ranked lists. */
export function fuseRankings(
  vectorRanked: RankedChunk[],
  keywordRanked: RankedChunk[],
  limit: number,
  k = 60,
): RankedChunk[] {
  const scores = new Map<number, RankedChunk>();

  const add = (list: RankedChunk[], field: 'vectorScore' | 'keywordScore') => {
    list.forEach((item, index) => {
      const existing = scores.get(item.id) ?? {
        ...item,
        vectorScore: 0,
        keywordScore: 0,
        score: 0,
      };
      existing[field] = item[field];
      existing.score += 1 / (k + index + 1);
      if (!existing.content) {
        existing.content = item.content;
      }
      scores.set(item.id, existing);
    });
  };

  add(vectorRanked, 'vectorScore');
  add(keywordRanked, 'keywordScore');

  return [...scores.values()]
    .sort((a, b) => b.score - a.score)
    .slice(0, limit);
}
