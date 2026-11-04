'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useImperativeHandle,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ClockGlyph } from '../../../internal/icons.js';
import {
  formatTime,
  localeHourCycle,
  parseIsoDateTime,
  parseTimeInput,
  toIsoDateTime,
  useToday,
  type HourCycle,
  type IsoDate,
  type IsoDateTime,
  type Weekday,
} from '../../../internal/dates/index.js';
import { useFieldContext, useFieldControl } from '../../forms/form-field/index.js';
import {
  DatePicker,
  type DatePickerMessages,
  type DatePickerPreset,
  type DateValidationError,
} from '../date-picker/index.js';

export type { IsoDateTime, HourCycle };

/** Why a date-time cannot be used: the date part's errors, plus an unreadable time. */
export type DateTimeValidationError = DateValidationError | 'invalid-time';

export interface DateTimePickerMessages extends DatePickerMessages {
  /** Accessible name of the date part, after the field label. Default "date". */
  date: string;
  /** Accessible name of the time part, after the field label. Default "time". */
  time: string;
  /** Shown when the time is not readable. Receives an example, e.g. "2:30 PM". */
  invalidTime: (example: string) => string;
  /** Shown for a time before `min` on the same day. Receives `min`, formatted. */
  timeBeforeMin: (min: string) => string;
  /** Shown for a time after `max` on the same day. Receives `max`, formatted. */
  timeAfterMax: (max: string) => string;
}

const DEFAULT_MESSAGES: Pick<
  DateTimePickerMessages,
  'date' | 'time' | 'invalidTime' | 'timeBeforeMin' | 'timeAfterMax'
> = {
  date: 'date',
  time: 'time',
  invalidTime: (example) => `Enter a time like ${example}.`,
  timeBeforeMin: (min) => `Choose ${min} or later.`,
  timeAfterMax: (max) => `Choose ${max} or earlier.`,
};

export interface DateTimePickerProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange'
> {
  /** The wall-clock date and time (controlled), `YYYY-MM-DDTHH:mm`, or `null`. No time zone: see `timeZone`. */
  value?: IsoDateTime | null | undefined;
  /** Initial value (uncontrolled). */
  defaultValue?: IsoDateTime | null | undefined;
  /** Called with a new valid value, or `null` when the date is cleared. */
  onValueChange?: ((value: IsoDateTime | null) => void) | undefined;
  /** Earliest allowed value, `YYYY-MM-DDTHH:mm`. Earlier days are disabled and earlier times on that day rejected. */
  min?: IsoDateTime | undefined;
  /** Latest allowed value. */
  max?: IsoDateTime | undefined;
  /** Blocks extra dates (weekends, holidays). */
  isDateDisabled?: ((date: IsoDate) => boolean) | undefined;
  /** Minutes the time moves with the Up and Down arrow keys. Default 15. */
  step?: number | undefined;
  /** 12- or 24-hour clock. Default: from the locale. */
  hourCycle?: HourCycle | undefined;
  /** Time used when a date is picked before any time, `HH:mm`. Default `'09:00'`. */
  defaultTime?: string | undefined;
  /**
   * The time zone the value is in, shown after the time: a label such as
   * "IST" or "Asia/Kolkata", or a time-zone select. The picker itself never
   * converts between zones.
   */
  timeZone?: ReactNode | undefined;
  /** Date quick picks (see DatePicker). The time is kept. */
  presets?: ReadonlyArray<DatePickerPreset> | undefined;
  /** BCP 47 locale for typing, display and the calendar. Default: the runtime's. */
  locale?: string | undefined;
  /** First day of the calendar week. Default: from the locale. */
  weekStartsOn?: Weekday | undefined;
  /** Today's date. Default: the device's. */
  today?: IsoDate | undefined;
  /** Control height. Default `md`. */
  size?: 'sm' | 'md' | 'lg' | undefined;
  disabled?: boolean | undefined;
  readOnly?: boolean | undefined;
  required?: boolean | undefined;
  /** Marks both parts invalid. Inside a Field, the Field's `invalid` is used. */
  invalid?: boolean | undefined;
  /** Submits the value (`YYYY-MM-DDTHH:mm`) under this name in a form. */
  name?: string | undefined;
  /** Called when the built-in validation result changes (`null` when valid). */
  onValidationChange?: ((error: DateTimeValidationError | null) => void) | undefined;
  /** Text for translation. */
  messages?: Partial<DateTimePickerMessages> | undefined;
}

