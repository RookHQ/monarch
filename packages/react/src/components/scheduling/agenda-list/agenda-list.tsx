'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { EventChip, type CalendarEvent } from '../event-chip/index.js';
import type { HourCycle } from '../../../internal/dates/index.js';
import {
  addDays,
  defaultZone,
  eventsByDay,
  eventDays,
  formatDay,
  formatTime,
  zonedParts,
  type IsoDay,
} from './agenda-list.dates.js';

export interface AgendaListLabels {
  /** Today's marker in its day header. Default "Today". */
  today?: string;
  /** Time column of an all-day event. Default "All day". */
  allDay?: string;
  /** Count in each day header. Default `${n} events`. */
  count?: (n: number) => string;
  /** Default empty-state title. Default "Nothing scheduled". */
  emptyTitle?: string;
}

export interface AgendaListProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  events: readonly CalendarEvent[];
  /**
   * First day listed, `'2026-11-04'`. Default: the first day with an event.
   * Events that started earlier but are still running appear on it.
   */
  from?: string;
  /** How many days from `from` to cover. Default: through the last event. */
  days?: number;
  /** Today, for the marker. Inject for server rendering; read from the clock after mount when left out; `null` for none. */
  today?: string | null;
  /** IANA zone events are grouped in. Defaults to the runtime's; pass it for server rendering. */
  timeZone?: string;
  locale?: string;
  /** Force a 12- or 24-hour clock (`'12'` or `'24'`, as in the date pickers); by default the locale decides. */
  hourCycle?: HourCycle;
  onEventClick?: (event: CalendarEvent) => void;
  /** Wrap or replace each chip (for example in a PopoverTrigger). Keep one focusable element. */
  renderEvent?: (event: CalendarEvent, chip: ReactElement) => ReactNode;
  /**
   * Informational rows shown first in a day, such as time off or a holiday.
   * A day with notes is listed even when it has no events.
   */
  dayNotes?: (day: string) => readonly ReactNode[] | undefined;
  /** List days in the range that have nothing. Default `false`. */
  showEmptyDays?: boolean;
  /** Replaces the default empty state. */
  empty?: ReactNode;
  /** Level of the day headings. Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | 6;
  labels?: AgendaListLabels;
}

/**
 * Events grouped by day, in order: sticky day headings with a count and a
 * "Today" marker, all-day events first, an empty state when nothing is
 * scheduled, and arrow keys to move between events.
 */
