/**
 * Series colours. Charts only ever use the categorical data tokens
 * (`--mn-color-data-1` … `-5`, contrast-checked per theme in
 * @rookcom/monarch-tokens) plus one neutral for "Other" or comparison series.
 * Status colours (danger, success, warning) are never series colours.
 */

/** A palette slot (1–5) or `neutral` for "Other", totals or a comparison period. */
export type ChartColor = 1 | 2 | 3 | 4 | 5 | 'neutral';

export const CHART_PALETTE_SIZE = 5;

/** The CSS colour for a slot: always a Monarch token. */
export function chartColor(color: ChartColor): string {
  return color === 'neutral' ? 'var(--mn-color-text-subtle)' : `var(--mn-color-data-${color})`;
}

/**
 * The default slot for the series at `index`: slots are assigned in order and
 * never cycled. A sixth series and later get `neutral`; fold them into
 * "Other" or split the chart instead.
 */
export function defaultChartColor(index: number): ChartColor {
  return index < CHART_PALETTE_SIZE ? ((index + 1) as ChartColor) : 'neutral';
}
