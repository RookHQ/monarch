'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph, ChevronRightGlyph } from '../../../internal/icons.js';
import {
  addDays,
  addMonths,
  addYears,
  clampIso,
  endOfMonth,
  endOfWeek,
  formatDayNumber,
  formatFullDate,
  formatIsoDate,
  formatMonthTitle,
  isIsoDate,
  isoWeekNumber,
  isSameMonth,
  isWithin,
  localeWeekStart,
  monthGrid,
  startOfMonth,
  startOfWeek,
  toIsoDate,
  useClientLocale,
  useToday,
  weekdayNames,
  type IsoDate,
  type Weekday,
} from '../../../internal/dates/index.js';
import { useDirection } from '../../../internal/direction.js';

/** A calendar date as an ISO `YYYY-MM-DD` string. */
export type { IsoDate, Weekday };

/** Colour of a day's event dot; each tone is a Monarch status or accent colour. */
export type MiniMonthDotTone = 'accent' | 'neutral' | 'info' | 'success' | 'warning' | 'danger';

/** Extra information for one day, returned by `getDayInfo`. */
export interface MiniMonthDayInfo {
  /** Up to three dots under the day number (events, availability). More are not drawn. */
  dots?: ReadonlyArray<MiniMonthDotTone>;
  /**
   * Added to the day's accessible name, for example `'3 events'` or
   * `'available'`. Dots are decorative, so say here what they mean.
   */
  description?: string;
}

/** A date range highlighted on the grid. `end` may be missing while a range is being picked. */
export interface MiniMonthRange {
  start: IsoDate | null;
  end: IsoDate | null;
}

export interface MiniMonthLabels {
  /** Accessible name of the previous-month button. Default "Previous month". */
  previousMonth: string;
  /** Accessible name of the next-month button. Default "Next month". */
  nextMonth: string;
  /** Header of the week-number column (visually hidden). Default "Week". */
  week: string;
  /** Accessible name of a week number. Default `Week ${n}`. */
  weekNumber: (week: number) => string;
  /** Added to the month title's accessible name; the title opens the month picker. Default "choose month". */
  chooseMonth: string;
  /** Accessible name of the year button in the month picker, which opens the year picker. Default "Choose year". */
  chooseYear: string;
  /** Accessible name of the previous-year button in the month picker. Default "Previous year". */
  previousYear: string;
  /** Accessible name of the next-year button in the month picker. Default "Next year". */
  nextYear: string;
  /** Accessible name of the previous-years button in the year picker. Default "Previous years". */
  previousYears: string;
  /** Accessible name of the next-years button in the year picker. Default "Next years". */
  nextYears: string;
  /** Accessible name of the month picker. Default "Months". */
  months: string;
  /** Accessible name of the year picker. Default "Years". */
  years: string;
}

const DEFAULT_LABELS: MiniMonthLabels = {
  previousMonth: 'Previous month',
  nextMonth: 'Next month',
  week: 'Week',
  weekNumber: (n) => `Week ${n}`,
  chooseMonth: 'choose month',
  chooseYear: 'Choose year',
  previousYear: 'Previous year',
  nextYear: 'Next year',
  previousYears: 'Previous years',
  nextYears: 'Next years',
  months: 'Months',
  years: 'Years',
};

/** Years shown at once in the year picker (3 columns by 4 rows). */
const YEARS_PER_PAGE = 12;
const yearOf = (iso: IsoDate) => Number(iso.slice(0, 4));
const monthStart = (year: number, month: number): IsoDate => toIsoDate(year, month + 1, 1);
const pageStartOf = (year: number) =>
  year - (((year % YEARS_PER_PAGE) + YEARS_PER_PAGE) % YEARS_PER_PAGE);

