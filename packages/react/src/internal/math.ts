/**
 * Loop-based minimum and maximum. `Math.min(...values)` spreads the array into
 * call arguments and throws a RangeError once it holds more than the engine's
 * argument limit (around 100 000 values in V8); these never do.
 */

/** The smallest value, ignoring NaN; `empty` when there is none. Default `Infinity`, like `Math.min()`. */
export function minOf(values: Iterable<number>, empty = Infinity): number {
  let out = Infinity;
  let any = false;
  for (const v of values) {
    if (Number.isNaN(v)) continue;
    any = true;
    if (v < out) out = v;
  }
  return any ? out : empty;
}

/** The largest value, ignoring NaN; `empty` when there is none. Default `-Infinity`, like `Math.max()`. */
export function maxOf(values: Iterable<number>, empty = -Infinity): number {
  let out = -Infinity;
  let any = false;
  for (const v of values) {
    if (Number.isNaN(v)) continue;
    any = true;
    if (v > out) out = v;
  }
  return any ? out : empty;
}
