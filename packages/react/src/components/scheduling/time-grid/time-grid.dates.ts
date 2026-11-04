/**
 * Private zone and event helpers for TimeGrid, on top of src/internal/dates
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

/** Wall-clock minutes as a time label ("09:30", "9:30 AM"). */
export function formatMinutes(minutes: number, locale?: string, hourCycle?: 'h12' | 'h23') {
  return new Intl.DateTimeFormat(locale, {
    hour: hourStyle(locale, hourCycle),
    minute: '2-digit',
    hourCycle,
    timeZone: 'UTC',
  }).format(new Date(Date.UTC(2000, 0, 1, Math.floor(minutes / 60), minutes % 60)));
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

/** Offset of `zone` from UTC at `instant`, in minutes (Asia/Kolkata → 330). DST-correct. */
function zoneOffsetMinutes(zone: string, instant: Date): number {
  const { day, minutes } = zonedParts(instant, zone);
  const [py = 1970, pm = 1, pd = 1] = day.split('-').map(Number);
  const p = { year: py, month: pm, day: pd };
  const wallAsUtc = Date.UTC(p.year, p.month - 1, p.day, Math.floor(minutes / 60), minutes % 60);
  return Math.round((wallAsUtc - Math.floor(instant.getTime() / 60000) * 60000) / 60000);
}

/**
 * The instant at wall-clock `minutes` on `day` in `zone`. A time inside a
 * spring-forward gap is pushed forward.
 */
export function wallTimeToInstant(day: IsoDay, minutes: number, zone: string): Date {
  const [py = 1970, pm = 1, pd = 1] = day.split('-').map(Number);
  const p = { year: py, month: pm, day: pd };
  const guess = Date.UTC(p.year, p.month - 1, p.day, Math.floor(minutes / 60), minutes % 60);
  let t = guess - zoneOffsetMinutes(zone, new Date(guess)) * 60000;
  t = guess - zoneOffsetMinutes(zone, new Date(t)) * 60000;
  return new Date(t);
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
