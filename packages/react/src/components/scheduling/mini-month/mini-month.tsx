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
import { ChevronRightGlyph } from '../../../internal/icons.js';
import {
  addDays,
  addMonths,
  addYears,
  clampIso,
  endOfMonth,
  endOfWeek,
  formatDayNumber,
  formatFullDate,
  formatMonthTitle,
  isIsoDate,
  isoWeekNumber,
  isSameMonth,
  isWithin,
  localeWeekStart,
  monthGrid,
  startOfMonth,
  startOfWeek,
  useToday,
  weekdayNames,
  type IsoDate,
  type Weekday,
} from '../../../internal/dates/index.js';

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
}

const DEFAULT_LABELS: MiniMonthLabels = {
  previousMonth: 'Previous month',
  nextMonth: 'Next month',
  week: 'Week',
  weekNumber: (n) => `Week ${n}`,
};

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

function isRtl(el: Element | null): boolean {
  if (!el) return false;
  const dir = el.closest('[dir]')?.getAttribute('dir');
  if (dir === 'rtl' || dir === 'ltr') return dir === 'rtl';
  return typeof getComputedStyle !== 'undefined' && getComputedStyle(el).direction === 'rtl';
}

/**
 * A month calendar grid with full keyboard support, following the WAI-ARIA
 * APG date-picker grid: arrow keys move by day and week, Home and End to the
 * week's ends, PageUp and PageDown by month (with Shift, by year), Enter or
 * Space selects. One roving Tab stop; `aria-selected` on the chosen days and
 * `aria-current="date"` on today. The foundation of DatePicker and
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
    locale,
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
  const labels = { ...DEFAULT_LABELS, ...labelsProp };
  const baseId = useId();
  const rootRef = useRef<HTMLDivElement>(null);
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
    if (focusOnMount) focusDay(tabStop);
    // Only on mount: opening a popover focuses the current day once.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const moveFocus = (target: IsoDate) => {
    const t = clampIso(target, min, max);
    if (t < firstMonth) setMonth(t);
    else if (t > visibleEnd) setMonth(addMonths(startOfMonth(t), -(count - 1)));
    pendingFocus.current = true;
    setFocused(t);
    onDayHover?.(t);
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
    const rtl = isRtl(rootRef.current);
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
      <div className="mn-mini-month__months">
        {months.map((m, index) => {
          const titleId = `${baseId}-title-${index}`;
          const weeks = monthGrid(m, weekStartsOn, fixedWeeks);
          const outside = showOutsideDays && count === 1;
          return (
            <div key={m} className="mn-mini-month__month">
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
                <span
                  id={titleId}
                  className="mn-mini-month__title"
                  aria-live={index === 0 ? 'polite' : undefined}
                >
                  {formatMonthTitle(m, locale)}
                </span>
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
                                data-unavailable={(disabled && isWithin(d, min, max)) || undefined}
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
      {footer ? <div className="mn-mini-month__footer">{footer}</div> : null}
    </div>
  );
});
