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
import { useFieldControl } from '../form-field/index.js';

export interface DurationFormatOptions {
  /** Largest unit used when formatting: `h` (default, "26h") or `d` ("1d 2h"). */
  largestUnit?: 'h' | 'd';
  /** Minutes in a day, for `d`: 1440 (default) for calendar days, or 480 for 8-hour work days. */
  minutesPerDay?: number;
}

const UNIT_MINUTES: Record<string, 'd' | 'h' | 'm'> = {
  d: 'd',
  day: 'd',
  days: 'd',
  h: 'h',
  hr: 'h',
  hrs: 'h',
  hour: 'h',
  hours: 'h',
  m: 'm',
  min: 'm',
  mins: 'm',
  minute: 'm',
  minutes: 'm',
};

/**
 * Reads a duration as whole minutes. Accepts `90`, `90m`, `1h 30m`, `1h30`,
 * `1.5h`, `1,5h`, `1:30`, `2 hours 15 minutes` and `1d 4h`. Returns `null`
 * for empty text and `NaN` for text it cannot read.
 */
export function parseDuration(text: string, options: DurationFormatOptions = {}): number | null {
  const perDay = options.minutesPerDay ?? 1440;
  const s = text.trim().toLowerCase().replace(/,/g, '.');
  if (s === '') return null;
  if (/^\d+(\.\d+)?$/.test(s)) return Math.round(Number(s));
  const clock = /^(\d+):([0-5]\d)$/.exec(s);
  if (clock) return Number(clock[1]) * 60 + Number(clock[2]);
  const re = /(\d+(?:\.\d+)?)\s*([a-z]+)\s*/gy;
  let total = 0;
  let consumed = 0;
  let lastUnit: 'd' | 'h' | 'm' | null = null;
  let match: RegExpExecArray | null;
  while ((match = re.exec(s))) {
    const unit = UNIT_MINUTES[match[2] ?? ''];
    if (!unit) return Number.NaN;
    const n = Number(match[1]);
    total += unit === 'd' ? n * perDay : unit === 'h' ? n * 60 : n;
    consumed = re.lastIndex;
    lastUnit = unit;
  }
  // "1h30": a bare number after hours means minutes.
  const rest = s.slice(consumed).trim();
  if (rest !== '') {
    if (lastUnit === 'h' && /^\d+$/.test(rest)) total += Number(rest);
    else return Number.NaN;
  }
  return consumed === 0 ? Number.NaN : Math.round(total);
}

/** `0m`, `45m`, `1h 30m`, `26h` (or `1d 2h` with `largestUnit: 'd'`). */
export function formatDuration(minutes: number, options: DurationFormatOptions = {}): string {
  const perDay = options.minutesPerDay ?? 1440;
  const total = Math.max(0, Math.round(minutes));
  if (total === 0) return '0m';
  const d = options.largestUnit === 'd' ? Math.floor(total / perDay) : 0;
  const afterDays = total - d * perDay;
  const h = Math.floor(afterDays / 60);
  const m = afterDays % 60;
  return [d ? `${d}d` : '', h ? `${h}h` : '', m ? `${m}m` : ''].filter(Boolean).join(' ');
}

/** Interface text, for translation. */
export interface DurationInputMessages {
  /** Shown when the text cannot be read. */
  invalid: string;
  /** Shown when the value is outside `min`/`max`. */
  range: (min: number | undefined, max: number | undefined) => string;
  /** Spoken value (`aria-valuetext`). Default "1 hour 30 minutes". */
  valueText: (minutes: number) => string;
}

const plural = (n: number, one: string) => `${n} ${one}${n === 1 ? '' : 's'}`;

const DEFAULT_MESSAGES: DurationInputMessages = {
  invalid: 'Use a duration like 1h 30m, 90m or 1.5h.',
  range: (min, max) =>
    min !== undefined && max !== undefined
      ? `Enter between ${formatDuration(min)} and ${formatDuration(max)}.`
      : min !== undefined
        ? `Enter at least ${formatDuration(min)}.`
        : `Enter at most ${formatDuration(max ?? 0)}.`,
  valueText: (minutes) => {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    if (h === 0) return plural(m, 'minute');
    return m === 0 ? plural(h, 'hour') : `${plural(h, 'hour')} ${plural(m, 'minute')}`;
  },
};

export interface DurationInputProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
  'value' | 'defaultValue' | 'onChange' | 'size' | 'type' | 'min' | 'max' | 'step'
> {
  /** Duration in whole minutes (controlled). `null` is empty. */
  value?: number | null | undefined;
  defaultValue?: number | null | undefined;
  /** Called with minutes (or `null` when cleared) on blur, Enter or an arrow-key step; never while typing. */
  onValueChange?: ((minutes: number | null) => void) | undefined;
  /** Smallest allowed value in minutes. Default 0. */
  min?: number | undefined;
  /** Largest allowed value in minutes. */
  max?: number | undefined;
  /** Minutes added or removed by ArrowUp / ArrowDown. Default 15. PageUp / PageDown step by 60. */
  step?: number | undefined;
  /** Largest unit when formatting: `h` (default) or `d`. */
  largestUnit?: 'h' | 'd' | undefined;
  /** Minutes in a "d" when parsing and formatting. Default 1440. */
  minutesPerDay?: number | undefined;
  /** Custom display format. Default `formatDuration`. */
  format?: ((minutes: number) => string) | undefined;
  /** Custom parser: minutes, `null` for empty, or `NaN` when unreadable. Default `parseDuration`. */
  parse?: ((text: string) => number | null) | undefined;
  invalid?: boolean | undefined;
  size?: 'sm' | 'md' | 'lg' | undefined;
  /** Interface text, for translation. */
  messages?: Partial<DurationInputMessages> | undefined;
}

