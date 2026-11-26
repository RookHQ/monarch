'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type InputHTMLAttributes,
  type KeyboardEvent,
  type PointerEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { stripBidiMarks } from '../../../internal/digits.js';
import { DashGlyph, PlusGlyph } from '../../../internal/icons.js';
import { useFieldControl } from '../form-field/index.js';

/* ------------------------------------------------------------------------ */
/* Decimal-safe arithmetic                                                  */
/* ------------------------------------------------------------------------ */

/** Number of digits after the decimal point in `n` (`0.1` → 1, `1e-7` → 7). */
export function decimalPlaces(n: number): number {
  if (!Number.isFinite(n)) return 0;
  const [mantissa = '', exponent] = String(n).toLowerCase().split('e');
  const fraction = mantissa.split('.')[1]?.length ?? 0;
  return Math.max(0, fraction - Number(exponent ?? 0));
}

/**
 * Rounds `n` to `places` decimals without binary floating-point drift:
 * `roundTo(0.1 + 0.2, 2)` is `0.3`, `roundTo(1.005, 2)` is `1.01`.
 */
export function roundTo(n: number, places: number): number {
  if (!Number.isFinite(n)) return n;
  const p = Math.max(0, Math.min(20, Math.round(places)));
  const shifted = Math.round(Number(`${n}e${p}`));
  const out = Number(`${shifted}e-${p}`);
  return Object.is(out, -0) ? 0 : out;
}

/**
 * Moves `value` by `delta` steps on the grid `base + k × step`, with no float
 * drift: from 0.3 with step 0.1, one step up is exactly 0.4. A value between
 * grid points moves to the next grid point in that direction (5 → 10 with
 * step 10 going up, → 0 going down).
 */
export function stepValue(value: number, step: number, delta: number, base = 0): number {
  const places = Math.max(decimalPlaces(step), decimalPlaces(base), decimalPlaces(value));
  const scale = 10 ** places;
  const v = Math.round(value * scale);
  const s = Math.round(step * scale);
  const b = Math.round(base * scale);
  if (s <= 0) return value;
  const offset = v - b;
  const onGrid = offset % s === 0;
  let k: number;
  if (onGrid) k = offset / s + delta;
  else if (delta > 0) k = Math.floor(offset / s) + delta;
  else k = Math.ceil(offset / s) + delta;
  return roundTo((b + k * s) / scale, places);
}

/* ------------------------------------------------------------------------ */
/* Locale-aware parsing                                                     */
/* ------------------------------------------------------------------------ */

interface LocaleSymbols {
  group: string;
  decimal: string;
  minus: string;
  digits: Map<string, string>;
}

const symbolCache = new Map<string, LocaleSymbols>();

function localeSymbols(locale: string | undefined): LocaleSymbols {
  const key = locale ?? '';
  const cached = symbolCache.get(key);
  if (cached) return cached;
  const nf = new Intl.NumberFormat(locale);
  const parts = nf.formatToParts(-12345.6);
  const digits = new Map<string, string>();
  const plain = new Intl.NumberFormat(locale, { useGrouping: false });
  for (let d = 0; d <= 9; d++) digits.set(plain.format(d), String(d));
  const symbols: LocaleSymbols = {
    group: parts.find((p) => p.type === 'group')?.value ?? ',',
    decimal: parts.find((p) => p.type === 'decimal')?.value ?? '.',
    minus: parts.find((p) => p.type === 'minusSign')?.value ?? '-',
    digits,
  };
  symbolCache.set(key, symbols);
  return symbols;
}

const SPACES = /[\s\u00a0\u202f\u2009]/g;
const IS_SPACE = /^[\s\u00a0\u202f\u2009]$/;
/** Text allowed around the number: currency symbols and codes, %, signs, brackets, spaces. */
const AFFIX = /^[\p{Sc}\p{L}%\u066a\uff05\u2030\u0609+\-\u2212()\s]*$/u;

/**
 * Reads a number typed in `locale`'s conventions: `1,234.5` in `en`,
 * `1.234,5` in `de`, `1 234,5` in `fr`, native digits such as `١٢٣` in `ar-EG`.
 * Currency symbols or codes and percent signs around the number are ignored;
 * with `percent: true` the result is divided by 100 ("12%" → 0.12).
 * Returns `null` for empty text and `NaN` for text it cannot read.
 */