/**
 * A date and a wall-clock time in one field: a DatePicker plus a typed time
 * in the locale's 12- or 24-hour clock, with arrow-key steps and a slot for
 * the time zone. The value is `YYYY-MM-DDTHH:mm`, the same as
 * `<input type="datetime-local">`, so it never shifts with the device's zone.
 */
export const DateTimePicker = forwardRef<HTMLDivElement, DateTimePickerProps>(
  function DateTimePicker(
    {
      value: valueProp,
      defaultValue = null,
      onValueChange,
      min,
      max,
      isDateDisabled,
      step = 15,
      hourCycle,
      defaultTime = '09:00',
      timeZone,
      presets,
      locale,
      weekStartsOn,
      today: todayProp,
      size = 'md',
      disabled,
      readOnly,
      required,
      invalid,
      name,
      onValidationChange,
      messages: messagesProp,
      className,
      'aria-label': ariaLabel,
      'aria-labelledby': ariaLabelledBy,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const today = useToday(todayProp);
    const id = useId();
    const rootRef = useRef<HTMLDivElement>(null);
    useImperativeHandle(ref, () => rootRef.current as HTMLDivElement);
    const field = useFieldContext();
    const cycle = hourCycle ?? localeHourCycle(locale);

    const [inner, setInner] = useState<IsoDateTime | null>(defaultValue);
    const value = valueProp !== undefined ? valueProp : inner;
    const parsed = value ? parseIsoDateTime(value) : null;
    const date = parsed?.date ?? null;
    const minutes = parsed?.minutes ?? null;
    const emit = (next: IsoDateTime | null) => {
      if (valueProp === undefined) setInner(next);
      onValueChange?.(next);
    };

    const minParts = min ? parseIsoDateTime(min) : null;
    const maxParts = max ? parseIsoDateTime(max) : null;
    /** The allowed minutes on a given day. */
    const bounds = (d: IsoDate) => ({
      lo: minParts && d === minParts.date ? minParts.minutes : 0,
      hi: maxParts && d === maxParts.date ? maxParts.minutes : 1439,
    });

    const shownTime = minutes === null ? '' : formatTime(minutes, locale, cycle);
    const [timeText, setTimeText] = useState(shownTime);
    const editing = useRef(false);
    const [timeError, setTimeError] = useState<'invalid-time' | 'before-min' | 'after-max' | null>(
      null,
    );
    const [dateError, setDateError] = useState<DateValidationError | null>(null);
    useEffect(() => {
      if (editing.current) return;
      setTimeText(shownTime);
      setTimeError(null);
    }, [shownTime]);

    const error: DateTimeValidationError | null =
      dateError ?? (timeError === 'invalid-time' ? 'invalid-time' : timeError);
    const reported = useRef<DateTimeValidationError | null>(null);
    const validationRef = useRef(onValidationChange);
    validationRef.current = onValidationChange;
    useEffect(() => {
      if (reported.current === error) return;
      reported.current = error;
      validationRef.current?.(error);
    }, [error]);

    const fallbackMinutes = parseTimeInput(defaultTime) ?? 540;

    const onDate = (d: IsoDate | null) => {
      if (!d) {
        emit(null);
        return;
      }
      const { lo, hi } = bounds(d);
      const m = Math.min(
        hi,
        Math.max(lo, minutes ?? parseTimeInput(timeText, locale) ?? fallbackMinutes),
      );
      editing.current = false;
      setTimeError(null);
      emit(toIsoDateTime(d, m));
    };

    const applyTime = (m: number) => {
      const d = date ?? (minParts && today < minParts.date ? minParts.date : today);
      const { lo, hi } = bounds(d);
      if (m < lo || m > hi) {
        setTimeError(m < lo ? 'before-min' : 'after-max');
        return;
      }
      editing.current = false;
      setTimeError(null);
      setTimeText(formatTime(m, locale, cycle));
      const next = toIsoDateTime(d, m);
      if (next !== value) emit(next);
    };

    const commitTime = () => {
      const t = timeText.trim();
      if (!t) {
        editing.current = false;
        // An empty time with a date is not a complete value; restore the last time.
        setTimeText(shownTime);
        setTimeError(null);
        return;
      }
      const m = parseTimeInput(t, locale);
      if (m === null) {
        setTimeError('invalid-time');
        return;
      }
      applyTime(m);
    };

    const onTimeKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
      if (event.key === 'Enter') commitTime();
      if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') return;
      if (readOnly) return;
      event.preventDefault();
      const s = Math.max(1, Math.round(step));
      const current = parseTimeInput(timeText, locale) ?? minutes ?? fallbackMinutes;
      const next =
        event.key === 'ArrowUp'
          ? (Math.floor(current / s) + 1) * s
          : (Math.ceil(current / s) - 1) * s;
      applyTime(((next % 1440) + 1440) % 1440);
    };

    const outerLabel = field?.labelId ?? ariaLabelledBy;
    const datePartId = `${id}-date`;
    const timePartId = `${id}-time`;
    const timeErrorId = `${id}-time-error`;
    const timeInputId = `${id}-time-input`;

    const example = formatTime(870, locale, cycle);
    const timeMessage =
      timeError === 'invalid-time'
        ? messages.invalidTime(example)
        : timeError === 'before-min' && minParts
          ? messages.timeBeforeMin(formatTime(minParts.minutes, locale, cycle))
          : timeError === 'after-max' && maxParts
            ? messages.timeAfterMax(formatTime(maxParts.minutes, locale, cycle))
            : null;

    const timeControl = useFieldControl(
      {
        id: timeInputId,
        disabled,
        required,
        'aria-describedby': timeMessage ? timeErrorId : undefined,
      },
      { invalid: invalid || !!timeError || undefined },
    );

    return (
      <div
        ref={rootRef}
        role="group"
        aria-label={ariaLabel}
        aria-labelledby={field?.labelId ?? ariaLabelledBy}
        {...rest}
        className={cx('mn-date-time-picker', className)}
        data-size={size}
        data-disabled={timeControl.disabled || undefined}
      >
        <span id={datePartId} hidden>
          {messages.date}
        </span>
        <span id={timePartId} hidden>
          {messages.time}
        </span>
        <DatePicker
          className="mn-date-time-picker__date"
          value={date}
          onValueChange={onDate}
          min={minParts?.date}
          max={maxParts?.date}
          isDateDisabled={isDateDisabled}
          presets={presets}
          locale={locale}
          weekStartsOn={weekStartsOn}
          today={todayProp}
          size={size}
          disabled={disabled}
          readOnly={readOnly}
          required={required}
          invalid={invalid}
          onValidationChange={setDateError}
          messages={messagesProp}
          aria-labelledby={outerLabel ? `${outerLabel} ${datePartId}` : datePartId}
        />
        <div className="mn-date-time-picker__time">
          <div
            className="mn-input mn-date-time-picker__time-box"
            data-size={size}
            data-invalid={timeControl['aria-invalid'] ? '' : undefined}
            data-disabled={timeControl.disabled || undefined}
          >
            <ClockGlyph className="mn-date-time-picker__icon" />
            <input
              {...timeControl}
              type="text"
              inputMode="text"
              autoComplete="off"
              spellCheck={false}
              readOnly={readOnly}
              className="mn-date-time-picker__input"
              aria-labelledby={outerLabel ? `${outerLabel} ${timePartId}` : timePartId}
              placeholder={example}
              value={timeText}
              onChange={(e) => {
                editing.current = true;
                setTimeText(e.target.value);
              }}
              onBlur={commitTime}
              onKeyDown={onTimeKeyDown}
            />
          </div>
          {timeMessage ? (
            <p
              id={timeErrorId}
              className="mn-field__error mn-date-time-picker__error"
              aria-live="polite"
            >
              {timeMessage}
            </p>
          ) : null}
        </div>
        {timeZone ? <div className="mn-date-time-picker__zone">{timeZone}</div> : null}
        {name ? (
          <input type="hidden" name={name} value={value ?? ''} disabled={timeControl.disabled} />
        ) : null}
      </div>
    );
  },
);
