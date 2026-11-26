/**
 * Day keys for the thread's day separators, cached per timestamp and zone:
 * a streaming reply re-renders the thread every frame, and working out the
 * calendar day of every message each time is wasted work.
 */
import { dayKey } from '../../../internal/day-groups.js';

const MAX_ENTRIES = 5000;
const cache = new Map<string, string>();

/** `dayKey(createdAt, timeZone)`, remembered. */
export function cachedDayKey(createdAt: string, timeZone?: string): string {
  const key = `${timeZone ?? ''}|${createdAt}`;
  let value = cache.get(key);
  if (value === undefined) {
    value = dayKey(createdAt, timeZone);
    if (cache.size >= MAX_ENTRIES) cache.clear();
    cache.set(key, value);
  }
  return value;
}
