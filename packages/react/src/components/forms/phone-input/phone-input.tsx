'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useState,
  type InputHTMLAttributes,
  type KeyboardEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { toAsciiDigits } from '../../../internal/digits.js';
import {
  PhoneCountrySelect,
  getPhoneCountry,
  matchPhoneCountry,
} from '../../geo/phone-country-select/index.js';
import { useFieldControl } from '../form-field/index.js';

/** Countries whose national numbers keep their leading 0 after the calling code. */
const KEEPS_LEADING_ZERO = new Set(['IT', 'SM', 'VA']);

/** Digits only, from any typed form ("(020) 7946-0958" → "02079460958"). */
function digitsOf(text: string): string {
  return toAsciiDigits(text).replace(/\D/g, '');
}

/**
 * Joins a calling code and a national number into E.164: `toE164('+44',
 * '020 7946 0958')` is `+442079460958`. Separators are removed and one
 * leading trunk 0 is dropped (except for Italy, San Marino and Vatican City,
 * pass `country` to apply that). Returns `null` when there are no digits.
 */
export function toE164(dialCode: string, national: string, country?: string): string | null {
  let digits = digitsOf(national);
  if (digits === '') return null;
  if (digits.startsWith('0') && !KEEPS_LEADING_ZERO.has((country ?? '').toUpperCase()))
    digits = digits.slice(1);
  return `+${digitsOf(dialCode)}${digits}`;
}

/** True for a plausible E.164 number: "+", then 8 to 15 digits, not starting with 0. */
export function isValidE164(value: string): boolean {
  return /^\+[1-9]\d{7,14}$/.test(value);
}

/**
 * Splits an international number ("+44 20 7946 0958", "0044…", or E.164)
 * into its country and national digits, using `matchPhoneCountry`. Returns
 * `null` when no calling code matches. `preferred` picks among countries
 * that share a code (+1, +7, +44).
 */
export function parsePhoneNumber(
  phone: string,
  preferred?: string,
): { country: string; dialCode: string; national: string } | null {
  const match = matchPhoneCountry(phone, preferred);
  if (!match) return null;
  const compact = phone.replace(/[\s().-]/g, '');
  const digits = compact.startsWith('+') ? compact.slice(1) : compact.slice(2);
  return {
    country: match.code,
    dialCode: match.dialCode,
    national: digits.slice(digitsOf(match.dialCode).length),
  };
}

/** Interface text, for translation. */
export interface PhoneInputMessages {
  /** Accessible name of the country picker. Default "Country code". */
  country: string;
  /** Shown when the number is too short or too long. */
  invalid: string;
}

export const PHONE_INPUT_MESSAGES: PhoneInputMessages = {
  country: 'Country code',
  invalid: 'Enter a complete phone number.',
};

/** Extra detail passed with each change. */
export interface PhoneValueDetails {
  /** ISO 3166-1 alpha-2 code of the chosen country. */
  country: string;
  /** Calling code, such as "+44". */
  dialCode: string;
  /** National digits as stored in the E.164 value. */
  national: string;
}

export interface PhoneInputProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
  'value' | 'defaultValue' | 'onChange' | 'size' | 'type'
> {
  /** Controlled number in E.164 (`+442079460958`), or `null` when empty. */
  value?: string | null | undefined;
  /** Uncontrolled initial number in E.164 (or any international form). */
  defaultValue?: string | null | undefined;
  /**
   * Called with the E.164 number (or `null` when cleared) on blur, Enter or
   * a country change; never on every keystroke. Invalid numbers are not sent.
   */
  onValueChange?: ((value: string | null, details: PhoneValueDetails | null) => void) | undefined;
  /** Controlled ISO country code, such as "GB". */
  country?: string | undefined;
  /** Initial country when the value does not say. Default "US". */
  defaultCountry?: string | undefined;
  /** Called when the country changes, by the picker or by pasting an international number. */
  onCountryChange?: ((country: string) => void) | undefined;
  /** Limit the picker to these ISO codes. Default: every country. */
  countries?: readonly string[] | undefined;
  /** ISO codes listed first in the picker, such as the visitor's own country. */
  preferred?: readonly string[] | undefined;
  /** Language for country names. Default: the runtime's language. */
  locale?: string | undefined;
  /** Marks the value invalid. Inside a Field, the Field's `invalid` is used. */
  invalid?: boolean | undefined;
  /** Control height from `--mn-control-*`. Default `md`. */
  size?: 'sm' | 'md' | 'lg' | undefined;
  /** Interface text, for translation. */
  messages?: Partial<PhoneInputMessages> | undefined;
}

function split(value: string | null | undefined, country: string) {
  if (!value) return { country, national: '' };
  const parsed = parsePhoneNumber(value, country);
  return parsed
    ? { country: parsed.country, national: parsed.national }
    : { country, national: digitsOf(value) };
}

/**
 * A phone number as a country calling code plus the national number, with
 * one E.164 value out (`+442079460958`). The country is a searchable
 * PhoneCountrySelect; pasting or typing a full international number
 * ("+44 20 7946 0958") picks the country and splits the digits. Incomplete
 * numbers are kept, flagged and explained, never sent.
 */
