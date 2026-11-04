'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useImperativeHandle,
  useRef,
  useState,
  type InputHTMLAttributes,
  type KeyboardEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CalendarGlyph, CloseGlyph } from '../../../internal/icons.js';
import {
  addDays,
  dateFormatHint,
  formatFullDate,
  formatIsoDate,
  formatNumericDate,
  parseDateInput,
  useDateText,
  useToday,
  validateIsoDate,
  type DateValidationError,
  type IsoDate,
  type Weekday,
} from '../../../internal/dates/index.js';
import { useFieldControl } from '../../forms/form-field/index.js';
import {
  Popover,
  PopoverAnchor,
  PopoverContent,
  PopoverTrigger,
} from '../../overlays/popover/index.js';
import { MiniMonth, type MiniMonthDayInfo } from '../mini-month/index.js';

export type { DateValidationError };

/**
 * A quick pick shown next to the calendar. Give a fixed `date`, or `days`
 * from today (0 = today, 1 = tomorrow, -1 = yesterday).
 */
export type DatePickerPreset =
  { label: string; date: IsoDate; days?: never } | { label: string; days: number; date?: never };

export interface DatePickerMessages {
  /** Shown when the text is not a date. Receives the format hint, e.g. "DD/MM/YYYY". */
  invalid: (hint: string) => string;
  /** Shown for a date before `min`. Receives `min`, formatted. */
  beforeMin: (min: string) => string;
  /** Shown for a date after `max`. Receives `max`, formatted. */
  afterMax: (max: string) => string;
  /** Shown for a date blocked by `isDateDisabled`. */
  unavailable: string;
  /** Accessible name of the calendar button while empty. */
  openCalendar: string;
  /** Accessible name of the calendar button with a value. Receives the full date. */
  changeDate: (date: string) => string;
  /** Accessible name of the clear button. */
  clear: string;
  /** Accessible name of the calendar popover. */
  dialogLabel: string;
  /** Accessible name of the presets group. */
  presets: string;
}

const DEFAULT_MESSAGES: DatePickerMessages = {
  invalid: (hint) => `Enter a date as ${hint}.`,
  beforeMin: (min) => `Choose ${min} or later.`,
  afterMax: (max) => `Choose ${max} or earlier.`,
  unavailable: 'That date is not available.',
  openCalendar: 'Choose date',
  changeDate: (date) => `Change date, ${date}`,
  clear: 'Clear date',
  dialogLabel: 'Choose date',
  presets: 'Quick picks',
};

export interface DatePickerProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
  'value' | 'defaultValue' | 'onChange' | 'size' | 'min' | 'max' | 'type'
> {
  /** The date (controlled), `YYYY-MM-DD`, or `null` for none. */
  value?: IsoDate | null | undefined;
  /** Initial date (uncontrolled). */
  defaultValue?: IsoDate | null | undefined;
  /** Called with a new valid date, or `null` when cleared. Never called with invalid text. */
  onValueChange?: ((value: IsoDate | null) => void) | undefined;
  /** Earliest allowed date. Earlier typed dates show an error; earlier days are disabled. */
  min?: IsoDate | undefined;
  /** Latest allowed date. */
  max?: IsoDate | undefined;
  /** Blocks extra dates (weekends, holidays). */
  isDateDisabled?: ((date: IsoDate) => boolean) | undefined;
  /** Quick picks such as Today and Tomorrow, shown beside the calendar. */
  presets?: ReadonlyArray<DatePickerPreset> | undefined;
  /** Show a clear button while there is a value. Default true. */
  clearable?: boolean | undefined;
  /** Control height. Default `md`. */
  size?: 'sm' | 'md' | 'lg' | undefined;
  /** Marks the field invalid. Inside a Field, the Field's `invalid` is used. Built-in errors also set it. */
  invalid?: boolean | undefined;
  /** BCP 47 locale for typing, display and the calendar. Default: the runtime's. Pass it for server rendering. */
  locale?: string | undefined;
  /** Display format of the text. Default: the locale's numeric date (`04/11/2026`). */
  formatOptions?: Intl.DateTimeFormatOptions | undefined;
  /** First day of the calendar week, 0 (Sunday) to 6. Default: from the locale. */
  weekStartsOn?: Weekday | undefined;
  /** Show ISO week numbers in the calendar. */
  showWeekNumbers?: boolean | undefined;
  /** Dots and descriptions for calendar days (see MiniMonth). */
  getDayInfo?: ((date: IsoDate) => MiniMonthDayInfo | undefined) | undefined;
  /** Today's date. Default: the device's. */
  today?: IsoDate | undefined;
  /** Whether the calendar is open (controlled). */
  open?: boolean | undefined;
  /** Called when the calendar opens or closes. */
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Called when the built-in validation result changes (`null` when valid). */
  onValidationChange?: ((error: DateValidationError | null) => void) | undefined;
  /** Render the built-in error message under the field. Default true. Turn off to show your own FieldError. */
  showErrors?: boolean | undefined;
  /** Text for translation. */
  messages?: Partial<DatePickerMessages> | undefined;
  /** Class for the outer wrapper (the input itself is `.mn-date-picker__input`). */
  className?: string | undefined;
}

