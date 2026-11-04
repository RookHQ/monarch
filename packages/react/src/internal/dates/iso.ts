/**
 * Calendar-date maths on ISO `YYYY-MM-DD` strings.
 *
 * A calendar date has no time and no time zone, so every helper here works on
 * the string and on UTC midnights internally: adding a day can never skip or
 * repeat one across a daylight-saving change, and the result never depends on
 * the time zone of the machine running it (server or browser). Pure and
 * SSR-safe: no `window`, no `document`, no ambient "now" unless passed in.
 */

/** A calendar date as `YYYY-MM-DD`, for example `'2026-11-04'`. */
export type IsoDate = string;

/** A wall-clock date and time as `YYYY-MM-DDTHH:mm`, the `datetime-local` format. */
export type IsoDateTime = string;

/** 0 = Sunday … 6 = Saturday, the same numbering as `Date#getUTCDay`. */
export type Weekday = 0 | 1 | 2 | 3 | 4 | 5 | 6;

export interface DateParts {
  year: number;
  /** 1-12. */
  month: number;
  /** 1-31. */
  day: number;
}

const ISO_RE = /^(\d{4})-(\d{2})-(\d{2})$/;
const DAY_MS = 86_400_000;

const pad2 = (n: number) => String(n).padStart(2, '0');
const pad4 = (n: number) => String(n).padStart(4, '0');

/** Days in a month (`month` is 1-12). */
export function daysInMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate();
}

/** Splits an ISO date into numbers, or `null` when it is not a real date (`2026-02-30`). */
export function parseIsoDate(iso: string): DateParts | null {
  const m = ISO_RE.exec(iso);
  if (!m) return null;
  const year = Number(m[1]);
  const month = Number(m[2]);
  const day = Number(m[3]);
  if (month < 1 || month > 12 || day < 1 || day > daysInMonth(year, month)) return null;
  return { year, month, day };
}

/** True for a well-formed, real calendar date. */
export function isIsoDate(value: unknown): value is IsoDate {
  return typeof value === 'string' && parseIsoDate(value) !== null;
}

/**
 * Builds an ISO date from parts. Out-of-range parts roll over the way `Date`
 * does (`month: 13` is January next year), so callers can do arithmetic.
 */
export function toIsoDate(year: number, month: number, day: number): IsoDate {
  const d = new Date(Date.UTC(2000, 0, 1));
  d.setUTCFullYear(year, month - 1, day);
  return `${pad4(d.getUTCFullYear())}-${pad2(d.getUTCMonth() + 1)}-${pad2(d.getUTCDate())}`;
}

function parts(iso: IsoDate): DateParts {
  const p = parseIsoDate(iso);
  if (!p) throw new RangeError(`Not an ISO date (YYYY-MM-DD): ${JSON.stringify(iso)}`);
  return p;
}

/** The UTC-midnight `Date` for an ISO date: the safe bridge to `Intl` (format with `timeZone: 'UTC'`). */
export function isoToUtcDate(iso: IsoDate): Date {
  const { year, month, day } = parts(iso);
  const d = new Date(Date.UTC(2000, 0, 1));
  d.setUTCFullYear(year, month - 1, day);
  return d;
}

/** The calendar date a `Date` falls on in a time zone (default: the runtime's own zone). */
export function dateToIso(date: Date, timeZone?: string): IsoDate {
  if (timeZone === 'UTC')
    return toIsoDate(date.getUTCFullYear(), date.getUTCMonth() + 1, date.getUTCDate());
  if (timeZone === undefined)
    return toIsoDate(date.getFullYear(), date.getMonth() + 1, date.getDate());
  const out = { year: 1970, month: 1, day: 1 };
  for (const p of new Intl.DateTimeFormat('en-US', {
    timeZone,
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
  }).formatToParts(date))
    if (p.type === 'year' || p.type === 'month' || p.type === 'day') out[p.type] = Number(p.value);
  return toIsoDate(out.year, out.month, out.day);
}

/** Today's date, in the given time zone or the runtime's own. Pass `now` to make it deterministic. */
export function todayIso(options: { now?: Date; timeZone?: string } = {}): IsoDate {
  return dateToIso(options.now ?? new Date(), options.timeZone);
}

export function addDays(iso: IsoDate, days: number): IsoDate {
  const { year, month, day } = parts(iso);
  return toIsoDate(year, month, day + days);
}

/** Adds months, clamping the day to the target month (Jan 31 + 1 month = Feb 28 or 29). */
export function addMonths(iso: IsoDate, months: number): IsoDate {
  const { year, month, day } = parts(iso);
  const first = toIsoDate(year, month + months, 1);
  const t = parts(first);
  return toIsoDate(t.year, t.month, Math.min(day, daysInMonth(t.year, t.month)));
}

export function addYears(iso: IsoDate, years: number): IsoDate {
  return addMonths(iso, years * 12);
}

