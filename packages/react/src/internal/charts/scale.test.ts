import { describe, expect, it } from 'vitest';
import { createXFormatter } from './format.js';
import { niceTicks } from './scale.js';

describe('niceTicks', () => {
  it('gives evenly spaced ticks that cover the range', () => {
    expect(niceTicks(0, 100)).toEqual([0, 20, 40, 60, 80, 100]);
    const ticks = niceTicks(0.1, 0.7);
    expect(ticks[0]).toBeLessThanOrEqual(0.1);
    expect(ticks.at(-1)).toBeGreaterThanOrEqual(0.7);
    // Counted steps do not drift: every tick is a clean multiple.
    for (const t of ticks) expect(String(t).length).toBeLessThan(6);
  });

  it('ends when the step is below float precision at that magnitude', () => {
    const ticks = niceTicks(1e16, 1e16 + 2);
    expect(ticks.length).toBeGreaterThanOrEqual(2);
    expect(ticks.length).toBeLessThanOrEqual(1001);
  });

  it('falls back for values that are not finite and pads a flat range', () => {
    expect(niceTicks(Number.NaN, 1)).toEqual([0, 1]);
    expect(niceTicks(5, 5).length).toBeGreaterThan(1);
  });
});

describe('createXFormatter', () => {
  it('formats an unreadable date as a gap instead of throwing', () => {
    const format = createXFormatter('en-GB');
    expect(format(new Date(Number.NaN), 0)).toBe('');
    expect(format(new Date(Date.UTC(2026, 10, 4)), 0)).not.toBe('');
    expect(format(12.345, 0)).toBe('12.35');
  });
});