export interface MiniMonthProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange'
> {
  /** Selected date (controlled), `YYYY-MM-DD`. `null` for none. */
  value?: IsoDate | null | undefined;
  /** Initially selected date (uncontrolled). */
  defaultValue?: IsoDate | null | undefined;
  /** Called with the date the user activates (click, Enter or Space). Not called for disabled dates. */
  onValueChange?: ((date: IsoDate) => void) | undefined;
  /** Any date in the first visible month (controlled). */
  month?: IsoDate | undefined;
  /** Any date in the initially visible month (uncontrolled). Default: the value, else today. */
  defaultMonth?: IsoDate | undefined;
  /** Called with the first day of the newly visible month. */
  onMonthChange?: ((month: IsoDate) => void) | undefined;
  /**
   * Highlights a range instead of a single value: the start and end are
   * selected, the days between are marked in range. For range pickers.
   */
  range?: MiniMonthRange | null | undefined;
  /** A provisional range drawn without selecting it, for example while hovering an end date. */
  rangePreview?: MiniMonthRange | null | undefined;
  /** Called when the pointer enters a day or focus moves to one, and with `null` when it leaves the grid. */
  onDayHover?: ((date: IsoDate | null) => void) | undefined;
  /** Earliest selectable date. Navigation stops at its month. */
  min?: IsoDate | undefined;
  /** Latest selectable date. Navigation stops at its month. */
  max?: IsoDate | undefined;
  /** Marks extra dates unavailable (weekends, holidays, booked days). They stay focusable but cannot be chosen. */
  isDateDisabled?: ((date: IsoDate) => boolean) | undefined;
  /** Dots and an accessible description for a day. Called for every visible day, so keep it cheap. */
  getDayInfo?: ((date: IsoDate) => MiniMonthDayInfo | undefined) | undefined;
  /** Today's date. Default: the device's date, updated at midnight. Pass it to use a workspace time zone. */
  today?: IsoDate | undefined;
  /** BCP 47 locale for names, numerals and the default week start. Default: the runtime's. Pass it for server rendering. */
  locale?: string | undefined;
  /** First day of the week, 0 (Sunday) to 6 (Saturday). Default: from the locale. */
  weekStartsOn?: Weekday | undefined;
  /** Show ISO 8601 week numbers in a leading column. */
  showWeekNumbers?: boolean | undefined;
  /** Show the neighbouring months' days, dimmed and inert. Default true (single month only). */
  showOutsideDays?: boolean | undefined;
  /** Always six week rows, so the height never changes between months. Default true. */
  fixedWeeks?: boolean | undefined;
  /** Consecutive months shown side by side. Default 1. */
  numberOfMonths?: number | undefined;
  /** Focus the current day on mount (when opened in a popover). */
  focusOnMount?: boolean | undefined;
  /** Visible and accessible text, for translation. */
  labels?: Partial<MiniMonthLabels> | undefined;
  /** Content under the grid, such as a "Today" button. */
  footer?: ReactNode | undefined;
}

/**
 * A month calendar grid with full keyboard support, following the WAI-ARIA
 * APG date-picker grid: arrow keys move by day and week, Home and End to the
 * week's ends, PageUp and PageDown by month (with Shift, by year), Enter or
 * Space selects. One roving Tab stop; `aria-selected` on the chosen days and
 * `aria-current="date"` on today. The month title opens a month picker, and
 * its year opens a year picker (12 years a page), so a far date takes a few
 * clicks, not one per month. The foundation of DatePicker and
 * DateRangePicker, and usable on its own as a sidebar calendar.
 */
