import type { ValueFormatter } from './format.js';

/** Words the generated summaries use; pass translations through a chart's `labels` prop. */
export interface ChartSummaryLabels {
  /** "{count} points from {first} to {last}" */
  points: (count: number, first: string, last: string) => string;
  /** "{series}: low {min}, high {max}, latest {last}" */
  series: (series: string, min: string, max: string, last: string) => string;
  /** "{series}: total {total}, highest {category} at {max}" */
  categories: (series: string, total: string, maxCategory: string, max: string) => string;
  noData: string;
}

export const DEFAULT_SUMMARY_LABELS: ChartSummaryLabels = {
  points: (count, first, last) =>
    count === 1 ? `1 point, ${first}` : `${count} points from ${first} to ${last}`,
  series: (series, min, max, last) => `${series}: low ${min}, high ${max}, latest ${last}`,
  categories: (series, total, maxCategory, max) =>
    `${series}: total ${total}, highest ${maxCategory} at ${max}`,
  noData: 'No data',
};

export interface SeriesStats {
  min: number;
  max: number;
  last: number;
  total: number;
  maxIndex: number;
  count: number;
}

/** Min, max, latest and total of the finite values (gaps ignored), or undefined if none. */
export function seriesStats(values: readonly (number | null)[]): SeriesStats | undefined {
  let min = Infinity;
  let max = -Infinity;
  let last = NaN;
  let total = 0;
  let maxIndex = -1;
  let count = 0;
  values.forEach((v, i) => {
    if (v === null) return;
    count++;
    total += v;
    last = v;
    if (v < min) min = v;
    if (v > max) {
      max = v;
      maxIndex = i;
    }
  });
  return count ? { min, max, last, total, maxIndex, count } : undefined;
}

/** "Sessions: low 1,204, high 3,410, latest 2,960" for one series. */
export function describeTrend(
  label: string,
  values: readonly (number | null)[],
  format: ValueFormatter,
  labels: ChartSummaryLabels,
): string {
  const s = seriesStats(values);
  if (!s) return `${label}: ${labels.noData}`;
  return labels.series(label, format(s.min), format(s.max), format(s.last));
}
