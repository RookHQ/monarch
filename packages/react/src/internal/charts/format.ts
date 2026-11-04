/**
 * Number and date formatting for charts. Everything goes through `Intl` with
 * an explicit locale, so axis ticks, tooltips, tables and text summaries agree.
 *
 * Pass `locale` when rendering on the server: without it the runtime default
 * is used, which can differ between the server and the browser.
 */

const numberCache = new Map<string, Intl.NumberFormat>();
const dateCache = new Map<string, Intl.DateTimeFormat>();

function numberFormat(locale: string | undefined, options: Intl.NumberFormatOptions) {
  const key = `${locale ?? ''}|${JSON.stringify(options)}`;
  let f = numberCache.get(key);
  if (!f) {
    f = new Intl.NumberFormat(locale, options);
    numberCache.set(key, f);
  }
  return f;
}

function dateFormat(locale: string | undefined, options: Intl.DateTimeFormatOptions) {
  const key = `${locale ?? ''}|${JSON.stringify(options)}`;
  let f = dateCache.get(key);
  if (!f) {
    f = new Intl.DateTimeFormat(locale, options);
    dateCache.set(key, f);
  }
  return f;
}

export type ValueFormatter = (value: number) => string;
export type XValue = string | number | Date;

/** Full-precision value formatter (tooltips, tables, summaries). */
export function createValueFormatter(
  locale: string | undefined,
  options: Intl.NumberFormatOptions = { maximumFractionDigits: 2 },
  custom?: ValueFormatter,
): ValueFormatter {
  if (custom) return custom;
  const f = numberFormat(locale, options);
  return (v) => f.format(v);
}

/**
 * Axis tick formatter: compact notation (1.2K, 3.4M) unless the caller set a
 * style, so long numbers never crowd the axis. A custom formatter wins.
 */
export function createTickFormatter(
  locale: string | undefined,
  options: Intl.NumberFormatOptions | undefined,
  custom?: ValueFormatter,
): ValueFormatter {
  if (custom) return custom;
  const f = numberFormat(locale, {
    notation: 'compact',
    maximumFractionDigits: 1,
    ...options,
  });
  return (v) => f.format(v);
}

const DEFAULT_DATE: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' };

/** Formats an x value: dates with `Intl.DateTimeFormat`, numbers with `Intl.NumberFormat`. */
export function createXFormatter(
  locale: string | undefined,
  dateOptions: Intl.DateTimeFormatOptions = DEFAULT_DATE,
  custom?: (value: XValue, index: number) => string,
): (value: XValue, index: number) => string {
  if (custom) return custom;
  const d = dateFormat(locale, dateOptions);
  const n = numberFormat(locale, { maximumFractionDigits: 2 });
  return (value) => {
    if (value instanceof Date) return d.format(value);
    if (typeof value === 'number') return n.format(value);
    return String(value);
  };
}
