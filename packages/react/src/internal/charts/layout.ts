import { estimateTextWidth, niceTicks } from './scale.js';
import type { ValueFormatter } from './format.js';

/** A fixed end of the value axis, or `auto` to fit the data. */
export type ChartDomainBound = number | 'auto';

export interface ValueAxisLayout {
  ticks: number[];
  domain: [number, number];
  /** Width the tick labels need (px), for the left margin. */
  labelWidth: number;
}

/**
 * Ticks and domain for a value axis. `includeZero` keeps the baseline in view
 * (always on for bars, whose length must start at zero).
 */
export function valueAxisLayout(
  extent: [number, number] | undefined,
  {
    includeZero,
    domain = ['auto', 'auto'],
    tickCount,
    format,
  }: {
    includeZero: boolean;
    domain?: readonly [ChartDomainBound, ChartDomainBound] | undefined;
    tickCount: number;
    format: ValueFormatter;
  },
): ValueAxisLayout {
  let [lo, hi] = extent ?? [0, 1];
  if (includeZero) {
    lo = Math.min(lo, 0);
    hi = Math.max(hi, 0);
  }
  if (domain[0] !== 'auto') lo = domain[0];
  if (domain[1] !== 'auto') hi = domain[1];
  let ticks = niceTicks(lo, hi, tickCount);
  if (domain[0] !== 'auto' || domain[1] !== 'auto') {
    const fixedLo = domain[0] === 'auto' ? (ticks[0] ?? lo) : domain[0];
    const fixedHi = domain[1] === 'auto' ? (ticks[ticks.length - 1] ?? hi) : domain[1];
    ticks = ticks.filter((t) => t >= fixedLo && t <= fixedHi);
    return {
      ticks,
      domain: [fixedLo, fixedHi],
      labelWidth: Math.max(0, ...ticks.map((t) => estimateTextWidth(format(t)))),
    };
  }
  return {
    ticks,
    domain: [ticks[0] ?? lo, ticks[ticks.length - 1] ?? hi],
    labelWidth: Math.max(0, ...ticks.map((t) => estimateTextWidth(format(t)))),
  };
}

/** About one value tick per 48px of plot height, at least two. */
export function tickCountFor(length: number): number {
  return Math.max(2, Math.min(8, Math.floor(length / 48)));
}

/** Index of the position closest to `px` in a sorted list of positions. */
export function nearestIndex(positions: readonly number[], px: number): number | null {
  if (positions.length === 0) return null;
  let lo = 0;
  let hi = positions.length - 1;
  while (hi - lo > 1) {
    const mid = (lo + hi) >> 1;
    if ((positions[mid] ?? 0) < px) lo = mid;
    else hi = mid;
  }
  return Math.abs((positions[lo] ?? 0) - px) <= Math.abs((positions[hi] ?? 0) - px) ? lo : hi;
}

/** Interaction words every chart needs; pass translations through a chart's `labels`. */
export interface ChartInteractionLabels {
  /** `aria-roledescription` of the plot. Default "chart". */
  chart: string;
  /** Read after the summary. Default "Use the arrow keys to inspect values." */
  keyboardHint: string;
  /** Shown for a missing value in the tooltip and table. Default "No data". */
  noValue: string;
  /** Total of a stacked bar, in the tooltip and table. Default "Total". */
  total: string;
}

export const DEFAULT_INTERACTION_LABELS: ChartInteractionLabels = {
  chart: 'chart',
  keyboardHint: 'Use the arrow keys to inspect values.',
  noValue: 'No data',
  total: 'Total',
};

/** Shortens a label to about `maxWidth` px, ending in an ellipsis. */
export function truncateLabel(label: string, maxWidth: number, fontSize = 11): string {
  const fit = Math.floor(maxWidth / (fontSize * 0.6));
  if (label.length <= fit) return label;
  return fit <= 1 ? '…' : `${label.slice(0, fit - 1).trimEnd()}…`;
}