/** Negative when `a` is earlier, 0 when equal, positive when later. */
export function compareIso(a: IsoDate, b: IsoDate): number {
  return a < b ? -1 : a > b ? 1 : 0;
}

/** Whole days from `a` to `b` (`b - a`). */
export function daysBetween(a: IsoDate, b: IsoDate): number {
  return Math.round((isoToUtcDate(b).getTime() - isoToUtcDate(a).getTime()) / DAY_MS);
}

/** True when `iso` is within `[min, max]`; either bound may be missing. */
export function isWithin(iso: IsoDate, min?: IsoDate | null, max?: IsoDate | null): boolean {
  return (!min || iso >= min) && (!max || iso <= max);
}

export function clampIso(iso: IsoDate, min?: IsoDate | null, max?: IsoDate | null): IsoDate {
  if (min && iso < min) return min;
  if (max && iso > max) return max;
  return iso;
}

export function startOfMonth(iso: IsoDate): IsoDate {
  return `${iso.slice(0, 7)}-01`;
}

export function endOfMonth(iso: IsoDate): IsoDate {
  const { year, month } = parts(iso);
  return toIsoDate(year, month, daysInMonth(year, month));
}

export function isSameMonth(a: IsoDate, b: IsoDate): boolean {
  return a.slice(0, 7) === b.slice(0, 7);
}

/** Whole months from the month of `a` to the month of `b`. */
export function monthsBetween(a: IsoDate, b: IsoDate): number {
  const pa = parts(a);
  const pb = parts(b);
  return (pb.year - pa.year) * 12 + (pb.month - pa.month);
}

export function dayOfWeek(iso: IsoDate): Weekday {
  return isoToUtcDate(iso).getUTCDay() as Weekday;
}

/** The first day of the week that contains `iso`. */
export function startOfWeek(iso: IsoDate, weekStartsOn: Weekday): IsoDate {
  return addDays(iso, -((dayOfWeek(iso) - weekStartsOn + 7) % 7));
}

export function endOfWeek(iso: IsoDate, weekStartsOn: Weekday): IsoDate {
  return addDays(startOfWeek(iso, weekStartsOn), 6);
}

/** ISO 8601 week number (weeks start on Monday; week 1 contains the year's first Thursday). */
export function isoWeekNumber(iso: IsoDate): number {
  const thursday = addDays(iso, 3 - ((dayOfWeek(iso) + 6) % 7));
  const jan1 = `${thursday.slice(0, 4)}-01-01`;
  return Math.floor(daysBetween(jan1, thursday) / 7) + 1;
}

/**
 * The weeks shown for a month: arrays of seven ISO dates, starting on
 * `weekStartsOn`, including the leading and trailing days of the neighbouring
 * months. With `fixedWeeks` there are always six rows, so the grid never
 * changes height as the month changes.
 */
export function monthGrid(iso: IsoDate, weekStartsOn: Weekday, fixedWeeks = false): IsoDate[][] {
  const first = startOfMonth(iso);
  const last = endOfMonth(iso);
  const weeks: IsoDate[][] = [];
  let cursor = startOfWeek(first, weekStartsOn);
  while (cursor <= last || (fixedWeeks && weeks.length < 6)) {
    const week: IsoDate[] = [];
    for (let i = 0; i < 7; i += 1) {
      week.push(cursor);
      cursor = addDays(cursor, 1);
    }
    weeks.push(week);
  }
  return weeks;
}

export function minIso(a: IsoDate, b: IsoDate): IsoDate {
  return a <= b ? a : b;
}

export function maxIso(a: IsoDate, b: IsoDate): IsoDate {
  return a >= b ? a : b;
}

/** `{ date, minutes }` from `YYYY-MM-DDTHH:mm` (seconds are ignored), or `null`. */
export function parseIsoDateTime(value: string): { date: IsoDate; minutes: number } | null {
  const m = /^(\d{4}-\d{2}-\d{2})T(\d{2}):(\d{2})(?::\d{2}(?:\.\d+)?)?$/.exec(value);
  if (!m || !isIsoDate(m[1])) return null;
  const h = Number(m[2]);
  const min = Number(m[3]);
  if (h > 23 || min > 59) return null;
  return { date: m[1], minutes: h * 60 + min };
}

/** `YYYY-MM-DDTHH:mm` from a date and minutes after midnight (0-1439). */
export function toIsoDateTime(date: IsoDate, minutes: number): IsoDateTime {
  return `${date}T${minutesToIsoTime(minutes)}`;
}

/** `HH:mm` from minutes after midnight. */
export function minutesToIsoTime(minutes: number): string {
  const m = ((Math.round(minutes) % 1440) + 1440) % 1440;
  return `${pad2(Math.floor(m / 60))}:${pad2(m % 60)}`;
}
