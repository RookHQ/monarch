import { describe, expect, it } from 'vitest';
import { maxOf, minOf } from './math.js';

describe('minOf / maxOf', () => {
  it('match Math.min and Math.max on small inputs', () => {
    expect(minOf([3, -2, 7])).toBe(-2);
    expect(maxOf([3, -2, 7])).toBe(7);
    expect(minOf([])).toBe(Infinity);
    expect(maxOf([])).toBe(-Infinity);
    expect(maxOf([], 0)).toBe(0);
    expect(minOf([Number.NaN, 4])).toBe(4);
  });

  it('handle arrays far beyond the call-argument limit', () => {
    const huge = Array.from({ length: 500_000 }, (_, i) => i - 1000);
    expect(() => Math.max(...huge)).toThrow(RangeError);
    expect(minOf(huge)).toBe(-1000);
    expect(maxOf(huge)).toBe(498_999);
  });
});
