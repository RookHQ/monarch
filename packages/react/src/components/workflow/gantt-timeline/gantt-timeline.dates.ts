/**
 * Calendar-day maths for the timeline. Dates are ISO strings (`2026-01-12`)
 * read as UTC days, so results never depend on the viewer's time zone.
 */

const DAY_MS = 86_400_000;

/** Days since 1970-01-01 for an ISO date, or NaN. */
export function dayNumber(iso: string): number {
  return Math.round(Date.parse(`${iso.slice(0, 10)}T00:00:00Z`) / DAY_MS);
}

/** The ISO date of a day number. */
export function isoOfDay(day: number): string {
  return new Date(day * DAY_MS).toISOString().slice(0, 10);
}

/** `iso` plus `days` (negative to go back). */
export function addDays(iso: string, days: number): string {
  return isoOfDay(dayNumber(iso) + days);
}

/** Whole days from `a` to `b` (`b - a`). */
export function daysBetween(a: string, b: string): number {
  return dayNumber(b) - dayNumber(a);
}

/** 0 = Sunday … 6 = Saturday. */
export function weekday(iso: string): number {
  return new Date(`${iso.slice(0, 10)}T00:00:00Z`).getUTCDay();
}

/** The Monday on or before `iso`. */
export function mondayOf(iso: string): string {
  return addDays(iso, -((weekday(iso) + 6) % 7));
}

/** The first day of the month of `iso`. */
export function monthStart(iso: string): string {
  return `${iso.slice(0, 8)}01`;
}

/** Today in the viewer's time zone, as an ISO date. */
export function localToday(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function isIsoDate(value: unknown): value is string {
  return (
    typeof value === 'string' && /^\d{4}-\d{2}-\d{2}/.test(value) && !Number.isNaN(dayNumber(value))
  );
}
