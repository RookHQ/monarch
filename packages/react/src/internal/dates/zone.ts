/**
 * Instants read and written in an IANA time zone, built on `Intl` only.
 *
 * An instant is a real moment (`2026-11-04T09:30:00Z`, a `Date`, epoch ms);
 * a wall-clock time is what a clock in a zone shows (`2026-11-04T15:00` in
 * Asia/Kolkata). These helpers convert between the two, correctly across
 * daylight-saving changes, and never depend on the zone of the machine
 * running them. An unknown zone id falls back to UTC instead of throwing.
 * Pure and SSR-safe.
 */
import type { HourCycle } from './locale.js';
import {
  minutesToIsoTime,
  parseIsoDateTime,
  toIsoDate,
  type IsoDate,
  type IsoDateTime,
} from './iso.js';

/** Anything `new Date()` accepts as an instant. */
export type InstantInput = string | Date | number;

const toDate = (instant: InstantInput) => (instant instanceof Date ? instant : new Date(instant));

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

/** The runtime's own zone, or `'UTC'` when `Intl` cannot say. */
export function defaultZone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  } catch {
    return 'UTC';
  }
}

/** A valid zone id, or `'UTC'` when the runtime does not know it. */
export function safeZone(zone: string): string {
  return formatter(`z|${zone}`, () => new Intl.DateTimeFormat('en-US', { timeZone: zone }))
    ? zone
    : 'UTC';
}

