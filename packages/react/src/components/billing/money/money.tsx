import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { VisuallyHidden } from '../../layout/visually-hidden/index.js';

/** An amount: a number, a bigint (minor units) or an exact decimal string such as `"1234.50"`. */
export type MoneyAmount = number | bigint | string;

export interface FormatMoneyOptions {
  /** BCP 47 locale, e.g. `en-IN`. Default: the runtime locale. */
  locale?: string;
  /** `amount` is in the currency's minor units (cents, paise): `1999` with USD is $19.99. */
  minorUnits?: boolean;
  /** `compact` writes $1.2M. Default `standard`. */
  notation?: 'standard' | 'compact';
  /** `Intl.NumberFormat` sign display. `exceptZero` shows +/− for gains and losses. Default `auto`. */
  signDisplay?: 'auto' | 'always' | 'exceptZero' | 'never';
  /** How the currency is written: `symbol` ($), `narrowSymbol`, `code` (USD) or `name` (US dollars). Default `symbol`. */
  currencyDisplay?: 'symbol' | 'narrowSymbol' | 'code' | 'name';
  /** `accounting` writes negatives in parentheses where the locale does. Default `standard`. */
  currencySign?: 'standard' | 'accounting';
  minimumFractionDigits?: number;
  maximumFractionDigits?: number;
}

/** Number of minor-unit digits for an ISO 4217 currency (USD 2, JPY 0, KWD 3). */
export function currencyDigits(currency: string): number {
  return (
    new Intl.NumberFormat('en', { style: 'currency', currency }).resolvedOptions()
      .maximumFractionDigits ?? 2
  );
}

/** Converts a minor-unit amount to an exact decimal string without floating-point loss. */
function minorToDecimal(amount: MoneyAmount, digits: number): string {
  let raw: string;
  if (typeof amount === 'bigint') raw = amount.toString();
  else if (typeof amount === 'number') raw = Math.round(amount).toString();
  else raw = amount.trim().replace(/\.0*$/, '');
  if (!/^-?\d+$/.test(raw))
    throw new RangeError(`Money: minor units must be an integer, got ${raw}`);
  if (digits === 0) return raw;
  const negative = raw.startsWith('-');
  const digitsOnly = (negative ? raw.slice(1) : raw).padStart(digits + 1, '0');
  const whole = digitsOnly.slice(0, -digits);
  const fraction = digitsOnly.slice(-digits);
  return `${negative ? '-' : ''}${whole}.${fraction}`;
}

/** The value `Intl.NumberFormat` receives: exact decimal strings are passed through. */
function toFormattable(amount: MoneyAmount, currency: string, minorUnits: boolean) {
  if (minorUnits) return minorToDecimal(amount, currencyDigits(currency));
  return typeof amount === 'string' ? amount.trim() : amount;
}

function isNegative(value: number | bigint | string): boolean {
  return typeof value === 'string' ? value.trim().startsWith('-') : value < 0;
}

/** Formats an amount of one currency with `Intl.NumberFormat`. */
export function formatMoney(
  amount: MoneyAmount,
  currency: string,
  options: FormatMoneyOptions = {},
): string {
  const { locale, minorUnits = false, notation = 'standard', ...intl } = options;
  const value = toFormattable(amount, currency, minorUnits);
  const formatter = new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    notation,
    ...intl,
  });
  // ES2023 Intl formats decimal strings exactly; the ES2022 lib types do not know that yet.
  return formatter.format(value as number);
}

export interface MoneyProps
  extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'>, FormatMoneyOptions {
  /** The amount. `null` or `undefined` renders `emptyLabel`. */
  amount: MoneyAmount | null | undefined;
  /** ISO 4217 code, e.g. `USD`, `EUR`, `INR`. */
  currency: string;
  /**
   * Colour negative amounts with the danger text colour (`data-negative` is
   * set either way). The minus sign or parentheses always carry the meaning.
   * Default `false`.
   */
  toneNegative?: boolean;
  /** Shown when `amount` is missing. Default "—", read as "No amount". */
  emptyLabel?: string;
}

/**
 * A formatted amount in tabular figures. With `notation="compact"` the short
 * form is shown and the full amount is given to assistive technology and as
 * a hover title.
 */
export const Money = forwardRef<HTMLSpanElement, MoneyProps>(function Money(
  {
    amount,
    currency,
    locale,
    minorUnits,
    notation = 'standard',
    signDisplay,
    currencyDisplay,
    currencySign,
    minimumFractionDigits,
    maximumFractionDigits,
    toneNegative = false,
    emptyLabel,
    className,
    title,
    ...rest
  },
  ref,
) {
  if (amount === null || amount === undefined) {
    return (
      <span ref={ref} {...rest} className={cx('mn-money', className)} data-empty="">
        {emptyLabel ?? (
          <>
            <span aria-hidden>—</span>
            <VisuallyHidden>No amount</VisuallyHidden>
          </>
        )}
      </span>
    );
  }
  const options: FormatMoneyOptions = {
    ...(locale !== undefined && { locale }),
    ...(minorUnits !== undefined && { minorUnits }),
    ...(signDisplay !== undefined && { signDisplay }),
    ...(currencyDisplay !== undefined && { currencyDisplay }),
    ...(currencySign !== undefined && { currencySign }),
    ...(minimumFractionDigits !== undefined && { minimumFractionDigits }),
    ...(maximumFractionDigits !== undefined && { maximumFractionDigits }),
  };
  const shown = formatMoney(amount, currency, { ...options, notation });
  const compact = notation === 'compact';
  const full = compact ? formatMoney(amount, currency, options) : shown;
  const negative = isNegative(amount);
  return (
    <span
      ref={ref}
      {...rest}
      className={cx('mn-money', className)}
      title={title ?? (compact && full !== shown ? full : undefined)}
      data-currency={currency}
      data-notation={notation}
      data-negative={negative || undefined}
      data-tone={toneNegative && negative ? 'danger' : undefined}
    >
      {compact && full !== shown ? (
        <>
          <span aria-hidden>{shown}</span>
          <VisuallyHidden>{full}</VisuallyHidden>
        </>
      ) : (
        shown
      )}
    </span>
  );
});
