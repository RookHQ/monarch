'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useImperativeHandle,
  useRef,
  useState,
  useSyncExternalStore,
  type HTMLAttributes,
  type KeyboardEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CalendarGlyph, CloseGlyph } from '../../../internal/icons.js';
import {
  addDays,
  addMonths,
  dateFormatHint,
  daysBetween,
  endOfMonth,
  formatIsoDate,
  formatNumericDate,
  parseDateInput,
  startOfMonth,
  useDateText,
  useToday,
  validateIsoDate,
  type DateValidationError,
  type IsoDate,
  type Weekday,
} from '../../../internal/dates/index.js';
import { useFieldContext, useFieldControl } from '../../forms/form-field/index.js';
import {
  Popover,
  PopoverAnchor,
  PopoverContent,
  PopoverTrigger,
} from '../../overlays/popover/index.js';
import { MiniMonth, type MiniMonthDayInfo } from '../../scheduling/mini-month/index.js';

/** An inclusive range of calendar dates, both `YYYY-MM-DD`. */
export interface DateRange {
  start: IsoDate;
  end: IsoDate;
}

/**
 * A quick pick. `lastDays: 7` is the seven days ending today, `nextDays: 7`
 * the seven days starting today, `months: 0` this calendar month and
 * `months: -1` the previous one; `range` is a fixed range.
 */
export type DateRangePreset =
  | { label: string; range: DateRange }
  | { label: string; lastDays: number }
  | { label: string; nextDays: number }
  | { label: string; months: number };

/** Why a range cannot be used: one date's error, or the range itself. */
export type DateRangeValidationError =
  DateValidationError | 'end-before-start' | 'too-short' | 'too-long';

export interface DateRangePickerMessages {
  /** Accessible name of the start input, after the field label. Default "start date". */
  start: string;
  /** Accessible name of the end input, after the field label. Default "end date". */
  end: string;
  invalid: (hint: string) => string;
  beforeMin: (min: string) => string;
  afterMax: (max: string) => string;
  unavailable: string;
  endBeforeStart: string;
  /** Receives `minDays`. */
  tooShort: (days: number) => string;
  /** Receives `maxDays`. */
  tooLong: (days: number) => string;
  openCalendar: string;
  /** Receives the formatted start and end. */
  changeDates: (start: string, end: string) => string;
  clear: string;
  dialogLabel: string;
  presets: string;
  /** Announced after the first day is picked in the calendar. Receives the start date. */
  chooseEnd: (start: string) => string;
}

const DEFAULT_MESSAGES: DateRangePickerMessages = {
  start: 'start date',
  end: 'end date',
  invalid: (hint) => `Enter dates as ${hint}.`,
  beforeMin: (min) => `Choose ${min} or later.`,
  afterMax: (max) => `Choose ${max} or earlier.`,
  unavailable: 'That date is not available.',
  endBeforeStart: 'The end date is before the start date.',
  tooShort: (n) => `Choose at least ${n} ${n === 1 ? 'day' : 'days'}.`,
  tooLong: (n) => `Choose at most ${n} ${n === 1 ? 'day' : 'days'}.`,
  openCalendar: 'Choose dates',
  changeDates: (start, end) => `Change dates, ${start} to ${end}`,
  clear: 'Clear dates',
  dialogLabel: 'Choose dates',
  presets: 'Quick picks',
  chooseEnd: (start) => `Start ${start}. Now choose the end date.`,
};

