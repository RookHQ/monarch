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

/** The 249 officially assigned ISO 3166-1 alpha-2 country and territory codes. */
export const COUNTRY_CODES: readonly string[] = (
  'AD AE AF AG AI AL AM AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BJ BL BM BN BO BQ BR ' +
  'BS BT BV BW BY BZ CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CW CX CY CZ DE DJ DK DM DO DZ ' +
  'EC EE EG EH ER ES ET FI FJ FK FM FO FR GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW ' +
  'GY HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP KE KG KH KI KM KN KP KR KW KY ' +
  'KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU MV ' +
  'MW MX MY MZ NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY ' +
  'QA RE RO RS RU RW SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR SS ST SV SX SY SZ TC TD TF TG ' +
  'TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG UM US UY UZ VA VC VE VG VI VN VU WF WS YE YT ZA ZM ZW'
).split(' ');

const namesCache = new Map<string, Intl.DisplayNames | null>();
function regionNames(locale: string): Intl.DisplayNames | null {
  if (!namesCache.has(locale)) {
    try {
      namesCache.set(locale, new Intl.DisplayNames([locale], { type: 'region', fallback: 'none' }));
    } catch {
      // No Intl.DisplayNames (old runtimes) or an invalid locale: names fall back to codes.
      namesCache.set(locale, null);
    }
  }
  return namesCache.get(locale) ?? null;
}

/**
 * The country's name in `locale` from `Intl.DisplayNames`, such as
 * `getCountryName('DE', 'fr')` → "Allemagne". Returns the upper-case code when
 * the runtime has no name for it or no `Intl.DisplayNames`.
 */
export function getCountryName(code: string, locale = 'en'): string {
  const upper = code.toUpperCase();
  try {
    return regionNames(locale)?.of(upper) ?? upper;
  } catch {
    return upper;
  }
}

/**
 * The flag emoji for an ISO 3166-1 alpha-2 code, built from regional indicator
 * symbols (`'PT'` → 🇵🇹). Returns an empty string for anything that is not two
 * letters. Some platforms (Windows) draw the two letters instead of a flag.
 */
export function getCountryFlag(code: string): string {
  if (!/^[a-z]{2}$/i.test(code)) return '';
  return String.fromCodePoint(
    ...[...code.toUpperCase()].map((c) => 0x1f1e6 + c.charCodeAt(0) - 65),
  );
}

/** One country, as listed by `listCountries`. */
export interface CountryInfo {
  /** ISO 3166-1 alpha-2 code, upper case. */
  code: string;
  /** Name in the requested locale. */
  name: string;
  /** Flag emoji. */
  flag: string;
}

/**
 * Countries with names in `locale`, sorted by name in that locale.
 * Default: every code in `COUNTRY_CODES`.
 */
export function listCountries(
  options: { locale?: string | undefined; codes?: readonly string[] | undefined } = {},
): CountryInfo[] {
  const locale = options.locale ?? 'en';
  // Never throws on an unknown locale; names then fall back to codes and the default order.
  const collator = safeCollator(locale);
  const codes = [...new Set((options.codes ?? COUNTRY_CODES).map((c) => c.toUpperCase()))];
  return codes
    .map((code) => ({ code, name: getCountryName(code, locale), flag: getCountryFlag(code) }))
    .sort((a, b) => collator.compare(a.name, b.name));
}

/** Interface text, for translation. Every message has an English default. */
export interface CountrySelectMessages {
  /** Heading over `priority` countries. Default "Suggested". */
  suggested: string;
  /** Heading over the rest when `priority` is set. Default "All countries". */
  all: string;
  /** Shown when nothing matches. Default "No countries match". */
  empty: string;
}

const DEFAULT_MESSAGES: CountrySelectMessages = {
  suggested: 'Suggested',
  all: 'All countries',
  empty: 'No countries match',
};

export interface CountrySelectProps extends Omit<
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
  /** Selected ISO 3166-1 alpha-2 code (controlled), such as `'PT'`. Any case is accepted; `onValueChange` reports upper case. */
  value?: string | null | undefined;
  /** Initially selected code (uncontrolled). */
  defaultValue?: string | null | undefined;
  /** Called with the chosen upper-case code, or `null` when cleared. */
  onValueChange?: ((code: string | null) => void) | undefined;
  /** BCP 47 locale for country names and sorting. Default `en`, so server and client render the same. */
  locale?: string | undefined;
  /** Limit the list to these codes, such as the countries the product ships to. Default: all 249. */
  countries?: readonly string[] | undefined;
  /** Codes to leave out, such as sanctioned regions. */
  exclude?: readonly string[] | undefined;
  /** Codes listed first under "Suggested" while nothing is typed, such as the detected or most used countries. */
  priority?: readonly string[] | undefined;
  /** Show each country's flag emoji before its name. Default true. */
  showFlag?: boolean | undefined;
  /** Show the ISO code at the end of each option. Default true. */
  showCode?: boolean | undefined;
  /** Override a country's name, for example to follow a product's legal wording. Return `undefined` to keep the `Intl` name. */
  countryName?: ((code: string) => string | undefined) | undefined;
  /** Interface text, for translation. Combobox messages can be passed here too. */
  messages?: (Partial<CountrySelectMessages> & Partial<ComboboxMessages>) | undefined;
}

/** Every word typed must appear in the name, code or English name. */
function matches(option: ComboboxOption, query: string): boolean {
  const haystack = `${option.label} ${option.value} ${option.keywords ?? ''}`.toLowerCase();
  return query
    .toLowerCase()
    .split(/\s+/)
    .filter(Boolean)
    .every((term) => haystack.includes(term));
}

/**
 * Searchable country picker. The value is an ISO 3166-1 alpha-2 code; names
 * come from `Intl.DisplayNames` in `locale` and are sorted for that locale,
 * with an optional flag emoji and a "Suggested" section. Search matches the
 * name, the code and the English name. Ships no country data beyond the code
 * list. Built on Combobox; forwards its ref to the input.
 */
export const CountrySelect = forwardRef<HTMLInputElement, CountrySelectProps>(
  function CountrySelect(
    {
      value: valueProp,
      defaultValue = null,
      onValueChange,
      locale = 'en',
      countries,
      exclude,
      priority,
      showFlag = true,
      showCode = true,
      countryName,
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
        const name = countryName?.(code) ?? getCountryName(code, locale);
        const flag = showFlag ? getCountryFlag(code) : '';
        return {
          value: code,
          label: name,
          ...(flag ? { media: <span className="mn-country-select__flag">{flag}</span> } : {}),
          ...(showCode ? { hint: code } : {}),
          ...(english ? {} : { keywords: getCountryName(code, 'en') }),
          ...(group ? { group } : {}),
        };
      };
    }, [locale, countryName, showFlag, showCode]);

    const all = useMemo(() => {
      const skip = new Set((exclude ?? []).map((c) => c.toUpperCase()));
      return listCountries({ locale, codes: countries })
        .filter((c) => !skip.has(c.code))
        .map((c) => c.code);
    }, [locale, countries, exclude]);

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
        className={cx('mn-country-select', className)}
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
