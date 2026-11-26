'use client';

import { forwardRef, useMemo } from 'react';
import { cx } from '../../../internal/cx.js';
import { safeCollator } from '../../../internal/collation.js';
import { useClientLocale } from '../../../internal/dates/client-time.js';
import {
  Combobox,
  type ComboboxMessages,
  type ComboboxOption,
  type ComboboxProps,
} from '../../forms/combobox/index.js';

/** One country or territory and its international calling code (ITU-T E.164). */
export interface PhoneCountry {
  /** ISO 3166-1 alpha-2 code, upper case, such as "GB". */
  code: string;
  /** Calling code with a leading plus, such as "+44". Several countries can share one. */
  dialCode: string;
  /**
   * Digits that follow the calling code for this country when it shares the
   * code with others, such as "242" for the Bahamas under "+1". Used by
   * `matchPhoneCountry` to tell shared-code countries apart.
   */
  leadingDigits?: readonly string[];
}

// Code, calling code (without "+") and, after "-", dot-separated leading digits.
const DATA =
  'AD376 AE971 AF93 AG1-268 AI1-264 AL355 AM374 AO244 AR54 AS1-684 AT43 AU61 AW297 AX358-18 AZ994 ' +
  'BA387 BB1-246 BD880 BE32 BF226 BG359 BH973 BI257 BJ229 BL590 BM1-441 BN673 BO591 BQ599-3.4.7 ' +
  'BR55 BS1-242 BT975 BW267 BY375 BZ501 CA1 CC61 CD243 CF236 CG242 CH41 CI225 CK682 CL56 CM237 ' +
  'CN86 CO57 CR506 CU53 CV238 CW599 CX61 CY357 CZ420 DE49 DJ253 DK45 DM1-767 DO1-809.829.849 ' +
  'DZ213 EC593 EE372 EG20 EH212 ER291 ES34 ET251 FI358 FJ679 FK500 FM691 FO298 FR33 GA241 GB44 ' +
  'GD1-473 GE995 GF594 GG44-1481 GH233 GI350 GL299 GM220 GN224 GP590 GQ240 GR30 GT502 GU1-671 ' +
  'GW245 GY592 HK852 HN504 HR385 HT509 HU36 ID62 IE353 IL972 IM44-1624 IN91 IO246 IQ964 IR98 ' +
  'IS354 IT39 JE44-1534 JM1-876.658 JO962 JP81 KE254 KG996 KH855 KI686 KM269 KN1-869 KP850 KR82 ' +
  'KW965 KY1-345 KZ7-6.7 LA856 LB961 LC1-758 LI423 LK94 LR231 LS266 LT370 LU352 LV371 LY218 ' +
  'MA212 MC377 MD373 ME382 MF590 MG261 MH692 MK389 ML223 MM95 MN976 MO853 MP1-670 MQ596 MR222 ' +
  'MS1-664 MT356 MU230 MV960 MW265 MX52 MY60 MZ258 NA264 NC687 NE227 NF672 NG234 NI505 NL31 ' +
  'NO47 NP977 NR674 NU683 NZ64 OM968 PA507 PE51 PF689 PG675 PH63 PK92 PL48 PM508 PR1-787.939 ' +
  'PS970 PT351 PW680 PY595 QA974 RE262 RO40 RS381 RU7 RW250 SA966 SB677 SC248 SD249 SE46 SG65 ' +
  'SH290 SI386 SJ47-79 SK421 SL232 SM378 SN221 SO252 SR597 SS211 ST239 SV503 SX1-721 SY963 SZ268 ' +
  'TC1-649 TD235 TG228 TH66 TJ992 TK690 TL670 TM993 TN216 TO676 TR90 TT1-868 TV688 TW886 TZ255 ' +
  'UA380 UG256 US1 UY598 UZ998 VA39-06698 VC1-784 VE58 VG1-284 VI1-340 VN84 VU678 WF681 WS685 ' +
  'XK383 YE967 YT262-269.639 ZA27 ZM260 ZW263';

/**
 * Every country and territory with its calling code, ordered by ISO code.
 * Names are not stored: they come from `Intl.DisplayNames` in the reader's
 * language. Shared with phone inputs so the picker and the parser agree.
 */
export const PHONE_COUNTRIES: readonly PhoneCountry[] = DATA.split(' ').map((entry) => {
  const [head = '', digits] = entry.split('-');
  const country: PhoneCountry = { code: head.slice(0, 2), dialCode: `+${head.slice(2)}` };
  if (digits) country.leadingDigits = digits.split('.');
  return country;
});

const BY_CODE = new Map(PHONE_COUNTRIES.map((c) => [c.code, c]));

