/**
 * Time-zone helpers for instants (`2026-11-04T09:30:00Z`), built on `Intl`
 * only. Pure and SSR-safe. Calendar dates stay ISO `YYYY-MM-DD` strings.
 */
import { dateToIso, type HourCycle, type IsoDate } from '../../../internal/dates/index.js';

const cache = new Map<string, Intl.DateTimeFormat | null>();

function formatter(key: string, make: () => Intl.DateTimeFormat): Intl.DateTimeFormat | null {
  if (!cache.has(key)) {
    try {
      cache.set(key, make());
    } catch {
      // An unknown zone id: callers fall back to UTC.
      cache.set(key, null);
    }
  }
  return cache.get(key) ?? null;
}

/** A valid zone id, or `'UTC'` when the runtime does not know it. */
export function safeZone(zone: string): string {
  return formatter(`z|${zone}`, () => new Intl.DateTimeFormat('en-US', { timeZone: zone }))
    ? zone
    : 'UTC';
}

/** The calendar date an instant falls on in `zone`. */
export function instantDay(instant: string, zone: string): IsoDate {
  return dateToIso(new Date(instant), safeZone(zone));
}

/** The same instant, whatever offset each string was written with. */
export function sameInstant(a: string, b: string): boolean {
  return new Date(a).getTime() === new Date(b).getTime();
}

/** Wall-clock time of an instant in `zone`: `'9:30 AM'` or `'09:30'`. */
export function formatInstantTime(
  instant: string,
  zone: string,
  locale?: string,
  hourCycle?: HourCycle,
): string {
  const z = safeZone(zone);
  const options: Intl.DateTimeFormatOptions = { timeZone: z, hour: 'numeric', minute: '2-digit' };
  if (hourCycle) {
    options.hourCycle = hourCycle === '12' ? 'h12' : 'h23';
    if (hourCycle === '24') options.hour = '2-digit';
  }
  const f = formatter(
    `t|${locale ?? ''}|${z}|${hourCycle ?? ''}`,
    () => new Intl.DateTimeFormat(locale, options),
  );
  return f ? f.format(new Date(instant)) : instant;
}

/** A readable zone name for an instant: `'India Standard Time'`, or the id when Intl has none. */
export function zoneName(zone: string, at: Date, locale?: string): string {
  const z = safeZone(zone);
  const f = formatter(
    `n|${locale ?? ''}|${z}`,
    () => new Intl.DateTimeFormat(locale, { timeZone: z, timeZoneName: 'long' }),
  );
  const name = f?.formatToParts(at).find((p) => p.type === 'timeZoneName')?.value;
  return name ? `${name} (${zone})` : zone;
}
