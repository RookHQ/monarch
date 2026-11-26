/**
 * Private event helpers for TimeGrid, on top of src/internal/dates (calendar-date
 * maths, formatting, and instants read in an IANA zone): how events map onto
 * days. Pure and Intl-based, no date library.
 */
import {
  addDays,
  dateTimeFormat,
  dayOfWeek,
  formatIsoDate,
  isIsoDate,
  localeHourCycle,
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

const timeFormats = new Map<string, Intl.DateTimeFormat>();

/**
 * The wall-clock time formatter for `zone`, with the hour padded on 24-hour
 * clocks only ("09:30", "9:30 AM"). Cached: the grid formats a label for
 * every slot, hour and event on each render, and re-renders on every pointer
 * move of a drag. An unknown zone falls back to UTC.
 */
export function timeFormat(
  zone: string,
  locale?: string,
  hourCycle?: 'h12' | 'h23',
): Intl.DateTimeFormat {
  const key = `${zone}|${locale ?? ''}|${hourCycle ?? ''}`;
  let format = timeFormats.get(key);
  if (!format) {
    const twelveHour = hourCycle ? hourCycle === 'h12' : localeHourCycle(locale) === '12';
    format = dateTimeFormat(locale, {
      hour: twelveHour ? 'numeric' : '2-digit',
      minute: '2-digit',
      hourCycle,
      timeZone: zone,
    });
    timeFormats.set(key, format);
  }
  return format;
}

/** Wall-clock minutes as a time label ("09:30", "9:30 AM"). */
export function formatMinutes(minutes: number, locale?: string, hourCycle?: 'h12' | 'h23') {
  return timeFormat('UTC', locale, hourCycle).format(
    Date.UTC(2000, 0, 1, Math.floor(minutes / 60), minutes % 60),
  );
}

const dayOf = (v: string | Date, zone: string) =>
  typeof v === 'string' && isIsoDate(v) ? v : zonedParts(v, zone).day;

const readable = (v: string | Date) => !Number.isNaN(new Date(v).getTime());

/**
 * True when the event's start (and end, when given) are real dates. An event
 * with an unreadable date is left out instead of throwing in render.
 */
export function isValidEvent(event: CalendarEvent): boolean {
  return readable(event.start) && (event.end === undefined || readable(event.end));
}

/** The first and last (inclusive) day an all-day event covers. */
export function allDaySpan(event: CalendarEvent, zone: string) {
  const first = dayOf(event.start, zone);
  const last = event.end === undefined ? first : dayOf(event.end, zone);
  return { first, last: last < first ? first : last };
}

/**
 * The [top, bottom) wall-clock minutes of a timed event inside one day, or
 * null when it does not touch the day. Multi-day events are clipped.
 */
export function daySpan(
  start: string | Date,
  end: string | Date | undefined,
  day: IsoDay,
  zone: string,
  minLength = 15,
): { top: number; bottom: number } | null {
  if (!readable(start) || (end !== undefined && !readable(end))) return null;
  const s = zonedParts(start, zone);
  const e = end === undefined ? s : zonedParts(end, zone);
  if (e.day < day || s.day > day) return null;
  if (e.day === day && e.minutes === 0 && s.day !== day) return null;
  const top = s.day < day ? 0 : s.minutes;
  let bottom = e.day > day ? 1440 : e.minutes;
  if (bottom - top < minLength) bottom = Math.min(1440, top + minLength);
  return { top, bottom };
}

export interface Packed<T> {
  item: T;
  top: number;
  bottom: number;
  /** Zero-based column inside its overlap cluster. */
  col: number;
  /** Columns in the cluster. */
  cols: number;
}

/**
 * Side-by-side layout for overlapping intervals: clusters of transitively
 * overlapping items, each in the first free column; every item in a cluster
 * shares the cluster's column count.
 */
export function packColumns<T>(items: { item: T; top: number; bottom: number }[]): Packed<T>[] {
  const sorted = [...items].sort((a, b) => a.top - b.top || b.bottom - a.bottom);
  const out: Packed<T>[] = [];
  let cluster: Packed<T>[] = [];
  let colEnds: number[] = [];
  let clusterEnd = -1;
  const flush = () => {
    for (const c of cluster) c.cols = colEnds.length;
    out.push(...cluster);
    cluster = [];
    colEnds = [];
  };
  for (const it of sorted) {
    if (cluster.length && it.top >= clusterEnd) flush();
    let col = colEnds.findIndex((end) => end <= it.top);
    if (col === -1) {
      col = colEnds.length;
      colEnds.push(it.bottom);
    } else {
      colEnds[col] = it.bottom;
    }
    cluster.push({ ...it, col, cols: 1 });
    clusterEnd = Math.max(clusterEnd, it.bottom);
  }
  if (cluster.length) flush();
  return out;
}
