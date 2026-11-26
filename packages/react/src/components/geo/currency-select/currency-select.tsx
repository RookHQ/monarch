'use client';

import { forwardRef, useMemo, useState } from 'react';
import { cx } from '../../../internal/cx.js';
import { safeCollator } from '../../../internal/collation.js';
import {
  Combobox,
  type ComboboxMessages,
  type ComboboxOption,
  type ComboboxProps,
} from '../../forms/combobox/index.js';

/**
 * Active ISO 4217 currency codes in circulation (no funds, metals, test codes
 * or retired currencies such as HRK). Pass `currencies` to use another list.
 */
export const CURRENCY_CODES: readonly string[] = (
  'AED AFN ALL AMD AOA ARS AUD AWG AZN BAM BBD BDT BHD BIF BMD BND BOB BRL BSD BTN BWP BYN BZD ' +
  'CAD CDF CHF CLP CNY COP CRC CUP CVE CZK DJF DKK DOP DZD EGP ERN ETB EUR FJD FKP GBP GEL GHS ' +
  'GIP GMD GNF GTQ GYD HKD HNL HTG HUF IDR ILS INR IQD IRR ISK JMD JOD JPY KES KGS KHR KMF KPW ' +
  'KRW KWD KYD KZT LAK LBP LKR LRD LSL LYD MAD MDL MGA MKD MMK MNT MOP MRU MUR MVR MWK MXN MYR ' +
  'MZN NAD NGN NIO NOK NPR NZD OMR PAB PEN PGK PHP PKR PLN PYG QAR RON RSD RUB RWF SAR SBD SCR ' +
  'SDG SEK SGD SHP SLE SOS SRD SSP STN SVC SYP SZL THB TJS TMT TND TOP TRY TTD TWD TZS UAH UGX ' +
  'USD UYU UZS VED VES VND VUV WST XAF XCD XCG XOF XPF YER ZAR ZMW ZWG'
).split(' ');

const namesCache = new Map<string, Intl.DisplayNames | null>();
function currencyNames(locale: string): Intl.DisplayNames | null {
  if (!namesCache.has(locale)) {
    try {
      namesCache.set(locale, new Intl.DisplayNames([locale], { type: 'currency' }));
    } catch {
      namesCache.set(locale, null);
    }
  }
  return namesCache.get(locale) ?? null;
}

/**
 * The currency's name in `locale` from `Intl.DisplayNames`, such as
 * `getCurrencyName('EUR', 'de')` → "Euro". Returns the upper-case code when
 * the runtime has no name or no `Intl.DisplayNames`.
 */
export function getCurrencyName(code: string, locale = 'en'): string {
  const upper = code.toUpperCase();
  try {
    return currencyNames(locale)?.of(upper) ?? upper;
  } catch {
    return upper;
  }
}

const symbolCache = new Map<string, string>();

/**
 * The currency's symbol in `locale` from `Intl.NumberFormat`: `'narrow'`
 * gives "$" for USD everywhere, `'standard'` gives "US$" outside the US.
 * Returns the code when the runtime has no symbol for it.
 */
export function getCurrencySymbol(
  code: string,
  locale = 'en',
  display: 'narrow' | 'standard' = 'narrow',
): string {
  const upper = code.toUpperCase();
  // Building an Intl.NumberFormat is slow, and the picker asks for every currency's
  // symbol each time its options are rebuilt: keep each answer.
  const key = `${locale}|${display}|${upper}`;
  const known = symbolCache.get(key);
  if (known !== undefined) return known;
  let symbol = upper;
  try {
    const parts = new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: upper,
      currencyDisplay: display === 'narrow' ? 'narrowSymbol' : 'symbol',
    }).formatToParts(0);
    symbol = parts.find((p) => p.type === 'currency')?.value ?? upper;
  } catch {
    symbol = upper;
  }
  symbolCache.set(key, symbol);
  return symbol;
}

/** One currency, as listed by `listCurrencies`. */
export interface CurrencyInfo {
  /** ISO 4217 code, upper case. */
  code: string;
  /** Name in the requested locale. */
  name: string;
  /** Narrow symbol in the requested locale ("$", "€", "₹"); the code when there is none. */
  symbol: string;
}

/** Currencies with names and symbols in `locale`, sorted by name. Default: every code in `CURRENCY_CODES`. */
export function listCurrencies(
  options: { locale?: string | undefined; codes?: readonly string[] | undefined } = {},
): CurrencyInfo[] {
  const locale = options.locale ?? 'en';
  // Never throws on an unknown locale.
  const collator = safeCollator(locale);
  const codes = [...new Set((options.codes ?? CURRENCY_CODES).map((c) => c.toUpperCase()))];
  return codes
    .map((code) => ({
      code,
      name: getCurrencyName(code, locale),
      symbol: getCurrencySymbol(code, locale),
    }))
    .sort((a, b) => collator.compare(a.name, b.name));
}

/** Interface text, for translation. Every message has an English default. */
export interface CurrencySelectMessages {
  /** Heading over `priority` currencies. Default "Suggested". */
  suggested: string;
  /** Heading over the rest when `priority` is set. Default "All currencies". */
  all: string;
  /** Shown when nothing matches. Default "No currencies match". */
  empty: string;
}

