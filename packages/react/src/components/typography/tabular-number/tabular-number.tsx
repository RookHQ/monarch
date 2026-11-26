import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';

export interface FormatTabularNumberOptions extends Intl.NumberFormatOptions {
  /** BCP 47 locale, e.g. `"de-DE"`. Default: the runtime's locale. */
  locale?: string | readonly string[] | undefined;
}

const cache = new Map<string, Intl.NumberFormat>();

function formatterFor({ locale, ...options }: FormatTabularNumberOptions): Intl.NumberFormat {
  const key = JSON.stringify([locale ?? null, options]);
  let f = cache.get(key);
  if (!f) {
    try {
      f = new Intl.NumberFormat(locale as string | string[] | undefined, options);
    } catch (error) {
      // A locale tag the runtime rejects ("en_US") falls back to the runtime's
      // own locale; anything else (a bad option) is the caller's bug.
      if (locale === undefined) throw error;
      f = new Intl.NumberFormat(undefined, options);
    }
    if (cache.size > 100) cache.clear();
    cache.set(key, f);
  }
  return f;
}

/**
 * Formats a number with `Intl.NumberFormat`, reusing formatters across
 * calls. Use it for strings outside the component (tooltips, `aria-label`,
 * CSV) so they match what TabularNumber shows.
 *
 *   formatTabularNumber(1234.5, { locale: 'en-US', minimumFractionDigits: 2 }) // "1,234.50"
 *   formatTabularNumber(0.125, { style: 'percent' })                           // "13%"
 */
export function formatTabularNumber(
  value: number | bigint,
  options: FormatTabularNumberOptions = {},
): string {
  return formatterFor(options).format(value);
}

export interface TabularNumberMessages {
  /** Shown when `value` is null, undefined or not a finite number. Default "—". */
  empty: string;
  /** Accessible name of the empty marker. Default "No value". */
  emptyLabel: string;
}

export const TABULAR_NUMBER_MESSAGES: TabularNumberMessages = {
  empty: '—',
  emptyLabel: 'No value',
};

export interface TabularNumberProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  /**
   * The number to format with `Intl.NumberFormat`. Leave it out and pass
   * `children` to show text that is already formatted.
   */
  value?: number | bigint | null | undefined;
  /** BCP 47 locale. Default: the runtime's locale. Pass it when rendering on the server, so the server and browser agree. */
  locale?: string | readonly string[] | undefined;
  /**
   * `Intl.NumberFormat` options: `style` (`decimal`, `percent`, `currency`,
   * `unit`), `currency`, `unit`, `notation`, `signDisplay`, fraction digits…
   * For money with Monarch's currency rules, use Money.
   */
  format?: Intl.NumberFormatOptions | undefined;
  /** Already formatted content, used when `value` is not given. */
  children?: ReactNode;
  /** Visible and spoken strings, for translation. */
  messages?: Partial<TabularNumberMessages> | undefined;
}

const isEmpty = (v: number | bigint | null | undefined): boolean =>
  v === null || v === undefined || (typeof v === 'number' && !Number.isFinite(v));

/**
 * A number set in tabular (fixed-width) digits, so columns of figures line
 * up and values that update in place do not jitter. Formats `value` with
 * `Intl.NumberFormat`, or wraps already formatted `children`. A missing value
 * shows an em dash with an accessible "No value" name.
 */
export const TabularNumber = forwardRef<HTMLSpanElement, TabularNumberProps>(function TabularNumber(
  { value, locale, format, children, messages: messagesProp, className, ...rest },
  ref,
) {
  const messages = { ...TABULAR_NUMBER_MESSAGES, ...messagesProp };
  const hasValue = value !== undefined;
  if (hasValue && isEmpty(value)) {
    return (
      <span ref={ref} {...rest} data-empty="" className={cx('mn-tabular-number', className)}>
        <span aria-hidden>{messages.empty}</span>
        <span className="mn-visually-hidden">{messages.emptyLabel}</span>
      </span>
    );
  }
  const text = hasValue
    ? formatTabularNumber(value as number | bigint, { ...format, locale })
    : children;
  return (
    <span
      ref={ref}
      {...rest}
      data-value={hasValue ? String(value) : undefined}
      className={cx('mn-tabular-number', className)}
    >
      {text}
    </span>
  );
});