export const MiniMonth = forwardRef<HTMLDivElement, MiniMonthProps>(function MiniMonth(
  {
    value: valueProp,
    defaultValue = null,
    onValueChange,
    month: monthProp,
    defaultMonth,
    onMonthChange,
    range,
    rangePreview,
    onDayHover,
    min,
    max,
    isDateDisabled,
    getDayInfo,
    today: todayProp,
    locale: localeProp,
    weekStartsOn: weekStartsOnProp,
    showWeekNumbers = false,
    showOutsideDays = true,
    fixedWeeks = true,
    numberOfMonths = 1,
    focusOnMount = false,
    labels: labelsProp,
    footer,
    className,
    onMouseLeave,
    ...rest
  },
  ref,
) {
  const readDir = useDirection();
  const labels = { ...DEFAULT_LABELS, ...labelsProp };
  const baseId = useId();
  const rootRef = useRef<HTMLDivElement>(null);
  // en-US on the server and during hydration, then the viewer's locale.
  const locale = useClientLocale(localeProp);
  const today = useToday(todayProp);
  const weekStartsOn = weekStartsOnProp ?? localeWeekStart(locale);
  const count = Math.max(1, Math.floor(numberOfMonths));

  const [innerValue, setInnerValue] = useState<IsoDate | null>(defaultValue);
  const value = valueProp !== undefined ? valueProp : innerValue;
  const anchor = range?.start ?? value ?? null;

  const [innerMonth, setInnerMonth] = useState<IsoDate | null>(() =>
    defaultMonth && isIsoDate(defaultMonth) ? startOfMonth(defaultMonth) : null,
  );
  const firstMonth = startOfMonth(
    clampIso(
      (monthProp && isIsoDate(monthProp) ? monthProp : null) ??
        innerMonth ??
        (anchor && isIsoDate(anchor) ? anchor : today),
      min ? startOfMonth(min) : null,
      max ? startOfMonth(addMonths(startOfMonth(max), -(count - 1))) : null,
    ),
  );
  const lastMonth = addMonths(firstMonth, count - 1);
  const visibleEnd = endOfMonth(lastMonth);

  const setMonth = useCallback(
    (next: IsoDate) => {
      const first = startOfMonth(next);
      if (monthProp === undefined) setInnerMonth(first);
      onMonthChange?.(first);
    },
    [monthProp, onMonthChange],
  );

  const [focused, setFocused] = useState<IsoDate | null>(null);
  const pendingFocus = useRef(false);

  // The month and year pickers replace the days while open. `anchor` is the month title that opened them.
  const [view, setView] = useState<'days' | 'months' | 'years'>('days');
  const [pickerYear, setPickerYear] = useState(() => yearOf(firstMonth));
  const [yearPage, setYearPage] = useState(() => pageStartOf(yearOf(firstMonth)));
  const [periodFocus, setPeriodFocus] = useState(0);
  const [anchorIndex, setAnchorIndex] = useState(0);
  const pendingPeriodFocus = useRef(false);
  const pendingTitleFocus = useRef<number | null>(null);

  const isDisabled = useCallback(
    (d: IsoDate) => !isWithin(d, min, max) || !!isDateDisabled?.(d),
    [min, max, isDateDisabled],
  );

  const inView = (d: IsoDate | null | undefined): d is IsoDate =>
    !!d && isIsoDate(d) && d >= firstMonth && d <= visibleEnd;

  // The roving Tab stop: the focused day, else the selection, else today, else the first enabled day.
  const tabStop = useMemo(() => {
    for (const c of [focused, value, range?.start, today]) if (inView(c)) return c;
    let d = firstMonth;
    while (d <= visibleEnd && isDisabled(d)) d = addDays(d, 1);
    return d <= visibleEnd ? d : firstMonth;
    // inView is derived from firstMonth and visibleEnd.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [focused, value, range?.start, today, firstMonth, visibleEnd, isDisabled]);

  const focusDay = useCallback((d: IsoDate) => {
    rootRef.current?.querySelector<HTMLButtonElement>(`[data-date="${d}"]`)?.focus();
  }, []);

  useImperativeHandle(ref, () => rootRef.current as HTMLDivElement);

  useEffect(() => {
    if (!pendingFocus.current || !focused) return;
    pendingFocus.current = false;
    focusDay(focused);
  }, [focused, firstMonth, focusDay]);

  useEffect(() => {
    if (view === 'days') {
      const i = pendingTitleFocus.current;
      pendingTitleFocus.current = null;
      if (i !== null)
        rootRef.current?.querySelector<HTMLButtonElement>(`[data-title="${i}"]`)?.focus();
      return;
    }
    if (!pendingPeriodFocus.current) return;
    pendingPeriodFocus.current = false;
    rootRef.current?.querySelector<HTMLButtonElement>(`[data-period="${periodFocus}"]`)?.focus();
  }, [view, periodFocus, pickerYear, yearPage]);

  useEffect(() => {
    if (focusOnMount) focusDay(tabStop);
    // Only on mount: opening a popover focuses the current day once.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const moveFocus = (target: IsoDate) => {
    const t = clampIso(target, min, max);
    if (t < firstMonth) setMonth(t);
    else if (t > visibleEnd) setMonth(addMonths(startOfMonth(t), -(count - 1)));
    onDayHover?.(t);
    // Already on the target (a key pressed at `min` or `max`): nothing will
    // re-render, so a pending focus would fire on a later, unrelated change.
    if (t === focused) return;
    pendingFocus.current = true;
    setFocused(t);
  };

  const select = (d: IsoDate) => {
    if (isDisabled(d)) return;
    if (valueProp === undefined && !range) setInnerValue(d);
    setFocused(d);
    onValueChange?.(d);
  };

  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    const d = (event.target as HTMLElement).dataset?.date;
    if (!d) return;
    const rtl = readDir(rootRef.current) === 'rtl';
    const back = rtl ? 1 : -1;
    let target: IsoDate | null = null;
    switch (event.key) {
      case 'ArrowLeft':
        target = addDays(d, back);
        break;
      case 'ArrowRight':
        target = addDays(d, -back);
        break;
      case 'ArrowUp':
        target = addDays(d, -7);
        break;
      case 'ArrowDown':
        target = addDays(d, 7);
        break;
      case 'Home':
        target = startOfWeek(d, weekStartsOn);
        break;
      case 'End':
        target = endOfWeek(d, weekStartsOn);
        break;
      case 'PageUp':
        target = event.shiftKey ? addYears(d, -1) : addMonths(d, -1);
        break;
      case 'PageDown':
        target = event.shiftKey ? addYears(d, 1) : addMonths(d, 1);
        break;
      case 'Enter':
      case ' ':
        event.preventDefault();
        select(d);
        return;
      default:
        return;
    }
    event.preventDefault();
    moveFocus(target);
  };

  const step = (delta: number) => {
    const next = addMonths(firstMonth, delta);
    setMonth(next);
    // The Tab stop follows to the same day in the new month, without moving focus off the button.
    const base = focused ?? tabStop;
    setFocused(clampIso(addMonths(base, delta), min, max));
  };

  const monthOff = (year: number, month: number) => {
    const first = monthStart(year, month);
    return (!!min && endOfMonth(first) < min) || (!!max && first > max);
  };
  const yearOff = (year: number) =>
    (!!min && monthStart(year, 11) < startOfMonth(min)) ||
    (!!max && monthStart(year, 0) > startOfMonth(max));

  const openMonths = (index: number) => {
    const m = addMonths(firstMonth, index);
    setAnchorIndex(index);
    setPickerYear(yearOf(m));
    setPeriodFocus(Number(m.slice(5, 7)) - 1);
    pendingPeriodFocus.current = true;
    setView('months');
  };
  const openYears = () => {
    const page = pageStartOf(pickerYear);
    setYearPage(page);
    setPeriodFocus(pickerYear - page);
    pendingPeriodFocus.current = true;
    setView('years');
  };
  const chooseYear = (year: number) => {
    if (yearOff(year)) return;
    setPickerYear(year);
    setPeriodFocus(
      Math.min(11, Math.max(0, Number(addMonths(firstMonth, anchorIndex).slice(5, 7)) - 1)),
    );
    pendingPeriodFocus.current = true;
    setView('months');
  };
  const chooseMonth = (month: number) => {
    if (monthOff(pickerYear, month)) return;
    setMonth(addMonths(monthStart(pickerYear, month), -anchorIndex));
    pendingTitleFocus.current = anchorIndex;
    setView('days');
  };
  const stepPeriods = (delta: number) => {
    if (view === 'years') setYearPage((p) => p + delta * YEARS_PER_PAGE);
    else setPickerYear((y) => y + delta);
  };
  const onPeriodKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    const rtl = readDir(rootRef.current) === 'rtl';
    const back = rtl ? 1 : -1;
    let next = periodFocus;
    switch (event.key) {
      case 'ArrowLeft':
        next += back;
        break;
      case 'ArrowRight':
        next -= back;
        break;
      case 'ArrowUp':
        next -= 3;
        break;
      case 'ArrowDown':
        next += 3;
        break;
      case 'Home':
        next = 0;
        break;
      case 'End':
        next = 11;
        break;
      case 'PageUp':
      case 'PageDown':
        event.preventDefault();
        pendingPeriodFocus.current = true;
        stepPeriods(event.key === 'PageUp' ? -1 : 1);
        return;
      default:
        return;
    }
    event.preventDefault();
    pendingPeriodFocus.current = true;
    setPeriodFocus(Math.min(11, Math.max(0, next)));
  };

  const canPrev = !min || endOfMonth(addMonths(firstMonth, -1)) >= min;
  const canNext = !max || addMonths(lastMonth, 1) <= max;

  const longNames = weekdayNames(locale, weekStartsOn, 'long');
  const shortNames = weekdayNames(locale, weekStartsOn, 'short');

  const inRange = (r: MiniMonthRange | null | undefined, d: IsoDate) => {
    if (!r?.start) return { start: false, end: false, within: false };
    const end = r.end ?? r.start;
    const lo = r.start <= end ? r.start : end;
    const hi = r.start <= end ? end : r.start;
    return { start: d === lo, end: d === hi, within: d >= lo && d <= hi };
  };

  const months = Array.from({ length: count }, (_, i) => addMonths(firstMonth, i));

  return (
    // Pointer leave only clears the hover preview; keyboard users get the same through focus.
    // eslint-disable-next-line jsx-a11y/no-static-element-interactions
    <div
      ref={rootRef}
      {...rest}
      className={cx('mn-mini-month', className)}
      data-months={count > 1 ? count : undefined}
      data-week-numbers={showWeekNumbers || undefined}
      onMouseLeave={(e) => {
        onMouseLeave?.(e);
        onDayHover?.(null);
      }}
    >
      <div className="mn-mini-month__stack">
        <div className="mn-mini-month__months" data-covered={view !== 'days' || undefined}>
          {months.map((m, index) => {
            const titleId = `${baseId}-title-${index}`;
            const weeks = monthGrid(m, weekStartsOn, fixedWeeks);
            const outside = showOutsideDays && count === 1;
            return (
              // Keyed by position, not by month: the header buttons must survive a
              // month change, or pressing "Next month" would drop keyboard focus.
              <div key={index} className="mn-mini-month__month">
                <div className="mn-mini-month__header">
                  {index === 0 ? (
                    <button
                      type="button"
                      className="mn-mini-month__nav"
                      data-direction="previous"
                      aria-label={labels.previousMonth}
                      disabled={!canPrev}
                      onClick={() => step(-1)}
                    >
                      <ChevronRightGlyph />
                    </button>
                  ) : (
                    <span className="mn-mini-month__nav-spacer" />
                  )}
                  <button
                    type="button"
                    className="mn-mini-month__title"
                    data-title={index}
                    aria-label={`${formatMonthTitle(m, locale)}, ${labels.chooseMonth}`}
                    aria-haspopup="true"
                    onClick={() => openMonths(index)}
                  >
                    <span id={titleId} aria-live={index === 0 ? 'polite' : undefined}>
                      {formatMonthTitle(m, locale)}
                    </span>
                    <ChevronDownGlyph className="mn-mini-month__title-glyph" />
                  </button>
                  {index === count - 1 ? (
                    <button
                      type="button"
                      className="mn-mini-month__nav"
                      data-direction="next"
                      aria-label={labels.nextMonth}
                      disabled={!canNext}
                      onClick={() => step(1)}
                    >
                      <ChevronRightGlyph />
                    </button>
                  ) : (
                    <span className="mn-mini-month__nav-spacer" />
                  )}
                </div>
                {/* The WAI-ARIA APG date-picker grid: a table with role="grid" and a roving Tab stop. */}
                <table
                  // eslint-disable-next-line jsx-a11y/no-noninteractive-element-to-interactive-role
                  role="grid"
                  className="mn-mini-month__grid"
                  aria-labelledby={titleId}
                  onKeyDown={onKeyDown}
                >
                  <thead>
                    <tr>
                      {showWeekNumbers ? (
                        <th scope="col" className="mn-mini-month__weekday mn-mini-month__week-head">
                          <span className="mn-visually-hidden">{labels.week}</span>
                        </th>
                      ) : null}
                      {shortNames.map((name, i) => (
                        <th
                          key={i}
                          scope="col"
                          abbr={longNames[i]}
                          className="mn-mini-month__weekday"
                        >
                          {name}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {weeks.map((week) => {
                      const weekNo = isoWeekNumber(week[3] ?? week[0] ?? m);
                      return (
                        <tr key={week[0]}>
                          {showWeekNumbers ? (
                            <th
                              scope="row"
                              className="mn-mini-month__week"
                              aria-label={labels.weekNumber(weekNo)}
                            >
                              {weekNo}
                            </th>
                          ) : null}
                          {week.map((d) => {
                            if (!isSameMonth(d, m))
                              return (
                                <td key={d} className="mn-mini-month__cell" data-outside="">
                                  {outside ? (
                                    <span className="mn-mini-month__outside" aria-hidden>
                                      {formatDayNumber(d, locale)}
                                    </span>
                                  ) : null}
                                </td>
                              );
                            const r = inRange(range, d);
                            const p = inRange(rangePreview, d);
                            const selected = range ? r.within : d === value;
                            const disabled = isDisabled(d);
                            const info = getDayInfo?.(d);
                            const dots = info?.dots?.slice(0, 3) ?? [];
                            const name = [formatFullDate(d, locale), info?.description]
                              .filter(Boolean)
                              .join(', ');
                            return (
                              <td
                                key={d}
                                className="mn-mini-month__cell"
                                aria-selected={selected}
                                aria-disabled={disabled || undefined}
                                data-in-range={(range && r.within) || undefined}
                                data-range-start={(range && r.start) || undefined}
                                data-range-end={(range && r.end) || undefined}
                                data-preview={p.within || undefined}
                                data-preview-start={p.start || undefined}
                                data-preview-end={p.end || undefined}
                              >
                                <button
                                  type="button"
                                  className="mn-mini-month__day"
                                  data-date={d}
                                  tabIndex={d === tabStop ? 0 : -1}
                                  aria-label={name}
                                  aria-current={d === today ? 'date' : undefined}
                                  aria-disabled={disabled || undefined}
                                  data-selected={
                                    (range ? r.start || r.end : d === value) || undefined
                                  }
                                  data-today={d === today || undefined}
                                  data-disabled={disabled || undefined}
                                  data-unavailable={
                                    (disabled && isWithin(d, min, max)) || undefined
                                  }
                                  onClick={() => select(d)}
                                  onFocus={() => {
                                    if (focused !== d) setFocused(d);
                                  }}
                                  onMouseEnter={() => onDayHover?.(d)}
                                >
                                  <span className="mn-mini-month__number">
                                    {formatDayNumber(d, locale)}
                                  </span>
                                  {dots.length ? (
                                    <span className="mn-mini-month__dots" aria-hidden>
                                      {dots.map((tone, i) => (
                                        <span
                                          key={i}
                                          className="mn-mini-month__dot"
                                          data-tone={tone}
                                        />
                                      ))}
                                    </span>
                                  ) : null}
                                </button>
                              </td>
                            );
                          })}
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            );
          })}
        </div>
        {view !== 'days' ? (
          <div className="mn-mini-month__picker">
            <div className="mn-mini-month__header">
              <button
                type="button"
                className="mn-mini-month__nav"
                data-direction="previous"
                aria-label={view === 'years' ? labels.previousYears : labels.previousYear}
                disabled={view === 'years' ? yearOff(yearPage - 1) : yearOff(pickerYear - 1)}
                onClick={() => stepPeriods(-1)}
              >
                <ChevronRightGlyph />
              </button>
              {view === 'months' ? (
                <button
                  type="button"
                  className="mn-mini-month__title"
                  aria-label={`${formatIsoDate(monthStart(pickerYear, 0), locale, { year: 'numeric' })}, ${labels.chooseYear}`}
                  aria-haspopup="true"
                  onClick={openYears}
                >
                  <span>
                    {formatIsoDate(monthStart(pickerYear, 0), locale, { year: 'numeric' })}
                  </span>
                  <ChevronDownGlyph className="mn-mini-month__title-glyph" />
                </button>
              ) : (
                <span className="mn-mini-month__title" aria-live="polite">
                  {formatIsoDate(monthStart(yearPage, 0), locale, { year: 'numeric' })}
                  {' – '}
                  {formatIsoDate(monthStart(yearPage + YEARS_PER_PAGE - 1, 0), locale, {
                    year: 'numeric',
                  })}
                </span>
              )}
              <button
                type="button"
                className="mn-mini-month__nav"
                data-direction="next"
                aria-label={view === 'years' ? labels.nextYears : labels.nextYear}
                disabled={
                  view === 'years' ? yearOff(yearPage + YEARS_PER_PAGE) : yearOff(pickerYear + 1)
                }
                onClick={() => stepPeriods(1)}
              >
                <ChevronRightGlyph />
              </button>
            </div>
            {/* A roving Tab stop over 12 buttons; the arrow keys move by one and by row. */}
            {/* eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions */}
            <div
              role="group"
              className="mn-mini-month__periods"
              aria-label={view === 'years' ? labels.years : labels.months}
              onKeyDown={onPeriodKeyDown}
            >
              {Array.from({ length: 12 }, (_, i) => {
                const year = view === 'years' ? yearPage + i : pickerYear;
                const iso = monthStart(year, view === 'years' ? 0 : i);
                const shown = addMonths(firstMonth, anchorIndex);
                const selected =
                  view === 'years'
                    ? year === yearOf(shown)
                    : i === Number(shown.slice(5, 7)) - 1 && year === yearOf(shown);
                const isNow =
                  view === 'years'
                    ? year === yearOf(today)
                    : year === yearOf(today) && i === Number(today.slice(5, 7)) - 1;
                const off = view === 'years' ? yearOff(year) : monthOff(year, i);
                const text =
                  view === 'years'
                    ? formatIsoDate(iso, locale, { year: 'numeric' })
                    : formatIsoDate(iso, locale, { month: 'short' });
                return (
                  <button
                    key={i}
                    type="button"
                    className="mn-mini-month__period"
                    data-period={i}
                    tabIndex={i === periodFocus ? 0 : -1}
                    aria-label={view === 'years' ? text : formatMonthTitle(iso, locale)}
                    aria-pressed={selected}
                    aria-current={isNow ? 'date' : undefined}
                    disabled={off}
                    data-selected={selected || undefined}
                    data-today={isNow || undefined}
                    onFocus={() => setPeriodFocus(i)}
                    onClick={() => (view === 'years' ? chooseYear(year) : chooseMonth(i))}
                  >
                    {text}
                  </button>
                );
              })}
            </div>
          </div>
        ) : null}
      </div>
      {footer ? <div className="mn-mini-month__footer">{footer}</div> : null}
    </div>
  );
});
