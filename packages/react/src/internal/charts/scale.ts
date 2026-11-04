/** Linear scales and "nice" ticks. Hand-written so charts carry no dependency. */

export interface LinearScale {
  (value: number): number;
  domain: readonly [number, number];
  range: readonly [number, number];
}

export function linearScale(
  domain: readonly [number, number],
  range: readonly [number, number],
): LinearScale {
  const [d0, d1] = domain;
  const [r0, r1] = range;
  const span = d1 - d0 || 1;
  const fn = ((v: number) => r0 + ((v - d0) / span) * (r1 - r0)) as LinearScale;
  fn.domain = domain;
  fn.range = range;
  return fn;
}

/** A 1, 2, 2.5 or 5 × 10ⁿ step close to `span / count`. */
export function niceStep(span: number, count: number): number {
  if (!(span > 0) || !(count > 0)) return 1;
  const raw = span / count;
  const pow = Math.pow(10, Math.floor(Math.log10(raw)));
  const unit = raw / pow;
  const nice = unit <= 1 ? 1 : unit <= 2 ? 2 : unit <= 2.5 ? 2.5 : unit <= 5 ? 5 : 10;
  return nice * pow;
}

/** Rounds away floating-point noise (0.30000000000000004 → 0.3). */
function clean(v: number): number {
  return Number.parseFloat(v.toPrecision(12));
}

/**
 * Extends `[min, max]` to round tick values and returns the ticks. A flat
 * series (min === max) gets a one-step band around it so the line sits in the
 * middle instead of on an edge.
 */
export function niceTicks(min: number, max: number, count = 5): number[] {
  if (!Number.isFinite(min) || !Number.isFinite(max)) return [0, 1];
  if (min === max) {
    const pad = min === 0 ? 1 : Math.abs(min) * 0.5;
    min -= min > 0 && min - pad < 0 ? min : pad;
    max += pad;
  }
  const step = niceStep(max - min, Math.max(1, count));
  const start = Math.floor(clean(min / step)) * step;
  const end = Math.ceil(clean(max / step)) * step;
  const ticks: number[] = [];
  for (let v = start; v <= end + step / 2; v += step) ticks.push(clean(v));
  return ticks;
}

/** Min and max of the finite numbers, or undefined when there are none. */
export function extent(values: Iterable<number | null | undefined>): [number, number] | undefined {
  let lo = Infinity;
  let hi = -Infinity;
  for (const v of values) {
    if (typeof v !== 'number' || !Number.isFinite(v)) continue;
    if (v < lo) lo = v;
    if (v > hi) hi = v;
  }
  return lo === Infinity ? undefined : [lo, hi];
}

/** Reads a numeric value from a datum; anything else (missing, NaN, null) is a gap. */
export function numberAt(datum: Record<string, unknown> | undefined, key: string): number | null {
  const v = datum?.[key];
  return typeof v === 'number' && Number.isFinite(v) ? v : null;
}

/**
 * Indices of the x labels to show so that labels are at least `minGap` px
 * apart. The first and last points are always labelled when there is room.
 */
export function labelIndices(count: number, plotWidth: number, minGap = 72): number[] {
  if (count <= 0) return [];
  if (count === 1) return [0];
  const fit = Math.max(2, Math.floor(plotWidth / minGap) + 1);
  if (count <= fit) return Array.from({ length: count }, (_, i) => i);
  const every = Math.ceil((count - 1) / (fit - 1));
  const out: number[] = [];
  for (let i = 0; i < count; i += every) out.push(i);
  const last = count - 1;
  const prev = out[out.length - 1] ?? 0;
  if (prev !== last) {
    // Too close to the previous label: label the last point in its place.
    if (last - prev < every) {
      if (out.length > 1) out[out.length - 1] = last;
    } else out.push(last);
  }
  return out;
}

/** Rough text width for layout before the browser can measure (SSR-safe). */
export function estimateTextWidth(text: string, fontSize = 11): number {
  return Math.ceil(text.length * fontSize * 0.6);
}
