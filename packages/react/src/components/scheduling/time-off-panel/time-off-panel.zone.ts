/**
 * Wall-clock <-> instant conversion in an IANA zone, built on `Intl` only.
 * Pure and SSR-safe. (TimeGrid, SlotPicker and RescheduleDialog keep similar private helpers;
 * a shared home in `src/internal/dates` would let them share one copy.)
 */
import {
  minutesToIsoTime,
  parseIsoDateTime,
  toIsoDate,
  type HourCycle,
  type IsoDate,
  type IsoDateTime,
} from '../../../internal/dates/index.js';

const cache = new Map<string, Intl.DateTimeFormat | null>();

function partsFormatter(zone: string): Intl.DateTimeFormat | null {
  if (!cache.has(zone)) {
    try {
      cache.set(
        zone,
        new Intl.DateTimeFormat('en-US', {
          timeZone: zone,
          hourCycle: 'h23',
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
        }),
      );
    } catch {
      cache.set(zone, null);
    }
  }
  return cache.get(zone) ?? null;
}

/** The calendar day and wall-clock minutes of an instant in `zone` (UTC for an unknown zone). */
export function zonedParts(instant: Date, zone: string): { day: IsoDate; minutes: number } {
  const f = partsFormatter(zone) ?? partsFormatter('UTC');
  const get: Record<string, number> = {};
  for (const p of f?.formatToParts(instant) ?? [])
    if (p.type !== 'literal') get[p.type] = Number(p.value);
  return {
    day: toIsoDate(get.year ?? 1970, get.month ?? 1, get.day ?? 1),
    minutes: ((get.hour ?? 0) % 24) * 60 + (get.minute ?? 0),
  };
}

function offsetMinutes(zone: string, instant: Date): number {
  const { day, minutes } = zonedParts(instant, zone);
  const [y = 1970, m = 1, d = 1] = day.split('-').map(Number);
  const wall = Date.UTC(y, m - 1, d, Math.floor(minutes / 60), minutes % 60);
  return Math.round((wall - Math.floor(instant.getTime() / 60000) * 60000) / 60000);
}

/** The instant for a wall-clock `YYYY-MM-DDTHH:mm` in `zone`, as an ISO string in UTC. A time in a spring-forward gap moves forward. */
export function wallTimeToInstant(value: IsoDateTime, zone: string): string | null {
  const parsed = parseIsoDateTime(value);
  if (!parsed) return null;
  const [y = 1970, m = 1, d = 1] = parsed.date.split('-').map(Number);
  const guess = Date.UTC(y, m - 1, d, Math.floor(parsed.minutes / 60), parsed.minutes % 60);
  let t = guess - offsetMinutes(zone, new Date(guess)) * 60000;
  t = guess - offsetMinutes(zone, new Date(t)) * 60000;
  return new Date(t).toISOString();
}

/** The wall-clock `YYYY-MM-DDTHH:mm` of an instant in `zone`. */
export function instantToWallTime(instant: string, zone: string): IsoDateTime | null {
  const date = new Date(instant);
  if (Number.isNaN(date.getTime())) return null;
  const { day, minutes } = zonedParts(date, zone);
  return `${day}T${minutesToIsoTime(minutes)}`;
}

/** `'Wed 4 Nov 2026, 09:30'` for an instant in `zone`. */
export function formatInstant(
  instant: string,
  zone: string,
  locale?: string,
  hourCycle?: HourCycle,
): string {
  const date = new Date(instant);
  if (Number.isNaN(date.getTime())) return instant;
  const options: Intl.DateTimeFormatOptions = {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: hourCycle === '24' ? '2-digit' : 'numeric',
    minute: '2-digit',
    timeZoneName: 'short',
  };
  if (hourCycle) options.hourCycle = hourCycle === '12' ? 'h12' : 'h23';
  try {
    return new Intl.DateTimeFormat(locale, { ...options, timeZone: zone }).format(date);
  } catch {
    return new Intl.DateTimeFormat(locale, { ...options, timeZone: 'UTC' }).format(date);
  }
}
