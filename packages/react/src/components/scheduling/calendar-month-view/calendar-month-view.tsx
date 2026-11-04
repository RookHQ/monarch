'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Popover, PopoverAnchor, PopoverContent } from '../../overlays/popover/index.js';
import { EventChip, type CalendarEvent } from '../event-chip/index.js';
import {
  addDays,
  defaultZone,
  eventsByDay,
  formatDay,
  formatTime,
  monthDays,
  zonedParts,
} from './calendar-month-view.dates.js';
import { localeWeekStart, type HourCycle, type Weekday } from '../../../internal/dates/index.js';

/** Per-day decoration supplied by the product (time off, closures, holidays). */
export interface CalendarMonthViewDayState {
  /** Hatch the day. Exposed as `data-hatched`. */
  hatched?: boolean;
  /** Short note beside the date, such as "Holiday". Also added to the day's name. */
  badge?: string;
}

export interface CalendarMonthViewLabels {
  /** Overflow button text. Default `+${n} more`. */
  more?: (n: number) => string;
  /**
   * Name of a day's date button. Default `${date}, ${count} events`, with
   * ", today" and the badge appended when they apply.
   */
  day?: (date: string, count: number) => string;
  /** Added to today's name. Default "today". */
  today?: string;
  /** Name of the narrow-layout summary button. Default `${count} events, ${date}`. */
  summary?: (date: string, count: number) => string;
}

export interface CalendarMonthViewProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Any day in the month to show, `'2026-11-04'`. The product owns month navigation. */
  month: string;
  events?: readonly CalendarEvent[];
  /**
   * Today, `'2026-11-04'`. Inject it for server rendering and tests; when
   * left out it is read from the clock (in `timeZone`) after mounting. `null`
   * marks no day.
   */
  today?: string | null;
  /**
   * First column: 0 = Sunday … 6 = Saturday, as in MiniMonth and the date
   * pickers. Default: the locale's week start.
   */
  weekStartsOn?: Weekday;
  /** Always show six weeks, so the height never jumps between months. Default `true`. */
  fixedWeeks?: boolean;
  /** Events listed in a cell before the "+N more" button. Default 3. */
  maxEvents?: number;
  /** IANA zone events are placed in. Defaults to the runtime's; pass it for server rendering. */
  timeZone?: string;
  /** BCP 47 locale for weekday, date and time labels. */
  locale?: string;
  /** Force a 12- or 24-hour clock (`'12'` or `'24'`, as in the date pickers); by default the locale decides. */
  hourCycle?: HourCycle;
  /** Called when a day's date is activated (click, Enter or Space). */
  onDayClick?: (day: string) => void;
  onEventClick?: (event: CalendarEvent) => void;
  /** Wrap or replace each chip, e.g. in a PopoverTrigger. Keep one focusable element. */
  renderEvent?: (event: CalendarEvent, chip: ReactElement) => ReactNode;
  /** Decorate days: hatching and a short badge. */
  dayState?: (day: string) => CalendarMonthViewDayState | undefined;
  /** The day with the keyboard focus stop (controlled). */
  focusedDay?: string;
  /** Initial focus stop (uncontrolled). Default: today when it is in the month, else the 1st. */
  defaultFocusedDay?: string;
  onFocusedDayChange?: (day: string) => void;
  /**
   * Called when keyboard focus moves to a day in another month (arrows into
   * or past the outside days, Page Up / Page Down), with that day. Change
   * `month` to follow it; the day keeps focus. Without it, focus moves only
   * within the shown weeks.
   */
  onNavigate?: (day: string) => void;
  labels?: CalendarMonthViewLabels;
}

/**
 * A month of days with their events: weekday header, today and outside-month
 * days marked, a "+N more" popover for busy days, arrow-key movement between
 * days, and callbacks for opening a day or an event. Below about 36rem of
 * container width, cells show a dot summary that opens the day's list.
 */
