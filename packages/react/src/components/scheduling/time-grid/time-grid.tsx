'use client';

import {
  Fragment,
  forwardRef,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type KeyboardEvent,
  type MouseEvent,
  type PointerEvent,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { EventChip, type CalendarEvent } from '../event-chip/index.js';
import type { WeeklyHoursInterval } from '../weekly-hours-editor/index.js';
import type { HourCycle } from '../../../internal/dates/index.js';
import {
  allDaySpan,
  daySpan,
  defaultZone,
  formatDay,
  formatMinutes,
  formatTime,
  packColumns,
  wallTimeToInstant,
  weekday,
  zonedParts,
  type IsoDay,
} from './time-grid.dates.js';

/** An empty slot the user chose (click, or Enter on a slot button). */
export interface TimeGridSlot {
  /** The day column, `'2026-11-04'`. */
  day: string;
  /** Wall-clock minutes from midnight in `timeZone`, snapped to `snapMinutes`. */
  minutes: number;
  /** The slot's start instant in `timeZone`. */
  start: Date;
}

/** A finished drag of an event to a new time or day. */
export interface TimeGridEventMove {
  event: CalendarEvent;
  /** New start, keeping the event's duration. */
  start: Date;
  end: Date;
}

/** A block of time drawn hatched and labelled (time off, closed). */
export interface TimeGridUnavailable {
  id: string;
  start: string | Date;
  end: string | Date;
  label?: ReactNode;
}

export interface TimeGridLabels {
  /** Gutter label of the all-day row. Default "All day". */
  allDay?: string;
  /** Name of a slot button. Default `Create event, ${when}`. */
  createEvent?: (when: string) => string;
  /** Added to today's header for assistive technology. Default "Today". */
  today?: string;
  /** Name of a day header button. Default `Open ${day}`. */
  openDay?: (day: string) => string;
}

export interface TimeGridProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The day columns, in order: one for a day view, seven for a week. `'2026-11-04'`. */
  days: readonly string[];
  events?: readonly CalendarEvent[];
  /** IANA zone the grid is drawn in. Defaults to the runtime's zone; pass it for server rendering. */
  timeZone?: string;
  /** BCP 47 locale for day and time labels. */
  locale?: string;
  /** Force a 12- or 24-hour clock (`'12'` or `'24'`, as in the date pickers); by default the locale decides. */
  hourCycle?: HourCycle;
  /**
   * The current time, for the now indicator and today's header. Inject it for
   * server rendering and tests. When left out the grid reads the clock after
   * mounting and moves the line every minute; `null` hides it.
   */
  now?: Date | string | number | null;
  /** First visible hour. Default 0. */
  startHour?: number;
  /** End of the visible range (exclusive hour). Default 24. */
  endHour?: number;
  /** Height of one hour in pixels. Default 48. */
  hourHeight?: number;
  /** Pointer clicks and drags snap to this many minutes. Default 15. */
  snapMinutes?: number;
  /**
   * Working hours as `{ day, start, end }` intervals (ISO weekday 1–7,
   * minutes from midnight), the WeeklyHoursEditor value. Time outside them is
   * shaded. Leave out to shade nothing.
   */
  workingHours?: readonly WeeklyHoursInterval[];
  /** Hatched blocks such as time off or closures. */
  unavailable?: readonly TimeGridUnavailable[];
  /**
   * Minutes from midnight to scroll to on mount and when the first day
   * changes. Default: an hour before the earliest event, else 08:00.
   */
  scrollToMinutes?: number;
  /**
   * Called when an empty slot is chosen, by pointer (snapped) or by keyboard
   * (Enter on a slot button). Turns on the slot buttons.
   */
  onSlotClick?: (slot: TimeGridSlot) => void;
  onEventClick?: (event: CalendarEvent) => void;
  /**
   * Turns on dragging events to a new time or day (pointer only; touch keeps
   * scrolling). The product decides whether to apply or confirm the move.
   * Always offer another way to reschedule, such as an action in the event's
   * popover.
   */
  onEventMove?: (move: TimeGridEventMove) => void;
  /** Which events may be dragged. Default: all, when `onEventMove` is set. */
  canMoveEvent?: (event: CalendarEvent) => boolean;
  /** Makes multi-day headers buttons, for example to open the day view. */
  onDayClick?: (day: string) => void;
  /**
   * Wrap or replace each event's chip, for example in a PopoverTrigger. Keep
   * a single focusable element.
   */
  renderEvent?: (event: CalendarEvent, chip: ReactElement) => ReactNode;
  /** Show the all-day row even when no all-day event is visible. */
  showAllDayRow?: boolean;
  labels?: TimeGridLabels;
}

