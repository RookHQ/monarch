import { describe, expect, it } from 'vitest';
import { diffSequences } from './diff.js';

const script = (a: string, b: string, max?: number) =>
  diffSequences([...a], [...b], max)
    .map((op) => `${{ equal: ' ', delete: '-', insert: '+' }[op.type]}${op.value}`)
    .join('');

describe('diffSequences', () => {
  it('keeps a longest common subsequence', () => {
    expect(script('abcd', 'abxd')).toBe(' a b-c+x d');
    expect(script('', 'ab')).toBe('+a+b');
    expect(script('ab', '')).toBe('-a-b');
    expect(script('', '')).toBe('');
    expect(diffSequences(['x'], ['x'])).toEqual([{ type: 'equal', value: 'x' }]);
  });

  it('puts a deletion before an insertion when both are optimal', () => {
    expect(script('a', 'b')).toBe('-a+b');
    expect(script('ab', 'ba')).toBe('-a b+a');
  });

  it('falls back to delete-all, insert-all above maxPairs', () => {
    expect(script('abc', 'abd', 8)).toBe('-a-b-c+a+b+d');
    expect(script('abc', 'abd', 9)).toBe(' a b-c+d');
  });
});