export const AgendaList = forwardRef<HTMLDivElement, AgendaListProps>(function AgendaList(
  {
    events,
    from,
    days: dayCount,
    today: todayProp,
    timeZone,
    locale,
    hourCycle,
    onEventClick,
    renderEvent,
    dayNotes,
    showEmptyDays = false,
    empty,
    headingLevel = 3,
    labels,
    className,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const uid = useId();
  const hc = hourCycle === '12' ? 'h12' : hourCycle === '24' ? 'h23' : undefined;
  const zone = timeZone ?? defaultZone();
  const [clockToday, setClockToday] = useState<string | null>(null);
  useEffect(() => {
    if (todayProp !== undefined) return;
    setClockToday(zonedParts(new Date(), zone).day);
  }, [todayProp, zone]);
  const today = todayProp === undefined ? clockToday : todayProp;

  const range = useMemo(() => {
    let first: IsoDay | undefined = from;
    let last: IsoDay | undefined =
      from !== undefined && dayCount !== undefined
        ? addDays(from, Math.max(1, dayCount) - 1)
        : undefined;
    if (first === undefined || last === undefined) {
      for (const e of events) {
        const span = eventDays(e, zone);
        if (first === undefined || (from === undefined && span.first < first)) first = span.first;
        if (dayCount === undefined && (last === undefined || span.last > last)) last = span.last;
      }
    }
    if (first === undefined) return [];
    if (last === undefined || last < first) last = first;
    // Cap runaway ranges (a multi-year event) at about a year of days.
    const out: IsoDay[] = [];
    for (let d = first; d <= last && out.length < 400; d = addDays(d, 1)) out.push(d);
    return out;
  }, [events, from, dayCount, zone]);

  const byDay = useMemo(() => eventsByDay(events, range, zone), [events, range, zone]);
  const rows = range
    .map((day) => ({ day, items: byDay.get(day) ?? [], notes: dayNotes?.(day) ?? [] }))
    .filter((r) => showEmptyDays || r.items.length > 0 || r.notes.length > 0);

  const keys = rows.flatMap((r) => r.items.map((e) => `${r.day}|${e.id}`));
  const [activeKey, setActiveKey] = useState<string | null>(null);
  const current =
    activeKey && keys.includes(activeKey)
      ? activeKey
      : (keys.find((k) => today !== null && today !== undefined && k.slice(0, 10) >= today) ??
        keys[0]);

  const L = {
    today: labels?.today ?? 'Today',
    allDay: labels?.allDay ?? 'All day',
    count: labels?.count ?? ((n: number) => `${n} event${n === 1 ? '' : 's'}`),
    emptyTitle: labels?.emptyTitle ?? 'Nothing scheduled',
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    onKeyDown?.(e);
    if (e.defaultPrevented) return;
    const items = Array.from(e.currentTarget.querySelectorAll<HTMLElement>('[data-agenda-key]'));
    const index = items.findIndex(
      (el) => el === document.activeElement || el.contains(document.activeElement),
    );
    if (index === -1) return;
    const dayOf = (el: HTMLElement | undefined) => el?.dataset.agendaKey?.slice(0, 10);
    let next: number | null = null;
    switch (e.key) {
      case 'ArrowDown':
        next = Math.min(items.length - 1, index + 1);
        break;
      case 'ArrowUp':
        next = Math.max(0, index - 1);
        break;
      case 'Home':
        next = 0;
        break;
      case 'End':
        next = items.length - 1;
        break;
      case 'PageDown': {
        const day = dayOf(items[index]);
        const i = items.findIndex((el) => (dayOf(el) ?? '') > (day ?? ''));
        next = i === -1 ? items.length - 1 : i;
        break;
      }
      case 'PageUp': {
        const day = dayOf(items[index]) ?? '';
        const prevDay = items
          .slice(0, index)
          .map(dayOf)
          .filter((d) => (d ?? '') < day)
          .at(-1);
        next = prevDay === undefined ? 0 : items.findIndex((el) => dayOf(el) === prevDay);
        break;
      }
      default:
        return;
    }
    e.preventDefault();
    const el = items[next];
    if (el) {
      setActiveKey(el.dataset.agendaKey ?? null);
      el.focus();
    }
  };

  if (rows.length === 0)
    return (
      <div ref={ref} {...rest} className={cx('mn-agenda-list', className)} data-empty="">
        {empty ?? <EmptyState title={L.emptyTitle} headingLevel={`h${headingLevel}`} />}
      </div>
    );

  const Heading = `h${headingLevel}` as const;
  const timeFor = (ev: CalendarEvent, day: IsoDay) => {
    if (ev.allDay) return L.allDay;
    const s = zonedParts(ev.start, zone).day;
    const start = formatTime(ev.start, zone, locale, hc);
    if (ev.end === undefined) return start;
    const end = formatTime(ev.end, zone, locale, hc);
    // A timed event continuing from an earlier day shows its end only.
    return s < day ? `– ${end}` : `${start} – ${end}`;
  };

  return (
    // Keyboard delegation for the roving focus between the chips inside; the chips are the controls.
    // eslint-disable-next-line jsx-a11y/no-static-element-interactions
    <div ref={ref} {...rest} className={cx('mn-agenda-list', className)} onKeyDown={handleKeyDown}>
      {rows.map(({ day, items, notes }) => {
        const isToday = day === today;
        const title = formatDay(day, { weekday: 'long', day: 'numeric', month: 'long' }, locale);
        const headingId = `${uid}-${day}`;
        return (
          <section
            key={day}
            className="mn-agenda-list__day"
            aria-labelledby={headingId}
            data-today={isToday || undefined}
          >
            <Heading className="mn-agenda-list__heading" id={headingId}>
              <span className="mn-agenda-list__date">{title}</span>
              {isToday ? (
                <>
                  <span className="mn-visually-hidden">, </span>
                  <span className="mn-agenda-list__today">{L.today}</span>
                </>
              ) : null}
              <span className="mn-visually-hidden">, </span>
              <span className="mn-agenda-list__count">{L.count(items.length)}</span>
            </Heading>
            <ul className="mn-agenda-list__items">
              {notes.map((n, i) => (
                <li key={`note-${i}`} className="mn-agenda-list__note">
                  {n}
                </li>
              ))}
              {items.map((ev) => {
                const key = `${day}|${ev.id}`;
                const chip = (
                  <EventChip
                    variant="bar"
                    title={ev.title}
                    time={timeFor(ev, day)}
                    {...(ev.status ? { status: ev.status } : {})}
                    {...(ev.color ? { color: ev.color } : {})}
                    {...(ev.detail ? { detail: ev.detail } : {})}
                    {...(ev.href !== undefined ? { href: ev.href } : {})}
                    data-agenda-key={key}
                    tabIndex={key === current ? 0 : -1}
                    onFocus={() => setActiveKey(key)}
                    {...(onEventClick ? { onClick: () => onEventClick(ev) } : {})}
                  />
                );
                return (
                  <li key={ev.id} className="mn-agenda-list__item">
                    {renderEvent ? renderEvent(ev, chip) : chip}
                  </li>
                );
              })}
            </ul>
          </section>
        );
      })}
    </div>
  );
});