export interface DateRangePickerProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange'
> {
  /** The range (controlled), or `null` for none. */
  value?: DateRange | null | undefined;
  /** Initial range (uncontrolled). */
  defaultValue?: DateRange | null | undefined;
  /** Called with a complete, valid range, or `null` when cleared. */
  onValueChange?: ((value: DateRange | null) => void) | undefined;
  /** Earliest allowed date. */
  min?: IsoDate | undefined;
  /** Latest allowed date. */
  max?: IsoDate | undefined;
  /** Blocks extra dates as a start or an end. */
  isDateDisabled?: ((date: IsoDate) => boolean) | undefined;
  /** Shortest allowed range in days, counting both ends. */
  minDays?: number | undefined;
  /** Longest allowed range in days, counting both ends (e.g. 366 for reports). */
  maxDays?: number | undefined;
  /** Quick picks such as "Last 7 days", shown beside the calendar. */
  presets?: ReadonlyArray<DateRangePreset> | undefined;
  /** Months shown in the calendar. `'auto'` (default) shows two on wide screens and one on narrow. */
  numberOfMonths?: 1 | 2 | 'auto' | undefined;
  /** Show a clear button while there is a value. Default true. */
  clearable?: boolean | undefined;
  /** BCP 47 locale. Default: the runtime's. Pass it for server rendering. */
  locale?: string | undefined;
  /** First day of the calendar week. Default: from the locale. */
  weekStartsOn?: Weekday | undefined;
  /** Show ISO week numbers in the calendar. */
  showWeekNumbers?: boolean | undefined;
  /** Dots and descriptions for calendar days (see MiniMonth). */
  getDayInfo?: ((date: IsoDate) => MiniMonthDayInfo | undefined) | undefined;
  /** Today's date. Default: the device's. */
  today?: IsoDate | undefined;
  /** Control height. Default `md`. */
  size?: 'sm' | 'md' | 'lg' | undefined;
  disabled?: boolean | undefined;
  readOnly?: boolean | undefined;
  required?: boolean | undefined;
  /** Marks the field invalid. Inside a Field, the Field's `invalid` is used. */
  invalid?: boolean | undefined;
  /** Form field name for the start date (`YYYY-MM-DD`). */
  startName?: string | undefined;
  /** Form field name for the end date. */
  endName?: string | undefined;
  /** Whether the calendar is open (controlled). */
  open?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Called when the built-in validation result changes (`null` when valid). */
  onValidationChange?: ((error: DateRangeValidationError | null) => void) | undefined;
  /** Render the built-in error message. Default true. */
  showErrors?: boolean | undefined;
  /** Text for translation. */
  messages?: Partial<DateRangePickerMessages> | undefined;
}

const WIDE = '(min-width: 40rem)';
const subscribeWide = (cb: () => void) => {
  if (typeof window === 'undefined' || !window.matchMedia) return () => {};
  const mq = window.matchMedia(WIDE);
  mq.addEventListener?.('change', cb);
  return () => mq.removeEventListener?.('change', cb);
};
const isWide = () =>
  typeof window !== 'undefined' && !!window.matchMedia && window.matchMedia(WIDE).matches;

function resolvePreset(p: DateRangePreset, today: IsoDate): DateRange {
  if ('range' in p) return p.range;
  if ('lastDays' in p) return { start: addDays(today, -(Math.max(1, p.lastDays) - 1)), end: today };
  if ('nextDays' in p) return { start: today, end: addDays(today, Math.max(1, p.nextDays) - 1) };
  const first = startOfMonth(addMonths(startOfMonth(today), p.months));
  return { start: first, end: endOfMonth(first) };
}

const sameRange = (a: DateRange | null, b: DateRange | null) =>
  a === b || (!!a && !!b && a.start === b.start && a.end === b.end);

/**
 * Two dates that bound a period: typed in the locale's format, or picked on
 * a one- or two-month calendar with a live preview of the range, plus quick
 * picks such as "Last 7 days". Enforces min/max dates and a minimum and
 * maximum length. The value is `{ start, end }` as ISO `YYYY-MM-DD`, inclusive.
 */