/**
 * Free-text duration field with a minutes value. Reads "1h 30m", "90m",
 * "1.5h" or "1:30", shows the value back as "1h 30m", and steps with the
 * arrow keys. Unreadable or out-of-range text is kept, flagged and explained,
 * never saved. The input is a `spinbutton` with a spoken value.
 */
export const DurationInput = forwardRef<HTMLInputElement, DurationInputProps>(
  function DurationInput(
    {
      value: valueProp,
      defaultValue = null,
      onValueChange,
      min = 0,
      max,
      step = 15,
      largestUnit = 'h',
      minutesPerDay = 1440,
      format,
      parse,
      invalid,
      size = 'md',
      messages: messagesProp,
      placeholder = 'e.g. 1h 30m',
      className,
      style,
      onBlur,
      onKeyDown,
      ...rest
    },
    ref,
  ) {
    const id = useId();
    const messageId = `mn-duration-input-${id}-message`;
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const show = format ?? ((m: number) => formatDuration(m, { largestUnit, minutesPerDay }));
    const read = parse ?? ((t: string) => parseDuration(t, { minutesPerDay }));

    const [internal, setInternal] = useState<number | null>(defaultValue);
    const value = valueProp !== undefined ? valueProp : internal;
    const [draft, setDraft] = useState(value === null ? '' : show(value));
    const [error, setError] = useState<string | null>(null);

    // Show the value again whenever it changes from outside.
    useEffect(() => {
      setDraft(value === null ? '' : show(value));
      setError(null);
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [value]);

    const set = (next: number | null) => {
      if (valueProp === undefined) setInternal(next);
      setDraft(next === null ? '' : show(next));
      setError(null);
      if (next !== value) onValueChange?.(next);
    };

    const outOfRange = (m: number) => m < min || (max !== undefined && m > max);

    const commit = () => {
      const parsed = read(draft);
      if (parsed === null) return set(null);
      if (Number.isNaN(parsed)) return setError(messages.invalid);
      if (outOfRange(parsed)) return setError(messages.range(min, max));
      set(parsed);
    };

    const clamp = (m: number) => Math.min(max ?? Number.POSITIVE_INFINITY, Math.max(min, m));

    const stepBy = (delta: number) => {
      const parsed = read(draft);
      const base = parsed === null || Number.isNaN(parsed) ? (value ?? min) : parsed;
      const unit = Math.abs(delta);
      // Snap to the step grid: 50 + 15 → 60, 50 − 15 → 45.
      const next =
        delta > 0 ? Math.floor(base / unit) * unit + unit : Math.ceil(base / unit) * unit - unit;
      set(clamp(next));
    };

    const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
      onKeyDown?.(e);
      if (e.defaultPrevented || rest.readOnly || rest.disabled) return;
      switch (e.key) {
        case 'ArrowUp':
          e.preventDefault();
          stepBy(step);
          break;
        case 'ArrowDown':
          e.preventDefault();
          stepBy(-step);
          break;
        case 'PageUp':
          e.preventDefault();
          stepBy(60);
          break;
        case 'PageDown':
          e.preventDefault();
          stepBy(-60);
          break;
        case 'Home':
          if (e.ctrlKey || e.metaKey) break;
          e.preventDefault();
          set(min);
          break;
        case 'End':
          if (max === undefined || e.ctrlKey || e.metaKey) break;
          e.preventDefault();
          set(max);
          break;
        case 'Enter':
          e.preventDefault();
          commit();
          break;
        case 'Escape':
          if (error || draft !== (value === null ? '' : show(value))) {
            e.preventDefault();
            setDraft(value === null ? '' : show(value));
            setError(null);
          }
          break;
      }
    };

    const describedBy = [rest['aria-describedby'], error ? messageId : undefined]
      .filter(Boolean)
      .join(' ');
    const fieldProps = useFieldControl(
      { ...rest, 'aria-describedby': describedBy || undefined },
      { invalid: error ? true : invalid },
    );

    return (
      <div className={cx('mn-duration-input', className)} style={style}>
        <input
          ref={ref}
          {...fieldProps}
          type="text"
          inputMode="text"
          role="spinbutton"
          autoComplete="off"
          aria-valuenow={value ?? undefined}
          aria-valuetext={value === null ? undefined : messages.valueText(value)}
          aria-valuemin={min}
          aria-valuemax={max}
          className="mn-input mn-duration-input__input"
          data-size={size}
          placeholder={placeholder}
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
        {error ? (
          <p id={messageId} className="mn-duration-input__message" role="alert">
            {error}
          </p>
        ) : null}
      </div>
    );
  },
);
