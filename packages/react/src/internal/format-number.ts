/**
 * Shared `Intl.NumberFormat` helpers for metric components (StatCard,
 * DeltaIndicator, BarList). Currency goes through Money's `formatMoney`
 * in the component, never here.
 */

/** How a plain number is written: `number` (1,234.5) or `percent` (a ratio: 0.12 is 12%). */
export type NumberKind = 'number' | 'percent';

export interface FormatNumberOptions {
  kind?: NumberKind;
  locale?: string;
  notation?: 'standard' | 'compact';
  /** Extra `Intl.NumberFormat` options, e.g. `{ maximumFractionDigits: 1 }`. */
  options?: Intl.NumberFormatOptions;
}

/** Formats `value` with `Intl.NumberFormat`. Percent defaults to at most one decimal. */
export function formatNumber(value: number, opts: FormatNumberOptions = {}): string {
  const { kind = 'number', locale, notation = 'standard', options } = opts;
  const base: Intl.NumberFormatOptions =
    kind === 'percent' ? { style: 'percent', maximumFractionDigits: 1 } : {};
  return new Intl.NumberFormat(locale, { ...base, notation, ...options }).format(value);
}

export interface FormatShareOptions {
  locale?: string | undefined;
  /** Decimal places. Default 0. */
  maximumFractionDigits?: number | undefined;
}

/**
 * A part of a whole as a percentage: `formatShare(3, 40)` is "8%". A non-zero
 * share under 1% is written "<1%", and one over 99% that is not the whole
 * is ">99%", so rounding never hides a segment.
 */
export function formatShare(part: number, total: number, opts: FormatShareOptions = {}): string {
  const digits = opts.maximumFractionDigits ?? 0;
  const pct = (n: number) =>
    formatNumber(n, {
      kind: 'percent',
      ...(opts.locale !== undefined && { locale: opts.locale }),
      options: { maximumFractionDigits: digits, minimumFractionDigits: 0 },
    });
  if (!(total > 0) || !(part > 0)) return pct(0);
  const ratio = part / total;
  const step = 10 ** -(digits + 2);
  if (ratio < step) return `<${pct(step)}`;
  if (ratio < 1 && ratio > 1 - step) return `>${pct(1 - step)}`;
  return pct(ratio);
}