export const DateRangePicker = forwardRef<HTMLDivElement, DateRangePickerProps>(
  function DateRangePicker(
    {
      value: valueProp,
      defaultValue = null,
      onValueChange,
      min,
      max,
      isDateDisabled,
      minDays,
      maxDays,
      presets,
      numberOfMonths = 'auto',
      clearable = true,
      locale,
      weekStartsOn,
      showWeekNumbers,
      getDayInfo,
      today: todayProp,
      size = 'md',
      disabled,
      readOnly,
      required,
      invalid,
      startName,
      endName,
      open: openProp,
      onOpenChange,
      onValidationChange,
      showErrors = true,
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
    const startRef = useRef<HTMLInputElement>(null);
    useImperativeHandle(ref, () => rootRef.current as HTMLDivElement);
    const field = useFieldContext();
    const wide = useSyncExternalStore(subscribeWide, isWide, () => false);
    const months = numberOfMonths === 'auto' ? (wide ? 2 : 1) : numberOfMonths;

    const [inner, setInner] = useState<DateRange | null>(defaultValue);
    const value = valueProp !== undefined ? valueProp : inner;
    const emit = (next: DateRange | null) => {
      if (sameRange(next, value)) return;
      if (valueProp === undefined) setInner(next);
      onValueChange?.(next);
    };

    // The typed (or partly picked) start and end, which become the value once complete and valid.
    const [draft, setDraft] = useState<{ start: IsoDate | null; end: IsoDate | null }>({
      start: value?.start ?? null,
      end: value?.end ?? null,
    });
    const [rangeError, setRangeError] = useState<DateRangeValidationError | null>(null);
    useEffect(() => {
      setDraft({ start: value?.start ?? null, end: value?.end ?? null });
      setRangeError(null);
    }, [value?.start, value?.end]);

    const rangeProblem = (start: IsoDate, end: IsoDate): DateRangeValidationError | null => {
      if (end < start) return 'end-before-start';
      const length = daysBetween(start, end) + 1;
      if (minDays && length < minDays) return 'too-short';
      if (maxDays && length > maxDays) return 'too-long';
      return null;
    };

    const updateDraft = (next: { start: IsoDate | null; end: IsoDate | null }) => {
      setDraft(next);
      if (!next.start && !next.end) {
        setRangeError(null);
        emit(null);
        return;
      }
      if (!next.start || !next.end) {
        setRangeError(null);
        return;
      }
      const problem = rangeProblem(next.start, next.end);
      setRangeError(problem);
      if (!problem) emit({ start: next.start, end: next.end });
    };

    const constraints = { min, max, isDateDisabled };
    const format = (iso: IsoDate) => formatNumericDate(iso, locale);
    const parse = (t: string) => parseDateInput(t, { locale, referenceDate: today });
    const validate = (iso: IsoDate) => validateIsoDate(iso, constraints);
    const startText = useDateText({
      value: draft.start,
      format,
      parse,
      validate,
      onCommit: (iso) => updateDraft({ ...draft, start: iso }),
    });
    const endText = useDateText({
      value: draft.end,
      format,
      parse,
      validate,
      onCommit: (iso) => updateDraft({ ...draft, end: iso }),
    });

    const error: DateRangeValidationError | null = startText.error ?? endText.error ?? rangeError;
    const reported = useRef<DateRangeValidationError | null>(null);
    const validationRef = useRef(onValidationChange);
    validationRef.current = onValidationChange;
    useEffect(() => {
      if (reported.current === error) return;
      reported.current = error;
      validationRef.current?.(error);
    }, [error]);

    const [innerOpen, setInnerOpen] = useState(false);
    const open = openProp ?? innerOpen;
    const setOpen = (next: boolean) => {
      if (openProp === undefined) setInnerOpen(next);
      onOpenChange?.(next);
    };

    // Calendar picking: the first day picked is the anchor; hovering or focusing previews the range.
    const [anchor, setAnchor] = useState<IsoDate | null>(null);
    const [hover, setHover] = useState<IsoDate | null>(null);
    const [announcement, setAnnouncement] = useState('');
    useEffect(() => {
      if (!open) {
        setAnchor(null);
        setHover(null);
        setAnnouncement('');
      }
    }, [open]);

    const long = (iso: IsoDate) => formatIsoDate(iso, locale, { dateStyle: 'medium' });
    const pickDay = (d: IsoDate) => {
      if (!anchor) {
        setAnchor(d);
        setHover(d);
        setAnnouncement(messages.chooseEnd(long(d)));
        return;
      }
      const start = d < anchor ? d : anchor;
      const end = d < anchor ? anchor : d;
      startText.show(start);
      endText.show(end);
      updateDraft({ start, end });
      setAnchor(null);
      setOpen(false);
    };

    // While picking, days that would make the range too short or too long are unavailable.
    const calendarDisabled = (d: IsoDate) => {
      if (isDateDisabled?.(d)) return true;
      if (!anchor) return false;
      const length = Math.abs(daysBetween(anchor, d)) + 1;
      return (!!minDays && length < minDays) || (!!maxDays && length > maxDays);
    };

    const hint = dateFormatHint(locale);
    const message = (() => {
      switch (error) {
        case null:
          return null;
        case 'invalid':
          return messages.invalid(hint);
        case 'before-min':
          return min ? messages.beforeMin(long(min)) : null;
        case 'after-max':
          return max ? messages.afterMax(long(max)) : null;
        case 'unavailable':
          return messages.unavailable;
        case 'end-before-start':
          return messages.endBeforeStart;
        case 'too-short':
          return messages.tooShort(minDays ?? 1);
        case 'too-long':
          return messages.tooLong(maxDays ?? 1);
      }
    })();
    const showError = showErrors && !!message;

    const errorId = `${id}-error`;
    const startPartId = `${id}-start`;
    const endPartId = `${id}-end`;
    const outerLabel = field?.labelId ?? ariaLabelledBy;
    const startControl = useFieldControl(
      {
        disabled,
        required,
        'aria-describedby': showError ? errorId : undefined,
      },
      { invalid: invalid || !!startText.error || undefined },
    );
    const endControl = useFieldControl(
      {
        id: `${id}-end-input`,
        disabled,
        required,
        'aria-describedby': showError ? errorId : undefined,
      },
      { invalid: invalid || !!endText.error || !!rangeError || undefined },
    );
    const isDisabled = !!startControl.disabled;

    const onInputKeyDown = (event: KeyboardEvent<HTMLInputElement>, which: 'start' | 'end') => {
      const text = which === 'start' ? startText : endText;
      if (event.key === 'Enter') text.commit();
      else if (event.key === 'ArrowDown' && !readOnly) {
        event.preventDefault();
        text.commit();
        setOpen(true);
      }
    };

    const resolvedPresets = (presets ?? []).map((p) => {
      // Presets are clipped to min and max, so "This month" with max today is month to date.
      const raw = resolvePreset(p, today);
      const range = {
        start: min && raw.start < min ? min : raw.start,
        end: max && raw.end > max ? max : raw.end,
      };
      const bad =
        !!validate(range.start) || !!validate(range.end) || !!rangeProblem(range.start, range.end);
      return { label: p.label, range, disabled: bad };
    });

    const choosePreset = (range: DateRange) => {
      startText.show(range.start);
      endText.show(range.end);
      updateDraft(range);
      setOpen(false);
    };

    const shownRange =
      anchor !== null
        ? { start: anchor, end: null }
        : draft.start
          ? { start: draft.start, end: draft.end }
          : null;
    const preview = anchor !== null && hover ? { start: anchor, end: hover } : null;

    return (
      <div
        ref={rootRef}
        role="group"
        aria-label={ariaLabel}
        aria-labelledby={outerLabel}
        {...rest}
        className={cx('mn-date-range-picker', className)}
        data-size={size}
        data-disabled={isDisabled || undefined}
        data-invalid={startControl['aria-invalid'] || endControl['aria-invalid'] ? '' : undefined}
      >
        <span id={startPartId} hidden>
          {messages.start}
        </span>
        <span id={endPartId} hidden>
          {messages.end}
        </span>
        <Popover
          open={open && !isDisabled && !readOnly}
          onOpenChange={(next) => {
            if (next) {
              startText.commit();
              endText.commit();
            }
            setOpen(next);
          }}
          modal
        >
          <PopoverAnchor asChild>
            <div
              className="mn-input mn-date-range-picker__box"
              data-size={size}
              data-disabled={isDisabled || undefined}
            >
              <input
                ref={startRef}
                {...startControl}
                type="text"
                autoComplete="off"
                spellCheck={false}
                readOnly={readOnly}
                placeholder={hint}
                className="mn-date-range-picker__input"
                aria-labelledby={outerLabel ? `${outerLabel} ${startPartId}` : startPartId}
                value={startText.text}
                onChange={(e) => startText.onTextChange(e.target.value)}
                onBlur={() => startText.commit()}
                onKeyDown={(e) => onInputKeyDown(e, 'start')}
              />
              <span className="mn-date-range-picker__separator" aria-hidden>
                –
              </span>
              <input
                {...endControl}
                type="text"
                autoComplete="off"
                spellCheck={false}
                readOnly={readOnly}
                placeholder={hint}
                className="mn-date-range-picker__input"
                aria-labelledby={outerLabel ? `${outerLabel} ${endPartId}` : endPartId}
                value={endText.text}
                onChange={(e) => endText.onTextChange(e.target.value)}
                onBlur={() => endText.commit()}
                onKeyDown={(e) => onInputKeyDown(e, 'end')}
              />
              {clearable && (draft.start || draft.end) && !isDisabled && !readOnly ? (
                <button
                  type="button"
                  className="mn-date-range-picker__clear"
                  aria-label={messages.clear}
                  onClick={() => {
                    startText.show(null);
                    endText.show(null);
                    updateDraft({ start: null, end: null });
                    startRef.current?.focus();
                  }}
                >
                  <CloseGlyph />
                </button>
              ) : null}
              <PopoverTrigger asChild>
                <button
                  type="button"
                  className="mn-date-range-picker__trigger"
                  aria-label={
                    value
                      ? messages.changeDates(long(value.start), long(value.end))
                      : messages.openCalendar
                  }
                  disabled={isDisabled || readOnly}
                >
                  <CalendarGlyph />
                </button>
              </PopoverTrigger>
            </div>
          </PopoverAnchor>
          <PopoverContent
            className="mn-date-range-picker__popover"
            aria-label={messages.dialogLabel}
            onOpenAutoFocus={(e) => {
              e.preventDefault();
              const el = e.currentTarget;
              if (el instanceof HTMLElement)
                el.querySelector<HTMLElement>('[data-date][tabindex="0"]')?.focus();
            }}
            onCloseAutoFocus={(e) => {
              e.preventDefault();
              startRef.current?.focus();
            }}
          >
            <div
              className="mn-date-range-picker__panel"
              data-presets={resolvedPresets.length ? '' : undefined}
            >
              {resolvedPresets.length ? (
                <div
                  role="group"
                  aria-label={messages.presets}
                  className="mn-date-range-picker__presets"
                >
                  {resolvedPresets.map((p) => (
                    <button
                      key={p.label}
                      type="button"
                      className="mn-date-range-picker__preset"
                      disabled={p.disabled}
                      aria-pressed={sameRange(p.range, value)}
                      onClick={() => choosePreset(p.range)}
                    >
                      {p.label}
                    </button>
                  ))}
                </div>
              ) : null}
              <MiniMonth
                numberOfMonths={months}
                defaultMonth={
                  draft.start && months === 2 && draft.end && draft.end > endOfMonth(draft.start)
                    ? draft.start
                    : (draft.end ?? draft.start ?? undefined)
                }
                range={shownRange}
                rangePreview={preview}
                onDayHover={(d) => {
                  if (anchor) setHover(d);
                }}
                onValueChange={pickDay}
                min={min}
                max={max}
                isDateDisabled={calendarDisabled}
                getDayInfo={getDayInfo}
                today={today}
                locale={locale}
                weekStartsOn={weekStartsOn}
                showWeekNumbers={showWeekNumbers}
              />
            </div>
            <p className="mn-visually-hidden" aria-live="polite">
              {announcement}
            </p>
          </PopoverContent>
        </Popover>
        {startName ? (
          <input type="hidden" name={startName} value={value?.start ?? ''} disabled={isDisabled} />
        ) : null}
        {endName ? (
          <input type="hidden" name={endName} value={value?.end ?? ''} disabled={isDisabled} />
        ) : null}
        {showError ? (
          <p
            id={errorId}
            className="mn-field__error mn-date-range-picker__error"
            aria-live="polite"
          >
            {message}
          </p>
        ) : null}
      </div>
    );
  },
);
