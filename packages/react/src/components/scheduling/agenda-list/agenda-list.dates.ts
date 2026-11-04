/**
 * Private zone and event helpers for AgendaList, on top of src/internal/dates
 * (calendar-date maths and formatting). What stays here is what
 * internal/dates does not cover yet: instants read in an IANA zone, and how
 * events map onto days. Pure and Intl-based, no date library.
 *
 * TimeGrid, CalendarMonthView and AgendaList each keep a copy of the zone and
 * event helpers; consolidate them into src/internal when the waves settle.
 */
import {
  addDays,
  dayOfWeek,
  formatIsoDate,
  isIsoDate,
  toIsoDate,
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

/** 24-hour clocks pad the hour ("09:00"); 12-hour clocks do not ("9:00 AM"). */
function hourStyle(locale?: string, hourCycle?: 'h12' | 'h23'): '2-digit' | 'numeric' {
  const hc =
    hourCycle ?? new Intl.DateTimeFormat(locale, { hour: 'numeric' }).resolvedOptions().hourCycle;
  return hc === 'h12' || hc === 'h11' ? 'numeric' : '2-digit';
}

export function defaultZone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  } catch {
    return 'UTC';
  }
}

const partsCache = new Map<string, Intl.DateTimeFormat>();
function partsFormatter(zone: string) {
  let f = partsCache.get(zone);
  if (!f) {
    f = new Intl.DateTimeFormat('en-US', {
      timeZone: zone,
      hourCycle: 'h23',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
    partsCache.set(zone, f);
  }
  return f;
}

/** The calendar day and wall-clock minutes (0–1439) of an instant in `zone`. */
export function zonedParts(instant: string | Date | number, zone: string) {
  const date = instant instanceof Date ? instant : new Date(instant);
  const get: Record<string, number> = {};
  for (const p of partsFormatter(zone).formatToParts(date))
    if (p.type !== 'literal') get[p.type] = Number(p.value);
  return {
    day: toIsoDate(get.year ?? 1970, get.month ?? 1, get.day ?? 1),
    minutes: ((get.hour ?? 0) % 24) * 60 + (get.minute ?? 0),
  };
}

export function formatTime(
  instant: string | Date,
  zone: string,
  locale?: string,
  hourCycle?: 'h12' | 'h23',
) {
  return new Intl.DateTimeFormat(locale, {
    hour: hourStyle(locale, hourCycle),
    minute: '2-digit',
    hourCycle,
    timeZone: zone,
  }).format(instant instanceof Date ? instant : new Date(instant));
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