export function parseNumber(
  text: string,
  locale?: string,
  options: { percent?: boolean } = {},
): number | null {
  const { group, decimal, minus, digits } = localeSymbols(locale);
  // RTL locales wrap signs and symbols in invisible direction marks
  // ("\u200e-12" in Hebrew, "\u061c-\u0661\u0662" in Arabic); they are not part of the number.
  let s = stripBidiMarks(text).trim();
  if (s === '') return null;
  s = Array.from(s, (ch) => digits.get(ch) ?? ch).join('');
  // Accounting negatives: (1,234.50)
  let negative = /^\(.*\)$/.test(s);
  s = IS_SPACE.test(group) ? s.replace(SPACES, '') : s.split(group).join('').replace(SPACES, '');
  s = s.split(decimal).join('.');
  const match = /^([^0-9.]*)(\d*\.?\d*)([^0-9.]*)$/.exec(s);
  if (!match || !/\d/.test(match[2] ?? '')) return Number.NaN;
  const affix = `${match[1] ?? ''}${match[3] ?? ''}`;
  if (!AFFIX.test(affix)) return Number.NaN;
  if (affix.includes(minus) || /[-\u2212]/.test(affix)) negative = true;
  const n = Number(match[2]) * (negative ? -1 : 1);
  if (!Number.isFinite(n)) return Number.NaN;
  if (options.percent) return roundTo(n / 100, decimalPlaces(n) + 2);
  return Object.is(n, -0) ? 0 : n;
}

/* ------------------------------------------------------------------------ */
/* Component                                                                */
/* ------------------------------------------------------------------------ */

/** Interface text, for translation. */
export interface NumberInputMessages {
  /** Accessible name of the + button. Default "Increase". */
  increment: string;
  /** Accessible name of the − button. Default "Decrease". */
  decrement: string;
  /** Shown when the text is not a number. */
  invalid: string;
  /** Shown when the value is outside `min`/`max`; receives them already formatted. */
  range: (min: string | undefined, max: string | undefined) => string;
}

export const NUMBER_INPUT_MESSAGES: NumberInputMessages = {
  increment: 'Increase',
  decrement: 'Decrease',
  invalid: 'Enter a number.',
  range: (min, max) =>
    min !== undefined && max !== undefined
      ? `Enter a number from ${min} to ${max}.`
      : min !== undefined
        ? `Enter ${min} or more.`
        : `Enter ${max ?? ''} or less.`,
};

export interface NumberInputProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
  'value' | 'defaultValue' | 'onChange' | 'size' | 'type' | 'min' | 'max' | 'step'
> {
  /** Controlled value. `null` is empty. */
  value?: number | null | undefined;
  /** Uncontrolled initial value. Default `null` (empty). */
  defaultValue?: number | null | undefined;
  /**
   * Called with the new number (or `null` when cleared) on blur, Enter, an
   * arrow-key step or a stepper button; never on every keystroke.
   */
  onValueChange?: ((value: number | null) => void) | undefined;
  /** Smallest allowed value. Typed values below it are flagged; steps stop at it. */
  min?: number | undefined;
  /** Largest allowed value. Typed values above it are flagged; steps stop at it. */
  max?: number | undefined;
  /**
   * Amount added by ArrowUp and the + button, and removed by ArrowDown and
   * the − button. Default 1 (0.01, shown as 1%, with `style: 'percent'`).
   */
  step?: number | undefined;
  /**
   * Amount for PageUp / PageDown and Shift+Arrow. Default `step × 10`. Like
   * `step`, it moves to the next point on its own grid (from 0.2, PageUp
   * with a large step of 1 goes to 1).
   */
  largeStep?: number | undefined;
  /** BCP 47 locale for parsing and formatting, e.g. `de-DE`. Default: the runtime locale. */
  locale?: string | undefined;
  /**
   * `Intl.NumberFormat` options for display, e.g. `{ style: 'percent' }` or
   * `{ minimumFractionDigits: 2 }`. The committed value is rounded to the
   * formatter's maximum fraction digits, so the value is always what is shown.
   */
  formatOptions?: Intl.NumberFormatOptions | undefined;
  /** Custom display format. Overrides `formatOptions` for display. */
  format?: ((value: number) => string) | undefined;
  /** Custom parser: a number, `null` for empty, or `NaN` when unreadable. Default `parseNumber`. */
  parse?: ((text: string) => number | null) | undefined;
  /** Show the − and + buttons. Default `true`. */
  steppers?: boolean | undefined;
  /** Marks the value invalid. Inside a Field, the Field's `invalid` is used. */
  invalid?: boolean | undefined;
  /** Control height from `--mn-control-*`. Default `md`. */
  size?: 'sm' | 'md' | 'lg' | undefined;
  /** Interface text, for translation. */
  messages?: Partial<NumberInputMessages> | undefined;
}