/** Focuses the roving Tab stop of the first calendar grid inside `container`. */
function focusCurrentDay(container: EventTarget | null) {
  if (container instanceof HTMLElement)
    container.querySelector<HTMLElement>('[data-date][tabindex="0"]')?.focus();
}

/**
 * A date field: type a date in the locale's format, or pick it from a
 * calendar popover with optional quick picks. The value is an ISO
 * `YYYY-MM-DD` string with no time or time zone. Validates min, max and
 * disabled dates, and reports errors through Field.
 */
export const DatePicker = forwardRef<HTMLInputElement, DatePickerProps>(function DatePicker(
  {
    value: valueProp,
    defaultValue = null,
    onValueChange,
    min,
    max,
    isDateDisabled,
    presets,
    clearable = true,
    size = 'md',
    invalid,
    locale,
    formatOptions,
    weekStartsOn,
    showWeekNumbers,
    getDayInfo,
    today: todayProp,
    open: openProp,
    onOpenChange,
    onValidationChange,
    showErrors = true,
    messages: messagesProp,
    className,
    name,
    placeholder,
    readOnly,
    onBlur,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const today = useToday(todayProp);
  const inputRef = useRef<HTMLInputElement>(null);
  useImperativeHandle(ref, () => inputRef.current as HTMLInputElement);
  const errorId = `${useId()}-error`;

  const [inner, setInner] = useState<IsoDate | null>(defaultValue);
  const value = valueProp !== undefined ? valueProp : inner;
  const commitValue = (next: IsoDate | null) => {
    if (valueProp === undefined) setInner(next);
    onValueChange?.(next);
  };

  const [innerOpen, setInnerOpen] = useState(false);
  const open = openProp ?? innerOpen;
  const setOpen = (next: boolean) => {
    if (openProp === undefined) setInnerOpen(next);
    onOpenChange?.(next);
  };

  const hint = dateFormatHint(locale);
  const format = (iso: IsoDate) =>
    formatOptions ? formatIsoDate(iso, locale, formatOptions) : formatNumericDate(iso, locale);
  const constraints = { min, max, isDateDisabled };
  const field = useDateText({
    value,
    format,
    parse: (t) => parseDateInput(t, { locale, referenceDate: today }),
    validate: (iso) => validateIsoDate(iso, constraints),
    onCommit: commitValue,
  });

  const reported = useRef<DateValidationError | null>(null);
  const validationRef = useRef(onValidationChange);
  validationRef.current = onValidationChange;
  useEffect(() => {
    if (reported.current === field.error) return;
    reported.current = field.error;
    validationRef.current?.(field.error);
  }, [field.error]);

  const short = (iso: IsoDate) => formatIsoDate(iso, locale, { dateStyle: 'medium' });
  const message =
    field.error === 'invalid'
      ? messages.invalid(hint)
      : field.error === 'before-min' && min
        ? messages.beforeMin(short(min))
        : field.error === 'after-max' && max
          ? messages.afterMax(short(max))
          : field.error
            ? messages.unavailable
            : null;
  const showError = showErrors && !!message;

  const control = useFieldControl(
    {
      ...rest,
      'aria-describedby': cx(rest['aria-describedby'], showError && errorId),
    },
    { invalid: invalid || !!field.error || undefined },
  );
  const disabled = !!control.disabled;

  const choose = (iso: IsoDate | null) => {
    field.set(iso);
    setOpen(false);
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    onKeyDown?.(event);
    if (event.defaultPrevented) return;
    if (event.key === 'Enter') field.commit();
    else if (event.key === 'ArrowDown' && !readOnly) {
      event.preventDefault();
      field.commit();
      setOpen(true);
    }
  };

  const resolvedPresets = (presets ?? []).map((p) => {
    const date = p.date ?? addDays(today, p.days ?? 0);
    return { label: p.label, date, disabled: !!validateIsoDate(date, constraints) };
  });

  return (
    <div
      className={cx('mn-date-picker', className)}
      data-size={size}
      data-disabled={disabled || undefined}
      data-invalid={control['aria-invalid'] ? '' : undefined}
    >
      <Popover
        open={open && !disabled && !readOnly}
        onOpenChange={(next) => {
          if (next) field.commit();
          setOpen(next);
        }}
        modal
      >
        <PopoverAnchor asChild>
          <div
            className="mn-input mn-date-picker__box"
            data-size={size}
            data-disabled={disabled || undefined}
          >
            <input
              ref={inputRef}
              {...control}
              type="text"
              autoComplete="off"
              spellCheck={false}
              readOnly={readOnly}
              placeholder={placeholder ?? hint}
              className="mn-date-picker__input"
              value={field.text}
              onChange={(e) => field.onTextChange(e.target.value)}
              onBlur={(e) => {
                onBlur?.(e);
                field.commit();
              }}
              onKeyDown={handleKeyDown}
            />
            {clearable && value && !disabled && !readOnly ? (
              <button
                type="button"
                className="mn-date-picker__clear"
                aria-label={messages.clear}
                onClick={() => {
                  field.set(null);
                  inputRef.current?.focus();
                }}
              >
                <CloseGlyph />
              </button>
            ) : null}
            <PopoverTrigger asChild>
              <button
                type="button"
                className="mn-date-picker__trigger"
                aria-label={
                  value ? messages.changeDate(formatFullDate(value, locale)) : messages.openCalendar
                }
                disabled={disabled || readOnly}
              >
                <CalendarGlyph />
              </button>
            </PopoverTrigger>
          </div>
        </PopoverAnchor>
        <PopoverContent
          className="mn-date-picker__popover"
          aria-label={messages.dialogLabel}
          onOpenAutoFocus={(e) => {
            // Focus the calendar's current day rather than the first button.
            e.preventDefault();
            focusCurrentDay(e.currentTarget);
          }}
          onCloseAutoFocus={(e) => {
            e.preventDefault();
            inputRef.current?.focus();
          }}
        >
          <div
            className="mn-date-picker__panel"
            data-presets={resolvedPresets.length ? '' : undefined}
          >
            {resolvedPresets.length ? (
              <div role="group" aria-label={messages.presets} className="mn-date-picker__presets">
                {resolvedPresets.map((p) => (
                  <button
                    key={p.label}
                    type="button"
                    className="mn-date-picker__preset"
                    disabled={p.disabled}
                    aria-pressed={p.date === value}
                    onClick={() => choose(p.date)}
                  >
                    <span>{p.label}</span>
                    <span className="mn-date-picker__preset-date">
                      {formatIsoDate(p.date, locale, {
                        weekday: 'short',
                        day: 'numeric',
                        month: 'short',
                      })}
                    </span>
                  </button>
                ))}
              </div>
            ) : null}
            <MiniMonth
              value={value}
              onValueChange={(d) => choose(d)}
              min={min}
              max={max}
              isDateDisabled={isDateDisabled}
              getDayInfo={getDayInfo}
              today={today}
              locale={locale}
              weekStartsOn={weekStartsOn}
              showWeekNumbers={showWeekNumbers}
            />
          </div>
        </PopoverContent>
      </Popover>
      {name ? <input type="hidden" name={name} value={value ?? ''} disabled={disabled} /> : null}
      {showError ? (
        <p id={errorId} className="mn-field__error mn-date-picker__error" aria-live="polite">
          {message}
        </p>
      ) : null}
    </div>
  );
});