const DRAG_THRESHOLD = 4;

interface DragState {
  event: CalendarEvent;
  duration: number;
  dayIndex: number;
  startMin: number;
}

function toDate(v: Date | string | number) {
  return v instanceof Date ? v : new Date(v);
}

/** The current time: injected, or read after mount and refreshed every minute. */
function useNow(now: TimeGridProps['now']): Date | null {
  const injected = now === undefined ? undefined : now === null ? null : toDate(now);
  const [tick, setTick] = useState<Date | null>(null);
  useEffect(() => {
    if (now !== undefined) return;
    setTick(new Date());
    const id = window.setInterval(() => setTick(new Date()), 60_000);
    return () => window.clearInterval(id);
  }, [now]);
  return injected === undefined ? tick : injected;
}

/**
 * The day and week view of a calendar: an hour gutter, an all-day row and one
 * column per day, with overlapping events laid out side by side, a now
 * indicator, working-hours shading and click-to-create slots that also work
 * from the keyboard.
 */
export const TimeGrid = forwardRef<HTMLDivElement, TimeGridProps>(function TimeGrid(
  {
    days,
    events = [],
    timeZone,
    locale,
    hourCycle,
    now: nowProp,
    startHour = 0,
    endHour = 24,
    hourHeight = 48,
    snapMinutes = 15,
    workingHours,
    unavailable = [],
    scrollToMinutes,
    onSlotClick,
    onEventClick,
    onEventMove,
    canMoveEvent,
    onDayClick,
    renderEvent,
    showAllDayRow = false,
    labels,
    className,
    style,
    ...rest
  },
  ref,
) {
  const zone = timeZone ?? defaultZone();
  const hc = hourCycle === '12' ? 'h12' : hourCycle === '24' ? 'h23' : undefined;
  const now = useNow(nowProp);
  const nowParts = now ? zonedParts(now, zone) : null;
  const rangeStart = Math.max(0, Math.min(23, startHour)) * 60;
  const rangeEnd = Math.max(rangeStart + 60, Math.min(24, endHour) * 60);
  const minutePx = hourHeight / 60;
  const hours = useMemo(
    () => Array.from({ length: (rangeEnd - rangeStart) / 60 }, (_, i) => rangeStart / 60 + i),
    [rangeStart, rangeEnd],
  );

  const columns = useMemo(
    () =>
      days.map((day) => {
        const timed = packColumns(
          events.flatMap((e) => {
            if (e.allDay) return [];
            const span = daySpan(e.start, e.end, day, zone, snapMinutes);
            if (!span) return [];
            const top = Math.max(span.top, rangeStart);
            const bottom = Math.min(span.bottom, rangeEnd);
            return bottom > top ? [{ item: e, top, bottom }] : [];
          }),
        ).sort((a, b) => a.top - b.top || a.col - b.col);
        const allDay = events.filter((e) => {
          if (!e.allDay) return false;
          const { first, last } = allDaySpan(e, zone);
          return first <= day && day <= last;
        });
        const off = unavailable.flatMap((u) => {
          const span = daySpan(u.start, u.end, day, zone, 1);
          if (!span) return [];
          const top = Math.max(span.top, rangeStart);
          const bottom = Math.min(span.bottom, rangeEnd);
          return bottom > top ? [{ u, top, bottom }] : [];
        });
        let shade: { top: number; bottom: number }[] = [];
        if (workingHours) {
          const w = weekday(day);
          const open = workingHours
            .filter((h) => h.day === w)
            .map((h) => ({ top: h.start, bottom: h.end }))
            .sort((a, b) => a.top - b.top);
          let cursor = rangeStart;
          for (const o of open) {
            if (o.top > cursor) shade.push({ top: cursor, bottom: Math.min(o.top, rangeEnd) });
            cursor = Math.max(cursor, o.bottom);
          }
          if (cursor < rangeEnd) shade.push({ top: cursor, bottom: rangeEnd });
          shade = shade.filter((s) => s.bottom > s.top);
        }
        return { day, timed, allDay, off, shade };
      }),
    [days, events, zone, snapMinutes, rangeStart, rangeEnd, unavailable, workingHours],
  );
  const hasAllDay = showAllDayRow || columns.some((c) => c.allDay.length > 0);

  /* ---- Scrolling ---------------------------------------------------- */
  const scrollerRef = useRef<HTMLDivElement | null>(null);
  const setRefs = useCallback(
    (el: HTMLDivElement | null) => {
      scrollerRef.current = el;
      if (typeof ref === 'function') ref(el);
      else if (ref) ref.current = el;
    },
    [ref],
  );
  const firstTop = Math.min(...columns.flatMap((c) => c.timed.map((t) => t.top)), Infinity);
  const scrollTarget =
    scrollToMinutes ?? (Number.isFinite(firstTop) ? Math.max(0, firstTop - 60) : 8 * 60);
  const scrollKey = `${days[0] ?? ''}:${days.length}`;
  const scrolledFor = useRef<string | null>(null);
  useEffect(() => {
    const el = scrollerRef.current;
    if (!el || scrolledFor.current === scrollKey) return;
    scrolledFor.current = scrollKey;
    // A little above the target, so its hour label is not under the sticky header.
    el.scrollTop = Math.max(0, (scrollTarget - rangeStart) * minutePx - 12);
  }, [scrollKey, scrollTarget, rangeStart, minutePx]);

  /* ---- Keyboard slots ----------------------------------------------- */
  const defaultHour = Math.min(
    Math.max(Math.ceil(scrollTarget / 60), rangeStart / 60),
    rangeEnd / 60 - 1,
  );
  const [activeSlot, setActiveSlot] = useState<{ dayIndex: number; hour: number } | null>(null);
  const active = {
    dayIndex: Math.min(activeSlot?.dayIndex ?? 0, Math.max(0, days.length - 1)),
    hour: Math.min(Math.max(activeSlot?.hour ?? defaultHour, rangeStart / 60), rangeEnd / 60 - 1),
  };
  const bodyRef = useRef<HTMLDivElement | null>(null);

  const focusSlot = (dayIndex: number, hour: number) => {
    setActiveSlot({ dayIndex, hour });
    const btn = bodyRef.current?.querySelector<HTMLButtonElement>(
      `[data-slot="${dayIndex}:${hour}"]`,
    );
    btn?.focus();
    btn?.scrollIntoView?.({ block: 'nearest', inline: 'nearest' });
  };

  const onSlotKeyDown = (e: KeyboardEvent<HTMLButtonElement>, dayIndex: number, hour: number) => {
    const rtl = e.currentTarget.closest('[dir="rtl"]') !== null;
    const first = rangeStart / 60;
    const last = rangeEnd / 60 - 1;
    let next: [number, number] | null = null;
    switch (e.key) {
      case 'ArrowUp':
        next = [dayIndex, Math.max(first, hour - 1)];
        break;
      case 'ArrowDown':
        next = [dayIndex, Math.min(last, hour + 1)];
        break;
      case 'ArrowLeft':
        next = [Math.max(0, Math.min(days.length - 1, dayIndex + (rtl ? 1 : -1))), hour];
        break;
      case 'ArrowRight':
        next = [Math.max(0, Math.min(days.length - 1, dayIndex + (rtl ? -1 : 1))), hour];
        break;
      case 'Home':
        next = e.ctrlKey ? [0, first] : [dayIndex, first];
        break;
      case 'End':
        next = e.ctrlKey ? [days.length - 1, last] : [dayIndex, last];
        break;
      default:
        return;
    }
    e.preventDefault();
    focusSlot(next[0], next[1]);
  };

  const slotFromPointer = (e: MouseEvent<HTMLElement>, day: IsoDay, hour: number): TimeGridSlot => {
    let minutes = hour * 60;
    // detail 0 is a keyboard activation: use the slot's own hour.
    if (e.detail !== 0) {
      const column = e.currentTarget.closest<HTMLElement>('.mn-time-grid__column');
      const rect = column?.getBoundingClientRect();
      if (rect && rect.height > 0) {
        const raw = rangeStart + (e.clientY - rect.top) / minutePx;
        minutes = Math.floor(raw / snapMinutes) * snapMinutes;
        minutes = Math.max(rangeStart, Math.min(rangeEnd - snapMinutes, minutes));
      }
    }
    return { day, minutes, start: wallTimeToInstant(day, minutes, zone) };
  };

  /* ---- Drag to move ------------------------------------------------- */
  const [drag, setDrag] = useState<DragState | null>(null);
  const origin = useRef<{
    x: number;
    y: number;
    grab: number;
    duration: number;
    moved: boolean;
    pointerId: number;
  } | null>(null);
  const suppressClick = useRef(false);

  const pointerTarget = (clientX: number, clientY: number) => {
    const rect = bodyRef.current?.querySelector('.mn-time-grid__columns')?.getBoundingClientRect();
    if (!rect || rect.width === 0) return null;
    const rtl = bodyRef.current?.closest('[dir="rtl"]') !== null;
    let dayIndex = Math.floor(((clientX - rect.left) / rect.width) * days.length);
    if (rtl) dayIndex = days.length - 1 - dayIndex;
    dayIndex = Math.max(0, Math.min(days.length - 1, dayIndex));
    const minutes = rangeStart + (clientY - rect.top) / minutePx;
    return { dayIndex, minutes };
  };

  const movable = (ev: CalendarEvent) => !!onEventMove && (canMoveEvent ? canMoveEvent(ev) : true);

  const onEventPointerDown = (e: PointerEvent<HTMLDivElement>, ev: CalendarEvent, top: number) => {
    if (!movable(ev) || e.button !== 0 || e.pointerType === 'touch') return;
    const at = pointerTarget(e.clientX, e.clientY);
    if (!at) return;
    const start = toDate(ev.start).getTime();
    const end = ev.end === undefined ? start : toDate(ev.end).getTime();
    origin.current = {
      x: e.clientX,
      y: e.clientY,
      grab: at.minutes - top,
      duration: Math.max(snapMinutes, Math.round((end - start) / 60000)),
      moved: false,
      pointerId: e.pointerId,
    };
  };

  const onEventPointerMove = (e: PointerEvent<HTMLDivElement>, ev: CalendarEvent) => {
    const o = origin.current;
    if (!o || o.pointerId !== e.pointerId) return;
    if (!o.moved) {
      if (Math.hypot(e.clientX - o.x, e.clientY - o.y) < DRAG_THRESHOLD) return;
      o.moved = true;
      e.currentTarget.setPointerCapture?.(e.pointerId);
    }
    const at = pointerTarget(e.clientX, e.clientY);
    if (!at) return;
    const snapped = Math.round((at.minutes - o.grab) / snapMinutes) * snapMinutes;
    setDrag({
      event: ev,
      duration: o.duration,
      dayIndex: at.dayIndex,
      startMin: Math.max(0, Math.min(1440 - snapMinutes, snapped)),
    });
  };

  const onEventPointerUp = (e: PointerEvent<HTMLDivElement>) => {
    const o = origin.current;
    origin.current = null;
    if (!o || !o.moved) return;
    if (e.currentTarget.hasPointerCapture?.(e.pointerId))
      e.currentTarget.releasePointerCapture(e.pointerId);
    suppressClick.current = true;
    window.setTimeout(() => {
      suppressClick.current = false;
    }, 0);
    const d = drag;
    setDrag(null);
    const day = d ? days[d.dayIndex] : undefined;
    if (!d || !day || !onEventMove) return;
    const start = wallTimeToInstant(day, d.startMin, zone);
    if (start.getTime() === toDate(d.event.start).getTime()) return;
    onEventMove({
      event: d.event,
      start,
      end: new Date(start.getTime() + d.duration * 60000),
    });
  };

  const cancelDrag = () => {
    origin.current = null;
    setDrag(null);
  };

  // Escape cancels a drag wherever focus is (Safari does not focus buttons on click).
  const dragging = drag !== null;
  useEffect(() => {
    if (!dragging) return;
    const onKey = (e: globalThis.KeyboardEvent) => {
      if (e.key !== 'Escape') return;
      e.preventDefault();
      origin.current = null;
      setDrag(null);
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [dragging]);

  /* ---- Render -------------------------------------------------------- */
  const allDayLabel = labels?.allDay ?? 'All day';
  const createLabel = labels?.createEvent ?? ((when: string) => `Create event, ${when}`);
  const todayLabel = labels?.today ?? 'Today';
  const openDayLabel = labels?.openDay ?? ((day: string) => `Open ${day}`);
  const multi = days.length > 1;
  const today = nowParts?.day;
  const longDay = (day: IsoDay) =>
    formatDay(day, { weekday: 'long', day: 'numeric', month: 'long' }, locale);
  const timeText = (v: string | Date) => formatTime(v, zone, locale, hc);

  const vars = {
    '--mn-time-grid-days': String(days.length),
    '--mn-time-grid-hour': `${hourHeight}px`,
    '--mn-time-grid-height': `${hours.length * hourHeight}px`,
    ...style,
  } as CSSProperties;

  const chipFor = (ev: CalendarEvent, variant: 'bar' | 'block', heightPx: number) => {
    const range =
      ev.end === undefined ? timeText(ev.start) : `${timeText(ev.start)} – ${timeText(ev.end)}`;
    const compact = heightPx < 38;
    const chip = (
      <EventChip
        variant={variant}
        title={ev.title}
        {...(variant === 'bar'
          ? { timeLabel: allDayLabel }
          : compact
            ? { time: timeText(ev.start), timeLabel: range, density: 'compact' as const }
            : { time: range })}
        {...(ev.status ? { status: ev.status } : {})}
        {...(ev.color ? { color: ev.color } : {})}
        {...(ev.detail && !compact && heightPx >= 56 ? { detail: ev.detail } : {})}
        {...(ev.href !== undefined ? { href: ev.href } : {})}
        {...(onEventClick
          ? {
              onClick: (e: MouseEvent<HTMLButtonElement>) => {
                if (suppressClick.current) {
                  e.preventDefault();
                  return;
                }
                onEventClick(ev);
              },
            }
          : {})}
      />
    );
    return renderEvent ? renderEvent(ev, chip) : chip;
  };

  return (
    <div
      ref={setRefs}
      role="group"
      {...rest}
      className={cx('mn-time-grid', className)}
      style={vars}
      data-multi={multi || undefined}
      data-dragging={drag ? '' : undefined}
    >
      <div className="mn-time-grid__inner">
        <div className="mn-time-grid__head">
          <div className="mn-time-grid__corner" aria-hidden />
          {days.map((day) => {
            const isToday = day === today;
            const content = (
              <>
                <span className="mn-time-grid__weekday">
                  {formatDay(day, { weekday: 'short' }, locale)}
                </span>{' '}
                <span className="mn-time-grid__date">
                  {formatDay(day, { day: 'numeric' }, locale)}
                </span>
              </>
            );
            return (
              <div key={day} className="mn-time-grid__day-head" data-today={isToday || undefined}>
                {multi && onDayClick ? (
                  <button
                    type="button"
                    className="mn-time-grid__day-button"
                    // Starts with the visible "Wed 4" (WCAG 2.5.3 label in name).
                    aria-label={`${formatDay(day, { weekday: 'short' }, locale)} ${formatDay(day, { day: 'numeric' }, locale)}, ${openDayLabel(longDay(day))}`}
                    aria-current={isToday ? 'date' : undefined}
                    onClick={() => onDayClick(day)}
                  >
                    {content}
                  </button>
                ) : (
                  <span className="mn-time-grid__day-label">
                    <span className="mn-time-grid__day-short" aria-hidden>
                      {content}
                    </span>
                    <span className="mn-visually-hidden">
                      {isToday ? `${longDay(day)}, ${todayLabel}` : longDay(day)}
                    </span>
                  </span>
                )}
              </div>
            );
          })}
          {hasAllDay ? (
            <>
              <div className="mn-time-grid__all-day-label" aria-hidden>
                {allDayLabel}
              </div>
              {columns.map(({ day, allDay }) => (
                <div
                  key={day}
                  className="mn-time-grid__all-day"
                  role="group"
                  aria-label={`${longDay(day)}, ${allDayLabel}`}
                >
                  {allDay.map((ev) => (
                    <Fragment key={ev.id}>{chipFor(ev, 'bar', 0)}</Fragment>
                  ))}
                </div>
              ))}
            </>
          ) : null}
        </div>

        <div className="mn-time-grid__body" ref={bodyRef}>
          <div className="mn-time-grid__gutter" aria-hidden>
            {hours.map((h, i) =>
              i === 0 ? null : (
                <span
                  key={h}
                  className="mn-time-grid__hour"
                  style={{ top: (h * 60 - rangeStart) * minutePx }}
                >
                  {formatMinutes(h * 60, locale, hc)}
                </span>
              ),
            )}
          </div>
          <div className="mn-time-grid__columns">
            {columns.map(({ day, timed, off, shade }, dayIndex) => {
              const isToday = day === today;
              const ghost = drag && drag.dayIndex === dayIndex ? drag : null;
              return (
                <div
                  key={day}
                  className="mn-time-grid__column"
                  role="group"
                  aria-label={longDay(day)}
                  data-today={isToday || undefined}
                  data-creatable={onSlotClick ? '' : undefined}
                >
                  {shade.map((s) => (
                    <div
                      key={`shade-${s.top}`}
                      className="mn-time-grid__shade"
                      aria-hidden
                      style={{
                        top: (s.top - rangeStart) * minutePx,
                        height: (s.bottom - s.top) * minutePx,
                      }}
                    />
                  ))}
                  {off.map(({ u, top, bottom }) => (
                    <div
                      key={u.id}
                      className="mn-time-grid__unavailable"
                      style={{
                        top: (top - rangeStart) * minutePx,
                        height: (bottom - top) * minutePx,
                      }}
                    >
                      {u.label ? (
                        <span className="mn-time-grid__unavailable-label">{u.label}</span>
                      ) : null}
                    </div>
                  ))}
                  {onSlotClick
                    ? hours.map((h) => {
                        const isActive = active.dayIndex === dayIndex && active.hour === h;
                        const when = `${longDay(day)}, ${formatMinutes(h * 60, locale, hc)}`;
                        return (
                          <button
                            key={h}
                            type="button"
                            className="mn-time-grid__slot"
                            data-slot={`${dayIndex}:${h}`}
                            tabIndex={isActive ? 0 : -1}
                            aria-label={createLabel(when)}
                            style={{ top: (h * 60 - rangeStart) * minutePx }}
                            onFocus={() => setActiveSlot({ dayIndex, hour: h })}
                            onKeyDown={(e) => onSlotKeyDown(e, dayIndex, h)}
                            onClick={(e) => onSlotClick(slotFromPointer(e, day, h))}
                          />
                        );
                      })
                    : null}
                  {timed.map(({ item: ev, top, bottom, col, cols }) => {
                    const heightPx = (bottom - top) * minutePx;
                    return (
                      <div
                        key={ev.id}
                        className="mn-time-grid__event"
                        data-movable={movable(ev) || undefined}
                        data-ghosted={drag?.event.id === ev.id || undefined}
                        style={
                          {
                            top: (top - rangeStart) * minutePx,
                            height: Math.max(heightPx, 18),
                            '--mn-time-grid-col': String(col),
                            '--mn-time-grid-cols': String(cols),
                          } as CSSProperties
                        }
                        onPointerDown={(e) => onEventPointerDown(e, ev, top)}
                        onPointerMove={(e) => onEventPointerMove(e, ev)}
                        onPointerUp={onEventPointerUp}
                        onPointerCancel={cancelDrag}
                        onClickCapture={(e) => {
                          if (suppressClick.current) {
                            e.preventDefault();
                            e.stopPropagation();
                          }
                        }}
                      >
                        {chipFor(ev, 'block', heightPx)}
                      </div>
                    );
                  })}
                  {ghost ? (
                    <div
                      className="mn-time-grid__ghost"
                      aria-hidden
                      style={
                        {
                          top: (ghost.startMin - rangeStart) * minutePx,
                          height: Math.max(ghost.duration * minutePx, 18),
                          ...(ghost.event.color ? { '--mn-event-color': ghost.event.color } : {}),
                        } as CSSProperties
                      }
                    >
                      {formatMinutes(ghost.startMin, locale, hc)} · {ghost.event.title}
                    </div>
                  ) : null}
                  {isToday &&
                  nowParts &&
                  nowParts.minutes >= rangeStart &&
                  nowParts.minutes < rangeEnd ? (
                    <div
                      className="mn-time-grid__now"
                      aria-hidden
                      style={{ top: (nowParts.minutes - rangeStart) * minutePx }}
                    />
                  ) : null}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
});