/** Countries that own a shared calling code when the number does not say otherwise. */
const MAIN_FOR_CODE: Record<string, string> = {
  '+1': 'US',
  '+7': 'RU',
  '+44': 'GB',
  '+47': 'NO',
  '+61': 'AU',
  '+212': 'MA',
  '+262': 'RE',
  '+358': 'FI',
  '+39': 'IT',
  '+590': 'GP',
  '+599': 'CW',
};

/** Arabic-Indic, Persian and full-width digits, by the code point of their zero. */
const DIGIT_ZEROS = [0x0660, 0x06f0, 0xff10];

/**
 * ASCII digits, "+" and "-" for a number typed on another keyboard
 * ("+٤٤ ٢٠…", "＋４４…"), so the calling code is still recognised.
 */
function toAsciiPhone(text: string): string {
  return (
    text
      .replace(/[\u0660-\u0669\u06f0-\u06f9\uff10-\uff19]/g, (ch) => {
        const code = ch.charCodeAt(0);
        const zero = DIGIT_ZEROS.find((z) => code >= z && code <= z + 9) ?? code;
        return String(code - zero);
      })
      .replace(/\uff0b/g, '+')
      .replace(/[\u2010-\u2015\u2212\uff0d]/g, '-')
      // Bidi marks that phones and chat apps wrap around a number.
      .replace(/[\u200e\u200f\u202a-\u202e\u2066-\u2069]/g, '')
  );
}

/** The calling code entry for an ISO 3166-1 alpha-2 code (any case), or undefined. */
export function getPhoneCountry(code: string): PhoneCountry | undefined {
  return BY_CODE.get(code.toUpperCase());
}

/**
 * The country an international number belongs to, from its leading digits:
 * "+44 20 7946 0000" gives GB, "+1 242 555 0100" gives BS. A calling code
 * shared by several countries resolves to `preferred` when it shares that
 * code, otherwise to the code's main country (US for +1). Returns undefined
 * when the text does not start with "+" or "00" and a known code.
 */
export function matchPhoneCountry(phone: string, preferred?: string): PhoneCountry | undefined {
  const compact = toAsciiPhone(phone).replace(/[\s().-]/g, '');
  const digits = compact.startsWith('+')
    ? compact.slice(1)
    : compact.startsWith('00')
      ? compact.slice(2)
      : null;
  if (!digits || !/^\d+$/.test(digits)) return undefined;
  for (let length = Math.min(4, digits.length); length >= 1; length--) {
    const dialCode = `+${digits.slice(0, length)}`;
    const candidates = PHONE_COUNTRIES.filter((c) => c.dialCode === dialCode);
    if (candidates.length === 0) continue;
    const rest = digits.slice(length);
    const byDigits = candidates.find((c) => c.leadingDigits?.some((d) => rest.startsWith(d)));
    if (byDigits) return byDigits;
    const pref = preferred?.toUpperCase();
    return (
      candidates.find((c) => c.code === pref && !c.leadingDigits) ??
      candidates.find((c) => c.code === MAIN_FOR_CODE[dialCode]) ??
      candidates.find((c) => !c.leadingDigits) ??
      candidates[0]
    );
  }
  return undefined;
}

/** The regional indicator flag for an ISO code. Some platforms show it as two letters. */
function flagFor(code: string): string {
  return String.fromCodePoint(...[...code].map((c) => 0x1f1e6 + c.charCodeAt(0) - 65));
}

function displayNames(locale: string | undefined): Intl.DisplayNames | null {
  try {
    return new Intl.DisplayNames(locale ? [locale] : undefined, { type: 'region' });
  } catch {
    return null;
  }
}

/** Interface text, for translation. Every message has an English default. */
export interface PhoneCountrySelectMessages {
  /** Group heading over `preferred` countries. Default "Suggested". */
  preferredGroup: string;
  /** Group heading over the rest when `preferred` is set. Default "All countries". */
  allGroup: string;
  /** Shown when no country matches the search. Default "No matching country". */
  empty: string;
  /** Accessible name of the list. Default "Country calling codes". */
  listLabel: string;
  /** The option text: the country name and its calling code. Default `${name} (${dialCode})`. */
  option: (name: string, dialCode: string) => string;
}

const DEFAULT_MESSAGES: PhoneCountrySelectMessages = {
  preferredGroup: 'Suggested',
  allGroup: 'All countries',
  empty: 'No matching country',
  listLabel: 'Country calling codes',
  option: (name, dialCode) => `${name} (${dialCode})`,
};

export interface PhoneCountrySelectProps extends Omit<
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
  | 'renderOption'
  | 'emptyMessage'
  | 'listLabel'
  | 'messages'