function partsFormatter(zone: string): Intl.DateTimeFormat | null {
  return formatter(
    `p|${zone}`,
    () =>
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
}

/** The calendar day and wall-clock minutes (0-1439) of an instant in `zone` (UTC for an unknown zone). */
export function zonedParts(instant: InstantInput, zone: string): { day: IsoDate; minutes: number } {
  const f = partsFormatter(zone) ?? partsFormatter('UTC');
  const date = toDate(instant);
  const get: Record<string, number> = {};
  // An unreadable instant reads as the epoch instead of throwing in render.
  for (const p of f?.formatToParts(Number.isNaN(date.getTime()) ? new Date(0) : date) ?? [])
    if (p.type !== 'literal') get[p.type] = Number(p.value);
  return {
    day: toIsoDate(get.year ?? 1970, get.month ?? 1, get.day ?? 1),
    minutes: ((get.hour ?? 0) % 24) * 60 + (get.minute ?? 0),
  };
}

/** Offset of `zone` from UTC at `instant`, in minutes (Asia/Kolkata → 330). DST-correct. */
export function zoneOffsetMinutes(zone: string, instant: Date): number {
  const { day, minutes } = zonedParts(instant, zone);
  const [y = 1970, m = 1, d = 1] = day.split('-').map(Number);
  const wall = Date.UTC(y, m - 1, d, Math.floor(minutes / 60), minutes % 60);
  return Math.round((wall - Math.floor(instant.getTime() / 60000) * 60000) / 60000);
}

/**
 * The instant at wall-clock `minutes` after midnight on `day` in `zone`.
 * A time inside a spring-forward gap is pushed forward.
 */
export function zonedTimeToDate(day: IsoDate, minutes: number, zone: string): Date {
  const [y = 1970, m = 1, d = 1] = day.split('-').map(Number);
  const guess = Date.UTC(y, m - 1, d, Math.floor(minutes / 60), minutes % 60);
  const a = guess - zoneOffsetMinutes(zone, new Date(guess)) * 60000;
  const b = guess - zoneOffsetMinutes(zone, new Date(a)) * 60000;
  if (a === b) return new Date(a);
  const reads = (t: number) => {
    const p = zonedParts(t, zone);
    return p.day === day && p.minutes === minutes;
  };
  // Around a clock change the two candidates differ. Whichever one a clock in the
  // zone actually shows as this time wins; in a spring-forward gap neither does,
  // and the later one is the time moved forward (east and west of UTC alike).
  if (reads(b)) return new Date(b);
  if (reads(a)) return new Date(a);
  return new Date(Math.max(a, b));
}

/**
 * The instant for a wall-clock `YYYY-MM-DDTHH:mm` in `zone`, as an ISO string
 * in UTC, or `null` for a malformed value. A time in a spring-forward gap moves forward.
 */
export function wallTimeToInstant(value: IsoDateTime, zone: string): string | null {
  const parsed = parseIsoDateTime(value);
  if (!parsed) return null;
  return zonedTimeToDate(parsed.date, parsed.minutes, zone).toISOString();
}

/** The wall-clock `YYYY-MM-DDTHH:mm` of an instant in `zone`, or `null` for an invalid instant. */
export function instantToWallTime(instant: InstantInput, zone: string): IsoDateTime | null {
  const date = toDate(instant);
  if (Number.isNaN(date.getTime())) return null;
  const { day, minutes } = zonedParts(date, zone);
  return `${day}T${minutesToIsoTime(minutes)}`;
}

/** The calendar date an instant falls on in `zone`. */
export function instantDay(instant: InstantInput, zone: string): IsoDate {
  return zonedParts(instant, zone).day;
}

/** The same instant, whatever offset each string was written with. */
export function sameInstant(a: InstantInput, b: InstantInput): boolean {
  return toDate(a).getTime() === toDate(b).getTime();
}

/** Wall-clock time of an instant in `zone`: `'9:30 AM'` or `'09:30'`. */
export function formatInstantTime(
  instant: InstantInput,
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
  return f ? f.format(toDate(instant)) : String(instant);
}

/** 24-hour clocks pad the hour ("09:00"); 12-hour clocks do not ("9:00 AM"). */
function hourStyle(locale?: string, hourCycle?: 'h12' | 'h23'): '2-digit' | 'numeric' {
  const hc =
    hourCycle ??
    formatter(
      `h|${locale ?? ''}`,
      () => new Intl.DateTimeFormat(locale, { hour: 'numeric' }),
    )?.resolvedOptions().hourCycle;
  return hc === 'h12' || hc === 'h11' ? 'numeric' : '2-digit';
}

/**
 * Wall-clock time of an instant in `zone`, with the hour padded on 24-hour
 * clocks only (`'09:30'`, `'9:30 AM'`). `hourCycle` uses Intl's names.
 */
export function formatZonedTime(
  instant: InstantInput,
  zone: string,
  locale?: string,
  hourCycle?: 'h12' | 'h23',
): string {
  const z = safeZone(zone);
  const f = formatter(
    `zt|${locale ?? ''}|${z}|${hourCycle ?? ''}`,
    () =>
      new Intl.DateTimeFormat(locale, {
        hour: hourStyle(locale, hourCycle),
        minute: '2-digit',
        hourCycle,
        timeZone: z,
      }),
  );
  const date = toDate(instant);
  return f && !Number.isNaN(date.getTime()) ? f.format(date) : String(instant);
}

/** `'Wed 4 Nov 2026, 09:30 GMT+5:30'` for an instant in `zone`; the input back for an invalid instant. */
export function formatInstant(
  instant: InstantInput,
  zone: string,
  locale?: string,
  hourCycle?: HourCycle,
): string {
  const date = toDate(instant);
  if (Number.isNaN(date.getTime())) return String(instant);
  const options: Intl.DateTimeFormatOptions = {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: hourCycle === '24' ? '2-digit' : 'numeric',
    minute: '2-digit',
    timeZoneName: 'short',
    timeZone: safeZone(zone),
  };
  if (hourCycle) options.hourCycle = hourCycle === '12' ? 'h12' : 'h23';
  const f = formatter(
    `i|${locale ?? ''}|${options.timeZone}|${hourCycle ?? ''}`,
    () => new Intl.DateTimeFormat(locale, options),
  );
  return f ? f.format(date) : String(instant);
}

/** A readable zone name at an instant: `'India Standard Time (Asia/Kolkata)'`, or the id when Intl has none. */
export function zoneName(zone: string, at: Date, locale?: string): string {
  const z = safeZone(zone);
  const f = formatter(
    `n|${locale ?? ''}|${z}`,
    () => new Intl.DateTimeFormat(locale, { timeZone: z, timeZoneName: 'long' }),
  );
  const name = f?.formatToParts(at).find((p) => p.type === 'timeZoneName')?.value;
  return name ? `${name} (${zone})` : zone;
}