/**
 * A number field that reads and writes numbers in the user's locale
 * ("1.234,5" in German), steps with the arrow keys, PageUp/PageDown and
 * optional − / + buttons, and never drifts (0.1 + 0.2 is 0.3). It is a
 * `spinbutton`: the spoken value is the formatted number. Unreadable or
 * out-of-range text is kept, flagged and explained, never saved.
 */
export const NumberInput = forwardRef<HTMLInputElement, NumberInputProps>(function NumberInput(
  {
    value: valueProp,
    defaultValue = null,
    onValueChange,
    min,
    max,
    step: stepProp,
    largeStep,
    locale,
    formatOptions,
    format,
    parse,
    steppers = true,
    invalid,
    size = 'md',
    messages: messagesProp,
    name,
    className,
    style,
    onBlur,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const autoId = useId();
  const messageId = `mn-number-input-${autoId}-message`;
  const messages = { ...NUMBER_INPUT_MESSAGES, ...messagesProp };
  const inputRef = useRef<HTMLInputElement | null>(null);

  const percent = formatOptions?.style === 'percent';
  const step = stepProp ?? (percent ? 0.01 : 1);
  const formatter = useMemo(
    () =>
      new Intl.NumberFormat(locale, {
        maximumFractionDigits: Math.max(percent ? 0 : 3, decimalPlaces(step) - (percent ? 2 : 0)),
        ...formatOptions,
      }),
    // formatOptions is usually an inline object; compare by content.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [locale, step, percent, JSON.stringify(formatOptions)],
  );
  const maxDigits = (formatter.resolvedOptions().maximumFractionDigits ?? 3) + (percent ? 2 : 0);
  const show = (n: number) => (format ? format(n) : formatter.format(n));
  const read = (t: string) => (parse ? parse(t) : parseNumber(t, locale, { percent }));

  const [internal, setInternal] = useState<number | null>(defaultValue);
  const value = valueProp !== undefined ? valueProp : internal;
  const shown = value === null ? '' : show(value);
  const [draft, setDraft] = useState(shown);
  const [error, setError] = useState<string | null>(null);

  // Show the value again whenever it (or its format) changes from outside.
  useEffect(() => {
    setDraft(shown);
    setError(null);
  }, [shown]);

  const set = (next: number | null) => {
    const rounded = next === null ? null : roundTo(next, maxDigits);
    if (valueProp === undefined) setInternal(rounded);
    setDraft(rounded === null ? '' : show(rounded));
    setError(null);
    if (rounded !== value) onValueChange?.(rounded);
  };

  const outOfRange = (n: number) =>
    (min !== undefined && n < min) || (max !== undefined && n > max);
  const clamp = (n: number) =>
    Math.min(max ?? Number.POSITIVE_INFINITY, Math.max(min ?? Number.NEGATIVE_INFINITY, n));
  const rangeMessage = () =>
    messages.range(
      min === undefined ? undefined : show(min),
      max === undefined ? undefined : show(max),
    );

  const commit = () => {
    const parsed = read(draft);
    if (parsed === null) return set(null);
    if (Number.isNaN(parsed)) return setError(messages.invalid);
    const rounded = roundTo(parsed, maxDigits);
    if (outOfRange(rounded)) return setError(rangeMessage());
    set(rounded);
  };

  const locked = !!(rest.readOnly || rest.disabled);
  const stepBy = (amount: number, direction: 1 | -1) => {
    if (locked) return;
    const parsed = read(draft);
    const current = parsed === null || Number.isNaN(parsed) ? value : parsed;
    if (current === null) {
      // Empty: start from min (or 0, or max when 0 is above it).
      set(clamp(min ?? 0));
      return;
    }
    set(clamp(stepValue(current, amount, direction, min ?? 0)));
  };

  const big = largeStep ?? stepValue(0, step, 10);

  const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
    onKeyDown?.(e);
    if (e.defaultPrevented || locked) return;
    switch (e.key) {
      case 'ArrowUp':
        e.preventDefault();
        stepBy(e.shiftKey ? big : step, 1);
        break;
      case 'ArrowDown':
        e.preventDefault();
        stepBy(e.shiftKey ? big : step, -1);
        break;
      case 'PageUp':
        e.preventDefault();
        stepBy(big, 1);
        break;
      case 'PageDown':
        e.preventDefault();
        stepBy(big, -1);
        break;
      case 'Home':
        if (min === undefined || e.ctrlKey || e.metaKey) break;
        e.preventDefault();
        set(min);
        break;
      case 'End':
        if (max === undefined || e.ctrlKey || e.metaKey) break;
        e.preventDefault();
        set(max);
        break;
      case 'Enter':
        commit();
        break;
      case 'Escape':
        if (error || draft !== shown) {
          e.preventDefault();
          setDraft(shown);
          setError(null);
        }
        break;
      default:
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
  const atMin = value !== null && min !== undefined && value <= min;
  const atMax = value !== null && max !== undefined && value >= max;

  // Keep focus in the input when a stepper is pressed with a pointer.
  const keepFocus = (e: PointerEvent<HTMLButtonElement>) => {
    e.preventDefault();
    inputRef.current?.focus();
  };

  return (
    <div
      className={cx('mn-number-input', className)}
      style={style}
      data-size={size}
      data-disabled={disabled || undefined}
      data-readonly={rest.readOnly || undefined}
      data-invalid={fieldProps['aria-invalid'] ? '' : undefined}
      data-steppers={steppers || undefined}
    >
      <div className="mn-number-input__control">
        {steppers ? (
          <button
            type="button"
            className="mn-number-input__stepper"
            data-direction="decrement"
            aria-label={messages.decrement}
            tabIndex={-1}
            disabled={disabled || !!rest.readOnly || atMin}
            onPointerDown={keepFocus}
            onClick={() => stepBy(step, -1)}
          >
            <DashGlyph />
          </button>
        ) : null}
        <input
          ref={(el) => {
            inputRef.current = el;
            if (typeof ref === 'function') ref(el);
            else if (ref) ref.current = el;
          }}
          {...fieldProps}
          type="text"
          inputMode={maxDigits === 0 && (min ?? 0) >= 0 ? 'numeric' : 'decimal'}
          role="spinbutton"
          autoComplete={rest.autoComplete ?? 'off'}
          aria-valuenow={value ?? undefined}
          aria-valuetext={value === null ? undefined : show(value)}
          aria-valuemin={min}
          aria-valuemax={max}
          className="mn-number-input__input"
          value={draft}
          onChange={(e) => {
            setDraft(e.target.value);
            if (error) setError(null);
          }}
          onBlur={(e) => {
            commit();
            onBlur?.(e);
          }}
          onKeyDown={handleKey}
        />
        {steppers ? (
          <button
            type="button"
            className="mn-number-input__stepper"
            data-direction="increment"
            aria-label={messages.increment}
            tabIndex={-1}
            disabled={disabled || !!rest.readOnly || atMax}
            onPointerDown={keepFocus}
            onClick={() => stepBy(step, 1)}
          >
            <PlusGlyph />
          </button>
        ) : null}
      </div>
      {name ? (
        <input
          type="hidden"
          name={name}
          value={value === null ? '' : String(value)}
          form={rest.form}
          disabled={disabled}
        />
      ) : null}
      {error ? (
        <p id={messageId} className="mn-number-input__message" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
});
