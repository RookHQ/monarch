/** SVG path builders for lines, areas and bars. */

export interface Point {
  x: number;
  y: number;
}

export type Curve = 'linear' | 'monotone';

function r(n: number): string {
  return String(Math.round(n * 100) / 100);
}

/** Splits points into runs, breaking at gaps (null), so a missing value never draws a fake line. */
export function segments(points: readonly (Point | null)[]): Point[][] {
  const out: Point[][] = [];
  let run: Point[] = [];
  for (const p of points) {
    if (p) run.push(p);
    else if (run.length) {
      out.push(run);
      run = [];
    }
  }
  if (run.length) out.push(run);
  return out;
}

/** Monotone cubic interpolation in x (Fritsch–Carlson): never overshoots the data. */
function monotone(run: readonly Point[]): string {
  const n = run.length;
  if (n < 3) return straight(run);
  const xs = run.map((p) => p.x);
  const ys = run.map((p) => p.y);
  const dx: number[] = [];
  const slope: number[] = [];
  for (let i = 0; i < n - 1; i++) {
    const d = (xs[i + 1] ?? 0) - (xs[i] ?? 0);
    dx.push(d);
    slope.push(d === 0 ? 0 : ((ys[i + 1] ?? 0) - (ys[i] ?? 0)) / d);
  }
  const t: number[] = [slope[0] ?? 0];
  for (let i = 1; i < n - 1; i++) {
    const s0 = slope[i - 1] ?? 0;
    const s1 = slope[i] ?? 0;
    const h0 = dx[i - 1] ?? 0;
    const h1 = dx[i] ?? 0;
    t.push(s0 * s1 <= 0 ? 0 : (3 * (h0 + h1)) / ((2 * h1 + h0) / s0 + (h1 + 2 * h0) / s1));
  }
  t.push(slope[n - 2] ?? 0);
  let d = `M${r(xs[0] ?? 0)},${r(ys[0] ?? 0)}`;
  for (let i = 0; i < n - 1; i++) {
    const ax = xs[i] ?? 0;
    const ay = ys[i] ?? 0;
    const bx = xs[i + 1] ?? 0;
    const by = ys[i + 1] ?? 0;
    const h = (dx[i] ?? 0) / 3;
    d += `C${r(ax + h)},${r(ay + h * (t[i] ?? 0))},${r(bx - h)},${r(by - h * (t[i + 1] ?? 0))},${r(bx)},${r(by)}`;
  }
  return d;
}

function straight(run: readonly Point[]): string {
  return run.map((p, i) => `${i ? 'L' : 'M'}${r(p.x)},${r(p.y)}`).join('');
}

function runPath(run: readonly Point[], curve: Curve): string {
  return curve === 'monotone' ? monotone(run) : straight(run);
}

/** A line through the points; each gap starts a new sub-path. Single points are left to markers. */
export function linePath(points: readonly (Point | null)[], curve: Curve = 'linear'): string {
  return segments(points)
    .filter((run) => run.length > 1)
    .map((run) => runPath(run, curve))
    .join('');
}

/** A filled area between the line and `baseline` (a y pixel), one closed shape per run. */
export function areaPath(
  points: readonly (Point | null)[],
  baseline: number,
  curve: Curve = 'linear',
): string {
  return segments(points)
    .filter((run) => run.length > 1)
    .flatMap((run) => {
      const first = run[0];
      const last = run[run.length - 1];
      if (!first || !last) return [];
      return `${runPath(run, curve)}L${r(last.x)},${r(baseline)}L${r(first.x)},${r(baseline)}Z`;
    })
    .join('');
}

/** Points that have no neighbour in their run: drawn as dots so a lone value is not lost. */
export function isolatedPoints(points: readonly (Point | null)[]): Point[] {
  return segments(points).flatMap((run) => (run.length === 1 && run[0] ? [run[0]] : []));
}

/**
 * A bar with only its far end rounded (the end away from the baseline), as a
 * path. `end` is the side the value grows towards.
 */
export function barPath(
  x: number,
  y: number,
  width: number,
  height: number,
  radius: number,
  end: 'top' | 'bottom' | 'left' | 'right',
): string {
  if (width <= 0 || height <= 0) return '';
  const rad = Math.max(0, Math.min(radius, width / 2, height / 2));
  const x1 = x + width;
  const y1 = y + height;
  if (rad === 0) return `M${r(x)},${r(y)}H${r(x1)}V${r(y1)}H${r(x)}Z`;
  switch (end) {
    case 'top':
      return `M${r(x)},${r(y1)}V${r(y + rad)}Q${r(x)},${r(y)} ${r(x + rad)},${r(y)}H${r(x1 - rad)}Q${r(x1)},${r(y)} ${r(x1)},${r(y + rad)}V${r(y1)}Z`;
    case 'bottom':
      return `M${r(x)},${r(y)}V${r(y1 - rad)}Q${r(x)},${r(y1)} ${r(x + rad)},${r(y1)}H${r(x1 - rad)}Q${r(x1)},${r(y1)} ${r(x1)},${r(y1 - rad)}V${r(y)}Z`;
    case 'right':
      return `M${r(x)},${r(y)}H${r(x1 - rad)}Q${r(x1)},${r(y)} ${r(x1)},${r(y + rad)}V${r(y1 - rad)}Q${r(x1)},${r(y1)} ${r(x1 - rad)},${r(y1)}H${r(x)}Z`;
    case 'left':
      return `M${r(x1)},${r(y)}H${r(x + rad)}Q${r(x)},${r(y)} ${r(x)},${r(y + rad)}V${r(y1 - rad)}Q${r(x)},${r(y1)} ${r(x + rad)},${r(y1)}H${r(x1)}Z`;
  }
}
