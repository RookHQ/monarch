'use client';

import { forwardRef, useMemo } from 'react';
import { cx } from '../../../internal/cx.js';
import { currencyDigits, formatMoney } from '../../billing/money/index.js';
import { NumberInput, roundTo, type NumberInputProps } from '../number-input/index.js';

/** Converts a minor-unit integer (cents) to a major-unit amount: `1999` USD → `19.99`. */
export function fromMinorUnits(minor: number, currency: string): number {
  const digits = currencyDigits(currency);
  return roundTo(Number(`${Math.round(minor)}e-${digits}`), digits);
}

/** Converts a major-unit amount to whole minor units without float loss: `19.99` USD → `1999`. */
export function toMinorUnits(major: number, currency: string): number {
  const digits = currencyDigits(currency);
  return Math.round(Number(`${roundTo(major, digits)}e${digits}`));
}

export interface CurrencyInputProps extends Omit<
  NumberInputProps,
  'formatOptions' | 'format' | 'parse'
> {
  /** ISO 4217 code, e.g. `USD`, `EUR`, `JPY`, `INR`. Sets the symbol and the decimals (JPY 0, USD 2, KWD 3). */
  currency: string;
  /**
   * `value`, `defaultValue`, `min`, `max`, `step`, `largeStep` and
   * `onValueChange` use whole minor units (cents, paise): `1999` with USD is
   * $19.99. Use it when the API stores integers. Default `false` (major units).
   */
  minorUnits?: boolean | undefined;
  /** How the currency is written: `symbol` ($), `narrowSymbol`, `code` (USD) or `name`. Default `symbol`. */
  currencyDisplay?: 'symbol' | 'narrowSymbol' | 'code' | 'name' | undefined;
  /** Allow amounts below zero (refunds, adjustments). Default `false`, which sets `min` to 0. */
  allowNegative?: boolean | undefined;
  /** Show the − and + buttons. Default `false`: amounts are usually typed, not stepped. */
  steppers?: boolean | undefined;
}

/**
 * An amount of money in one currency, typed and shown in the user's locale
 * ("1.234,50 €" in German) with the currency's own number of decimals. Built
 * on NumberInput with Money's formatting, so stepping never drifts and the
 * committed amount is exactly what is shown. Values can be major units
 * (19.99) or integer minor units (1999).
 */
export const CurrencyInput = forwardRef<HTMLInputElement, CurrencyInputProps>(
  function CurrencyInput(
    {
      currency,
      minorUnits = false,
      currencyDisplay,
      allowNegative = false,
      locale,
      value,
      defaultValue,
      onValueChange,
      min,
      max,
      step,
      largeStep,
      className,
      ...rest
    },
    ref,
  ) {
    const major = (n: number | undefined) =>
      n === undefined || !minorUnits ? n : fromMinorUnits(n, currency);
    const majorValue = (n: number | null | undefined) => (n === null ? null : major(n));
    const formatOptions = useMemo<Intl.NumberFormatOptions>(
      () => ({ style: 'currency', currency, ...(currencyDisplay && { currencyDisplay }) }),
      [currency, currencyDisplay],
    );
    const format = (n: number) =>
      formatMoney(n, currency, {
        ...(locale !== undefined && { locale }),
        ...(currencyDisplay !== undefined && { currencyDisplay }),
      });
    const lowest = major(min) ?? (allowNegative ? undefined : 0);
    return (
      <NumberInput
        ref={ref}
        steppers={false}
        {...rest}
        className={cx('mn-currency-input', className)}
        locale={locale}
        formatOptions={formatOptions}
        format={format}
        value={majorValue(value)}
        defaultValue={majorValue(defaultValue)}
        min={lowest}
        max={major(max)}
        step={major(step) ?? 1}
        largeStep={major(largeStep)}
        onValueChange={
          onValueChange &&
          ((n) => onValueChange(n === null || !minorUnits ? n : toMinorUnits(n, currency)))
        }
      />
    );
  },
);
