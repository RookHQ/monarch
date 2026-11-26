import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { formatNumber } from '../../../internal/format-number.js';
import { formatMoney } from '../../billing/money/index.js';
import { DeltaIndicator, type DeltaIndicatorProps } from '../../charts/delta-indicator/index.js';

/** The words Metric shows and speaks. Override them to translate. */
export interface MetricMessages {
  /** Spoken for a missing value, which is shown as a dash. Default "No data". */
  empty: string;
}

const DEFAULT_MESSAGES: MetricMessages = { empty: 'No data' };

export interface MetricProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** What the number measures, e.g. "Open tickets". Always visible. */
  label: ReactNode;
  /**
   * The number. A `number` is formatted by `format`; a string or element is
   * shown as given ("1.2K", "—"). `null` or `undefined` shows a dash that is
   * read as `messages.empty`.
   */
  value: number | ReactNode;
  /**
   * How a numeric `value` is written with `Intl.NumberFormat`: `number`
   * (default), `percent` (a ratio: 0.12 is 12%) or `currency` (needs `currency`).
   */
  format?: 'number' | 'percent' | 'currency' | undefined;
  /** ISO 4217 code for `format="currency"`, e.g. `EUR`. */
  currency?: string | undefined;
  /** BCP 47 locale for formatting. Default: the runtime locale. */
  locale?: string | undefined;
  /**
   * `compact` writes 1.2K or €3.4M and gives the full value to assistive
   * technology and as a hover title. Default `standard`.
   */
  notation?: 'standard' | 'compact' | undefined;
  /** Extra `Intl.NumberFormat` options, e.g. `{ maximumFractionDigits: 1 }`. */
  formatOptions?: Intl.NumberFormatOptions | undefined;
  /** Unit written smaller after the value, e.g. `ms`, `GB` or `/ month`. */
  unit?: ReactNode;
  /**
   * The change versus an earlier period, drawn by DeltaIndicator, e.g.
   * `{ value: 0.08, comparison: 'vs last week' }` or
   * `{ current, previous, goodDirection: 'down' }`.
   */
  change?: Omit<DeltaIndicatorProps, 'ref'> | undefined;
  /** `sm` for dense rows, `md` (default), `lg` for a headline figure. */
  size?: 'sm' | 'md' | 'lg' | undefined;
  /**
   * `stacked` (default) puts the label above the value; `inline` puts the
   * label, value and change on one line, for toolbars and table footers.
   */
  layout?: 'stacked' | 'inline' | undefined;
  /** Text alignment in the stacked layout. Use `end` in numeric table columns. Default `start`. */
  align?: 'start' | 'end' | undefined;
  /** Words to show and speak; merged over the English defaults. */
  messages?: Partial<MetricMessages> | undefined;
}

function isEmpty(value: unknown) {
  return value === null || value === undefined || value === false || value === '';
}

/** Formats a number the way Metric shows it; exported for tables that need the same text. */
export function formatMetricValue(
  value: number,
  options: Pick<MetricProps, 'format' | 'currency' | 'locale' | 'notation' | 'formatOptions'> = {},
): string {
  const { format = 'number', currency, locale, notation = 'standard', formatOptions } = options;
  if (format === 'currency' && currency) {
    return formatMoney(value, currency, {
      ...(locale !== undefined && { locale }),
      notation,
      ...(formatOptions?.minimumFractionDigits !== undefined && {
        minimumFractionDigits: formatOptions.minimumFractionDigits,
      }),
      ...(formatOptions?.maximumFractionDigits !== undefined && {
        maximumFractionDigits: formatOptions.maximumFractionDigits,
      }),
    });
  }
  return formatNumber(value, {
    kind: format === 'percent' ? 'percent' : 'number',
    notation,
    ...(locale !== undefined && { locale }),
    ...(formatOptions !== undefined && { options: formatOptions }),
  });
}

/**
 * A single number with its label, an optional unit and an optional change:
 * "Open tickets 128 ↑ 12%". Formatting uses `Intl.NumberFormat`. It has no
 * card surface; use StatCard for a dashboard tile.
 */
export const Metric = forwardRef<HTMLDivElement, MetricProps>(function Metric(
  {
    label,
    value,
    format,
    currency,
    locale,
    notation = 'standard',
    formatOptions,
    unit,
    change,
    size = 'md',
    layout = 'stacked',
    align = 'start',
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const empty = isEmpty(value);
  let shown: ReactNode;
  if (empty) {
    shown = (
      <>
        <span aria-hidden="true">—</span>
        <span className="mn-visually-hidden">{messages.empty}</span>
      </>
    );
  } else if (typeof value === 'number') {
    const opts = { format, currency, locale, formatOptions };
    const text = formatMetricValue(value, { ...opts, notation });
    const full = notation === 'compact' ? formatMetricValue(value, opts) : text;
    shown =
      full !== text ? (
        <span title={full}>
          <span aria-hidden="true">{text}</span>
          <span className="mn-visually-hidden">{full}</span>
        </span>
      ) : (
        text
      );
  } else {
    shown = value;
  }
  const hasUnit = !empty && unit !== undefined && unit !== null && unit !== false && unit !== '';
  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-metric', className)}
      data-size={size}
      data-layout={layout}
      data-align={align}
      data-empty={empty || undefined}
    >
      <span className="mn-metric__label">{label}</span>
      <span className="mn-metric__figure">
        <span className="mn-metric__value">{shown}</span>
        {hasUnit ? <span className="mn-metric__unit"> {unit}</span> : null}
      </span>
      {change ? (
        <DeltaIndicator
          size={size === 'lg' ? 'md' : 'sm'}
          {...change}
          {...(locale !== undefined && change.locale === undefined && { locale })}
          className={cx('mn-metric__change', change.className)}
        />
      ) : null}
    </div>
  );
});
