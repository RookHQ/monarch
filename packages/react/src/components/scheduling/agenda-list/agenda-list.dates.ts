/**
 * Private event helpers for AgendaList, on top of src/internal/dates (calendar-date
 * maths, formatting, and instants read in an IANA zone): how events map onto
 * days. Pure and Intl-based, no date library.
 */
import {
  addDays,
  dayOfWeek,
  formatIsoDate,
  isIsoDate,
  zonedParts,
} from '../../../internal/dates/index.js';
import type { CalendarEvent } from '../event-chip/index.js';

export type IsoDay = string;

export { addDays };

/** ISO weekday, 1 = Monday … 7 = Sunday (the WeeklyHoursInterval convention). */
export function weekday(day: IsoDay): number {
  const d = dayOfWeek(day);
  return d === 0 ? 7 : d;
}

/** Formats a calendar day without zone drift. */
export function formatDay(day: IsoDay, options: Intl.DateTimeFormatOptions, locale?: string) {
  return formatIsoDate(day, locale, options);
}

const dayOf = (v: string | Date, zone: string) =>
  typeof v === 'string' && isIsoDate(v) ? v : zonedParts(v, zone).day;

/** The first and last (inclusive) day an all-day event covers. */
export function allDaySpan(event: CalendarEvent, zone: string) {
  const first = dayOf(event.start, zone);
  const last = event.end === undefined ? first : dayOf(event.end, zone);
  return { first, last: last < first ? first : last };
}

/** Every day a timed or all-day event touches, as first and last (inclusive) day. */
export function eventDays(event: CalendarEvent, zone: string) {
  if (event.allDay) return allDaySpan(event, zone);
  const s = zonedParts(event.start, zone);
  if (event.end === undefined) return { first: s.day, last: s.day };
  const e = zonedParts(event.end, zone);
  let last = e.day;
  // An event ending at midnight does not touch the day it ends on.
  if (last > s.day && e.minutes === 0) last = addDays(last, -1);
  return { first: s.day, last: last < s.day ? s.day : last };
}

const startTime = (e: CalendarEvent) =>
  e.start instanceof Date ? e.start.getTime() : new Date(e.start).getTime();

const readable = (v: string | Date) => !Number.isNaN(new Date(v).getTime());

/**
 * True when the event's start (and end, when given) are real dates. An event
 * with an unreadable date is left out instead of throwing in render.
 */
export function isValidEvent(event: CalendarEvent): boolean {
  return readable(event.start) && (event.end === undefined || readable(event.end));
}

/** Events grouped by the days they touch: all-day first, then by start time. */
export function eventsByDay(
  events: readonly CalendarEvent[],
  days: readonly IsoDay[],
  zone: string,
) {
  const map = new Map<IsoDay, CalendarEvent[]>();
  for (const d of days) map.set(d, []);
  const firstDay = days[0];
  const lastDay = days[days.length - 1];
  if (firstDay === undefined || lastDay === undefined) return map;
  for (const e of events) {
    if (!isValidEvent(e)) continue;
    const span = eventDays(e, zone);
    if (span.last < firstDay || span.first > lastDay) continue;
    let d = span.first < firstDay ? firstDay : span.first;
    while (d <= span.last && d <= lastDay) {
      map.get(d)?.push(e);
      d = addDays(d, 1);
    }
  }
  for (const list of map.values())
    list.sort(
      (a, b) =>
        Number(!!b.allDay) - Number(!!a.allDay) ||
        startTime(a) - startTime(b) ||
        a.title.localeCompare(b.title),
    );
  return map;
}