export const PhoneInput = forwardRef<HTMLInputElement, PhoneInputProps>(function PhoneInput(
  {
    value: valueProp,
    defaultValue = null,
    onValueChange,
    country: countryProp,
    defaultCountry = 'US',
    onCountryChange,
    countries,
    preferred,
    locale,
    invalid,
    size = 'md',
    messages: messagesProp,
    name,
    className,
    style,
    onBlur,
    onKeyDown,
    placeholder,
    ...rest
  },
  ref,
) {
  const messages = { ...PHONE_INPUT_MESSAGES, ...messagesProp };
  const autoId = useId();
  const countryId = `mn-phone-input-${autoId}-country`;
  const messageId = `mn-phone-input-${autoId}-message`;

  const [internal, setInternal] = useState<string | null>(() => {
    if (!defaultValue) return null;
    const s = split(defaultValue, defaultCountry);
    return toE164(getPhoneCountry(s.country)?.dialCode ?? '', s.national, s.country);
  });
  const value = valueProp !== undefined ? valueProp : internal;
  const [countryState, setCountryState] = useState(
    () => split(value, (countryProp ?? defaultCountry).toUpperCase()).country,
  );
  const country = (countryProp ?? countryState).toUpperCase();
  const [draft, setDraft] = useState(() => split(value, country).national);
  const [error, setError] = useState<string | null>(null);

  // Show the value again whenever it changes from outside.
  useEffect(() => {
    const s = split(value, country);
    if (s.country !== country) {
      if (countryProp === undefined) setCountryState(s.country);
      onCountryChange?.(s.country);
    }
    setDraft(s.national);
    setError(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  const changeCountry = (next: string) => {
    if (next === country) return;
    if (countryProp === undefined) setCountryState(next);
    onCountryChange?.(next);
  };

  const emit = (next: string | null, details: PhoneValueDetails | null) => {
    if (valueProp === undefined) setInternal(next);
    if (next !== value) onValueChange?.(next, details);
  };

  /** Reads `text` for `forCountry`, emitting a valid number or flagging the text. */
  const commit = (text: string, forCountry: string) => {
    let code = forCountry;
    let national = text;
    const compact = text.trim();
    if (compact.startsWith('+') || compact.startsWith('00')) {
      const parsed = parsePhoneNumber(compact, code);
      if (!parsed) return setError(messages.invalid);
      code = parsed.country;
      national = parsed.national;
      changeCountry(code);
      setDraft(national);
    }
    const dialCode = getPhoneCountry(code)?.dialCode ?? '';
    const e164 = toE164(dialCode, national, code);
    if (e164 === null) {
      setError(null);
      return emit(null, null);
    }
    if (!isValidE164(e164)) return setError(messages.invalid);
    // A shared calling code (+1 242…) may belong to another country.
    const owner = matchPhoneCountry(e164, code);
    if (owner && owner.code !== code && owner.dialCode === dialCode) changeCountry(owner.code);
    setError(null);
    emit(e164, {
      country: owner?.dialCode === dialCode ? owner.code : code,
      dialCode,
      national: e164.slice(dialCode.length),
    });
  };

  const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
    onKeyDown?.(e);
    if (e.defaultPrevented) return;
    if (e.key === 'Enter') commit(draft, country);
    else if (e.key === 'Escape' && (error || draft !== split(value, country).national)) {
      e.preventDefault();
      setDraft(split(value, country).national);
      setError(null);
    }
  };

  const describedBy = [rest['aria-describedby'], error ? messageId : undefined]
    .filter(Boolean)
    .join(' ');
  const fieldProps = useFieldControl(
    { ...rest, 'aria-describedby': describedBy || undefined },
    { invalid: error ? true : invalid },
  );
  const disabled = !!fieldProps.disabled;
  const readOnly = !!rest.readOnly;

  return (
    <div
      className={cx('mn-phone-input', className)}
      style={style}
      data-size={size}
      data-disabled={disabled || undefined}
      data-invalid={fieldProps['aria-invalid'] ? '' : undefined}
    >
      <div className="mn-phone-input__row">
        <PhoneCountrySelect
          id={countryId}
          aria-label={messages.country}
          className="mn-phone-input__country"
          value={country}
          onValueChange={(code) => {
            if (!code) return;
            changeCountry(code);
            if (draft.trim() !== '') commit(draft, code);
          }}
          countries={countries}
          preferred={preferred}
          locale={locale}
          size={size}
          // A read-only number keeps its country too: the picker has no read-only state.
          disabled={disabled || readOnly}
          invalid={!!fieldProps['aria-invalid']}
        />
        <input
          ref={ref}
          {...fieldProps}
          type="tel"
          inputMode="tel"
          autoComplete={rest.autoComplete ?? 'tel-national'}
          dir="ltr"
          className="mn-input mn-phone-input__number"
          data-size={size}
          placeholder={placeholder}
          value={draft}
          onChange={(e) => {
            const text = toAsciiDigits(e.target.value);
            // A pasted full international number picks its country at once.
            const compact = text.replace(/[\s().-]/g, '');
            if (/^(\+|00)\d{7,}$/.test(compact)) {
              const parsed = parsePhoneNumber(compact, country);
              if (parsed) {
                changeCountry(parsed.country);
                setDraft(parsed.national);
                setError(null);
                return;
              }
            }
            setDraft(text);
            if (error) setError(null);
          }}
          onBlur={(e) => {
            commit(draft, country);
            onBlur?.(e);
          }}
          onKeyDown={handleKey}
        />
      </div>
      {name ? (
        <input type="hidden" name={name} value={value ?? ''} form={rest.form} disabled={disabled} />
      ) : null}
      {error ? (
        <p id={messageId} className="mn-phone-input__message" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
});