> {
  /** Selected ISO 3166-1 alpha-2 code, such as "GB" (controlled). */
  value?: string | null | undefined;
  /** Initial ISO code when uncontrolled. */
  defaultValue?: string | null | undefined;
  /** Called with the chosen ISO code and its calling code, or nulls when cleared. */
  onValueChange?: ((code: string | null, dialCode: string | null) => void) | undefined;
  /** Limit the list to these ISO codes. Default: every country in `PHONE_COUNTRIES`. */
  countries?: readonly string[] | undefined;
  /** ISO codes listed first under the "Suggested" heading, such as the visitor's own country. */
  preferred?: readonly string[] | undefined;
  /**
   * Language for country names, such as "fr". Default: the viewer's language
   * after hydration (`en-US` on the server and while hydrating).
   */
  locale?: string | undefined;
  /** Show the flag emoji beside each country. Default true. */
  showFlags?: boolean | undefined;
  /** Allow clearing the choice. Default false: a phone number always needs a country. */
  clearable?: boolean | undefined;
  /** Interface text, for translation. */
  messages?: Partial<PhoneCountrySelectMessages> | undefined;
  /** Text of the underlying combobox (loading, show/close options). */
  comboboxMessages?: Partial<ComboboxMessages> | undefined;
}

/**
 * Picks the country calling code for a phone number: a searchable list of
 * countries named in the reader's language (`Intl.DisplayNames`), each with
 * its flag and code. Typing matches the name, the ISO code or the digits
 * ("44", "+44"). Built on Combobox, so it sits in a Field and follows the
 * WAI-ARIA combobox pattern.
 */
export const PhoneCountrySelect = forwardRef<HTMLInputElement, PhoneCountrySelectProps>(
  function PhoneCountrySelect(
    {
      value,
      defaultValue,
      onValueChange,
      countries,
      preferred,
      locale: localeProp,
      showFlags = true,
      clearable = false,
      messages: messagesProp,
      comboboxMessages,
      sortByMatch = true,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const { preferredGroup, allGroup, option: optionText } = messages;
    // The same names on the server and while hydrating, then the viewer's language.
    const locale = useClientLocale(localeProp);
    // Lists written inline are new arrays on every render: compare them by content,
    // so 240 names are not looked up and sorted again each time the parent renders.
    const countriesKey = countries?.join(',');
    const preferredKey = preferred?.join(',');

    const options = useMemo<ComboboxOption[]>(() => {
      const names = displayNames(locale);
      const collator = safeCollator(locale);
      const list = countries
        ? countries.map((c) => BY_CODE.get(c.toUpperCase())).filter((c) => c !== undefined)
        : [...PHONE_COUNTRIES];
      const toOption = (c: PhoneCountry, group?: string): ComboboxOption => {
        let name = c.code;
        try {
          name = names?.of(c.code) ?? c.code;
        } catch {
          // Unknown to this runtime: keep the code.
        }
        const o: ComboboxOption = {
          value: c.code,
          label: optionText(name, c.dialCode),
          keywords: `${c.code} ${c.dialCode} ${c.dialCode.slice(1)}${
            c.leadingDigits ? ` ${c.leadingDigits.map((d) => c.dialCode + d).join(' ')}` : ''
          }`,
        };
        if (showFlags)
          o.media = <span className="mn-phone-country-select__flag">{flagFor(c.code)}</span>;
        if (group) o.group = group;
        return o;
      };
      const pref = (preferred ?? []).map((p) => p.toUpperCase());
      const top = pref
        .map((p) => list.find((c) => c.code === p))
        .filter((c) => c !== undefined)
        .map((c) => toOption(c, preferredGroup));
      const rest = list
        .map((c) => toOption(c, top.length > 0 ? allGroup : undefined))
        .sort((a, b) => collator.compare(a.label, b.label));
      // Option values must be unique, so a preferred country is listed only under its heading.
      return [...top, ...rest.filter((o) => !pref.includes(o.value))];
      // eslint-disable-next-line react-hooks/exhaustive-deps -- lists compared by content
    }, [countriesKey, preferredKey, locale, showFlags, preferredGroup, allGroup, optionText]);

    const normalise = (v: string | null | undefined) => (v == null ? v : v.toUpperCase());

    return (
      <Combobox
        ref={ref}
        {...rest}
        className={cx('mn-phone-country-select', className)}
        options={options}
        value={normalise(value)}
        defaultValue={normalise(defaultValue)}
        onValueChange={(code) =>
          onValueChange?.(code, code ? (BY_CODE.get(code)?.dialCode ?? null) : null)
        }
        clearable={clearable}
        sortByMatch={sortByMatch}
        emptyMessage={messages.empty}
        listLabel={messages.listLabel}
        messages={comboboxMessages}
        autoComplete="off"
      />
    );
  },
);