export const CalendarMonthView = forwardRef<HTMLDivElement, CalendarMonthViewProps>(
  function CalendarMonthView(
    {
      month,
      events = [],
      today: todayProp,
      weekStartsOn: weekStartsOnProp,
      fixedWeeks = true,
      maxEvents = 3,
      timeZone,
      locale,
      hourCycle,
      onDayClick,
      onEventClick,
      renderEvent,
      dayState,
      focusedDay: focusedProp,
      defaultFocusedDay,
      onFocusedDayChange,
      onNavigate,
      labels,
      className,
      'aria-label': ariaLabel,
      ...rest
    },
    ref,
  ) {
    const zone = timeZone ?? defaultZone();
    const hc = hourCycle === '12' ? 'h12' : hourCycle === '24' ? 'h23' : undefined;
    const weekStartsOn = weekStartsOnProp ?? localeWeekStart(locale);
    const days = useMemo(
      () => monthDays(month, weekStartsOn, fixedWeeks),
      [month, weekStartsOn, fixedWeeks],
    );
    const byDay = useMemo(() => eventsByDay(events, days, zone), [events, days, zone]);
    const monthKey = month.slice(0, 7);

    const [clockToday, setClockToday] = useState<string | null>(null);
    useEffect(() => {
      if (todayProp !== undefined) return;
      setClockToday(zonedParts(new Date(), zone).day);
    }, [todayProp, zone]);
    const today = todayProp === undefined ? clockToday : todayProp;

    const fallbackFocus = today && today.slice(0, 7) === monthKey ? today : `${monthKey}-01`;
    const [innerFocus, setInnerFocus] = useState<string | undefined>(defaultFocusedDay);
    const requested = focusedProp ?? innerFocus;
    const focused = requested && days.includes(requested) ? requested : fallbackFocus;

    const gridRef = useRef<HTMLDivElement | null>(null);
    const wantFocus = useRef<string | null>(null);
    const setFocus = useCallback(
      (day: string) => {
        if (focusedProp === undefined) setInnerFocus(day);
        if (day !== focused) onFocusedDayChange?.(day);
      },
      [focusedProp, focused, onFocusedDayChange],
    );

    // Move DOM focus once the requested day is rendered (also after a month change).
    useEffect(() => {
      const target = wantFocus.current;
      if (!target) return;
      const btn = gridRef.current?.querySelector<HTMLButtonElement>(
        `[data-date-button="${target}"]`,
      );
      if (btn) {
        wantFocus.current = null;
        btn.focus();
      }
    });

    const moveTo = (day: string) => {
      setFocus(day);
      if (day.slice(0, 7) !== monthKey && onNavigate) {
        // Focus follows once the product has shown the new month.
        wantFocus.current = day;
        onNavigate(day);
        if (!days.includes(day)) return;
      }
      gridRef.current?.querySelector<HTMLButtonElement>(`[data-date-button="${day}"]`)?.focus();
    };

    const onDateKeyDown = (e: KeyboardEvent<HTMLButtonElement>, day: string) => {
      const rtl = e.currentTarget.closest('[dir="rtl"]') !== null;
      const col = days.indexOf(day) % 7;
      const shiftMonth = (n: number) => {
        const [y, m, d] = day.split('-').map(Number) as [number, number, number];
        const target = new Date(Date.UTC(y, m - 1 + n, 1));
        const last = new Date(Date.UTC(target.getUTCFullYear(), target.getUTCMonth() + 1, 0));
        const dd = Math.min(d, last.getUTCDate());
        return `${target.getUTCFullYear()}-${String(target.getUTCMonth() + 1).padStart(2, '0')}-${String(dd).padStart(2, '0')}`;
      };
      let next: string | null = null;
      switch (e.key) {
        case 'ArrowRight':
          next = addDays(day, rtl ? -1 : 1);
          break;
        case 'ArrowLeft':
          next = addDays(day, rtl ? 1 : -1);
          break;
        case 'ArrowDown':
          next = addDays(day, 7);
          break;
        case 'ArrowUp':
          next = addDays(day, -7);
          break;
        case 'Home':
          next = addDays(day, -col);
          break;
        case 'End':
          next = addDays(day, 6 - col);
          break;
        case 'PageUp':
          next = shiftMonth(e.shiftKey ? -12 : -1);
          break;
        case 'PageDown':
          next = shiftMonth(e.shiftKey ? 12 : 1);
          break;
        default:
          return;
      }
      e.preventDefault();
      // Without onNavigate, focus stays inside the shown weeks.
      if (!days.includes(next) && !onNavigate) return;
      moveTo(next);
    };

    const [openDay, setOpenDay] = useState<string | null>(null);
    const opener = useRef<HTMLElement | null>(null);

    const L = {
      more: labels?.more ?? ((n: number) => `+${n} more`),
      day: labels?.day ?? ((date: string, n: number) => `${date}, ${n} event${n === 1 ? '' : 's'}`),
      today: labels?.today ?? 'today',
      summary:
        labels?.summary ??
        ((date: string, n: number) => `${n} event${n === 1 ? '' : 's'}, ${date}`),
    };
    const longDate = (day: string) =>
      formatDay(day, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }, locale);

    const chip = (ev: CalendarEvent, tabbable: boolean) => {
      const node = (
        <EventChip
          variant="dot"
          title={ev.title}
          {...(ev.allDay ? {} : { time: formatTime(ev.start, zone, locale, hc) })}
          {...(ev.status ? { status: ev.status } : {})}
          {...(ev.color ? { color: ev.color } : {})}
          {...(ev.href !== undefined ? { href: ev.href } : {})}
          tabIndex={tabbable ? undefined : -1}
          {...(onEventClick ? { onClick: () => onEventClick(ev) } : {})}
        />
      );
      return renderEvent ? renderEvent(ev, node) : node;
    };

    const weeks: string[][] = [];
    for (let i = 0; i < days.length; i += 7) weeks.push(days.slice(i, i + 7));
    const header = days.slice(0, 7);

    return (
      <div ref={ref} {...rest} className={cx('mn-calendar-month-view', className)}>
        <div
          ref={gridRef}
          role="grid"
          aria-label={
            ariaLabel ?? formatDay(`${monthKey}-01`, { month: 'long', year: 'numeric' }, locale)
          }
          className="mn-calendar-month-view__grid"
        >
          <div role="row" className="mn-calendar-month-view__weekdays">
            {header.map((d) => (
              <div key={d} role="columnheader" className="mn-calendar-month-view__weekday">
                <span aria-hidden>{formatDay(d, { weekday: 'short' }, locale)}</span>
                <span className="mn-visually-hidden">
                  {formatDay(d, { weekday: 'long' }, locale)}
                </span>
              </div>
            ))}
          </div>
          {weeks.map((week) => (
            <div key={week[0]} role="row" className="mn-calendar-month-view__week">
              {week.map((day) => {
                const list = byDay.get(day) ?? [];
                const count = list.length;
                const state = dayState?.(day);
                const isToday = day === today;
                const outside = day.slice(0, 7) !== monthKey;
                const isFocused = day === focused;
                const shownCount = count > maxEvents ? Math.max(0, maxEvents - 1) : count;
                const hidden = count - shownCount;
                const name = [
                  L.day(longDate(day), count),
                  isToday ? L.today : null,
                  state?.badge ?? null,
                ]
                  .filter(Boolean)
                  .join(', ');
                const open = openDay === day;
                const openList = (el: HTMLElement) => {
                  opener.current = el;
                  setOpenDay(day);
                };
                const body = (
                  <div className="mn-calendar-month-view__events-area">
                    {shownCount > 0 ? (
                      <ul className="mn-calendar-month-view__events">
                        {list.slice(0, shownCount).map((ev) => (
                          <li key={ev.id}>{chip(ev, isFocused)}</li>
                        ))}
                      </ul>
                    ) : null}
                    {hidden > 0 ? (
                      <button
                        type="button"
                        className="mn-calendar-month-view__more"
                        tabIndex={isFocused ? undefined : -1}
                        aria-haspopup="dialog"
                        aria-expanded={open}
                        onClick={(e) => openList(e.currentTarget)}
                      >
                        {L.more(hidden)}
                      </button>
                    ) : null}
                    {count > 0 ? (
                      <button
                        type="button"
                        className="mn-calendar-month-view__summary"
                        tabIndex={isFocused ? undefined : -1}
                        aria-haspopup="dialog"
                        aria-expanded={open}
                        aria-label={L.summary(longDate(day), count)}
                        onClick={(e) => openList(e.currentTarget)}
                      >
                        {list.slice(0, 3).map((ev) => (
                          <span
                            key={ev.id}
                            className="mn-calendar-month-view__dot"
                            data-status={ev.status ?? 'confirmed'}
                            style={
                              ev.color
                                ? ({ ['--mn-event-color']: ev.color } as CSSProperties)
                                : undefined
                            }
                          />
                        ))}
                        {count > 3 ? (
                          <span
                            className="mn-calendar-month-view__dot-more"
                            data-more={count - 3}
                            aria-hidden
                          />
                        ) : null}
                      </button>
                    ) : null}
                  </div>
                );
                return (
                  <div
                    key={day}
                    role="gridcell"
                    className="mn-calendar-month-view__day"
                    data-day={day}
                    data-today={isToday || undefined}
                    data-outside={outside || undefined}
                    data-hatched={state?.hatched || undefined}
                    data-focused={isFocused || undefined}
                    onFocus={() => {
                      if (!isFocused) setFocus(day);
                    }}
                  >
                    <div className="mn-calendar-month-view__day-head">
                      <button
                        type="button"
                        className="mn-calendar-month-view__date"
                        data-date-button={day}
                        tabIndex={isFocused ? 0 : -1}
                        aria-label={name}
                        aria-current={isToday ? 'date' : undefined}
                        onKeyDown={(e) => onDateKeyDown(e, day)}
                        onClick={() => onDayClick?.(day)}
                      >
                        {formatDay(day, { day: 'numeric' }, locale)}
                      </button>
                      {state?.badge ? (
                        <span className="mn-calendar-month-view__badge" aria-hidden>
                          {state.badge}
                        </span>
                      ) : null}
                    </div>
                    {count > 0 ? (
                      <Popover open={open} onOpenChange={(o) => setOpenDay(o ? day : null)}>
                        <PopoverAnchor asChild>{body}</PopoverAnchor>
                        <PopoverContent
                          className="mn-calendar-month-view__popover"
                          aria-label={longDate(day)}
                          onCloseAutoFocus={(e) => {
                            e.preventDefault();
                            opener.current?.focus();
                          }}
                        >
                          <p className="mn-calendar-month-view__popover-title" aria-hidden>
                            {formatDay(
                              day,
                              { weekday: 'long', day: 'numeric', month: 'long' },
                              locale,
                            )}
                          </p>
                          <ul className="mn-calendar-month-view__popover-list">
                            {list.map((ev) => (
                              <li key={ev.id}>{chip(ev, true)}</li>
                            ))}
                          </ul>
                        </PopoverContent>
                      </Popover>
                    ) : (
                      body
                    )}
                  </div>
                );
              })}
            </div>
          ))}
        </div>
      </div>
    );
  },
);