const DEFAULT_MESSAGES: CurrencySelectMessages = {
  suggested: 'Suggested',
  all: 'All currencies',
  empty: 'No currencies match',
};

export interface CurrencySelectProps extends Omit<
  ComboboxProps,
  | 'options'
  | 'value'
  | 'defaultValue'
  | 'onValueChange'
  | 'multiple'
  | 'values'
  | 'defaultValues'
  | 'onValuesChange'
  | 'valueOptions'
  | 'onSearch'
  | 'onCreate'
  | 'filter'
  | 'messages'
> {
  /** Selected ISO 4217 code (controlled), such as `'EUR'`. Any case is accepted; `onValueChange` reports upper case. */
  value?: string | null | undefined;
  /** Initially selected code (uncontrolled). */
  defaultValue?: string | null | undefined;
  /** Called with the chosen upper-case code, or `null` when cleared. */
  onValueChange?: ((code: string | null) => void) | undefined;
  /** BCP 47 locale for names, symbols and sorting. Default `en`, so server and client render the same. */
  locale?: string | undefined;
  /** Limit the list to these codes, such as the currencies the payment provider supports. Default: `CURRENCY_CODES`. */
  currencies?: readonly string[] | undefined;
  /** Codes listed first under "Suggested" while nothing is typed. */
  priority?: readonly string[] | undefined;
  /**
   * What the input shows for the selection. `name` → "Euro", `code` → "EUR",
   * `code-name` → "EUR · Euro". Default `name`. The list always shows the name,
   * with the code and symbol at the end.
   */
  display?: 'name' | 'code' | 'code-name' | undefined;
  /** Show the symbol next to the code in each option. Default true. */
  showSymbol?: boolean | undefined;
  /** Interface text, for translation. Combobox messages can be passed here too. */
  messages?: (Partial<CurrencySelectMessages> & Partial<ComboboxMessages>) | undefined;
}

function matches(option: ComboboxOption, query: string): boolean {
  const haystack =
    `${option.label} ${option.value} ${option.hint ?? ''} ${option.keywords ?? ''}`.toLowerCase();
  return query
    .toLowerCase()
    .split(/\s+/)
    .filter(Boolean)
    .every((term) => haystack.includes(term));
}

/**
 * Searchable currency picker. The value is an ISO 4217 code; names come from
 * `Intl.DisplayNames` and symbols from `Intl.NumberFormat` in `locale`. Search
 * matches the name, code, symbol and English name. Built on Combobox; forwards
 * its ref to the input.
 */
export const CurrencySelect = forwardRef<HTMLInputElement, CurrencySelectProps>(
  function CurrencySelect(
    {
      value: valueProp,
      defaultValue = null,
      onValueChange,
      locale = 'en',
      currencies,
      priority,
      display = 'name',
      showSymbol = true,
      messages: messagesProp,
      emptyMessage,
      onInputValueChange,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const [internalValue, setInternalValue] = useState<string | null>(defaultValue);
    const raw = valueProp !== undefined ? valueProp : internalValue;
    const value = raw ? raw.toUpperCase() : null;
    const [text, setText] = useState('');

    const toOption = useMemo(() => {
      const english = locale.toLowerCase().startsWith('en');
      return (code: string, group?: string): ComboboxOption => {
        const name = getCurrencyName(code, locale);
        const symbol = getCurrencySymbol(code, locale);
        const label =
          display === 'code' ? code : display === 'code-name' ? `${code} · ${name}` : name;
        return {
          value: code,
          label,
          ...(display === 'name' ? {} : { description: name }),
          hint: showSymbol && symbol !== code ? `${code} ${symbol}` : code,
          keywords: `${symbol} ${english ? '' : getCurrencyName(code, 'en')}`,
          ...(group ? { group } : {}),
        };
      };
    }, [locale, display, showSymbol]);

    const all = useMemo(
      () => listCurrencies({ locale, codes: currencies }).map((c) => c.code),
      [locale, currencies],
    );

    const selectedOption = value ? toOption(value) : null;
    const searching = text.trim() !== '' && text !== (selectedOption?.label ?? '');

    const options = useMemo(() => {
      const extra = value && !all.includes(value) ? [value] : [];
      const top = searching
        ? []
        : [...new Set((priority ?? []).map((c) => c.toUpperCase()))].filter((c) => all.includes(c));
      if (top.length === 0) return [...extra, ...all].map((c) => toOption(c));
      return [
        ...top.map((c) => toOption(c, messages.suggested)),
        ...[...extra, ...all].map((c) => toOption(c, messages.all)),
      ];
    }, [all, value, searching, priority, toOption, messages.suggested, messages.all]);

    return (
      <Combobox
        ref={ref}
        sortByMatch
        {...rest}
        className={cx('mn-currency-select', className)}
        options={options}
        value={value}
        valueOptions={selectedOption ? [selectedOption] : undefined}
        filter={matches}
        emptyMessage={emptyMessage ?? messages.empty}
        messages={messages}
        onValueChange={(next) => {
          if (valueProp === undefined) setInternalValue(next);
          onValueChange?.(next);
        }}
        onInputValueChange={(t) => {
          setText(t);
          onInputValueChange?.(t);
        }}
      />
    );
  },
);
