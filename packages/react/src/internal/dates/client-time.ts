/**
 * Hydration-safe reads of the viewer's clock, time zone and locale.
 *
 * The server's zone, locale and clock are not the viewer's. Reading them in
 * render makes the server HTML and the hydrating client disagree. These hooks
 * render a stable value on the server and during hydration (an explicit prop,
 * or UTC / `SERVER_LOCALE` / no clock), then switch to the viewer's values
 * right after mount, the same way `useToday` and `RelativeTime` do.
 */
import { useEffect, useRef, useState, useSyncExternalStore } from 'react';
import { defaultZone } from './zone.js';

/** Anything `new Date()` accepts. */
export type ClockInput = Date | number | string;

/** The locale the server and the hydrating client both use when none is given. */
export const SERVER_LOCALE = 'en-US';

const noopSubscribe = () => () => {};

/** False on the server and during hydration; true afterwards and on client-only mounts. */
export function useHydrated(): boolean {
  return useSyncExternalStore(
    noopSubscribe,
    () => true,
    () => false,
  );
}

const serverZone = () => 'UTC';

/**
 * `zone` when given; otherwise UTC on the server and during hydration, then
 * the viewer's own zone.
 */
export function useClientZone(zone?: string | null): string {
  const live = useSyncExternalStore(noopSubscribe, defaultZone, serverZone);
  return zone || live;
}

function runtimeLocale(): string {
  try {
    return new Intl.DateTimeFormat().resolvedOptions().locale || SERVER_LOCALE;
  } catch {
    return SERVER_LOCALE;
  }
}
const serverLocale = () => SERVER_LOCALE;

/**
 * `locale` when given; otherwise `SERVER_LOCALE` on the server and during
 * hydration, then the runtime's own locale.
 */
export function useClientLocale(locale?: string | null): string {
  const live = useSyncExternalStore(noopSubscribe, runtimeLocale, serverLocale);
  return locale || live;
}

const toMs = (input: ClockInput): number =>
  input instanceof Date ? input.getTime() : typeof input === 'number' ? input : Date.parse(input);

/** How often to re-render: a fixed interval, or one picked from the current time. */
export type NowTick = number | ((now: number) => number) | false;

/**
 * The current time in epoch ms, kept current by `tick`. An injected `now`
 * wins and never ticks. Without one, the server and the hydrating client get
 * `null` (render something clock-free, such as the absolute date in UTC),
 * and the real clock right after mount.
 */
export function useNow(injected?: ClockInput | null, tick: NowTick = false): number | null {
  const fixed = injected === undefined || injected === null ? undefined : toMs(injected);
  const hydrated = useHydrated();
  const [clock, setClock] = useState<number | null>(null);
  const tickRef = useRef(tick);
  tickRef.current = tick;
  const ticking = tick !== false;

  useEffect(() => {
    if (fixed === undefined && hydrated) setClock(Date.now());
  }, [fixed, hydrated]);

  useEffect(() => {
    const current = tickRef.current;
    if (fixed !== undefined || clock === null || current === false) return;
    const delay = typeof current === 'number' ? current : current(clock);
    const timer = setTimeout(() => setClock(Date.now()), Math.max(1000, delay));
    return () => clearTimeout(timer);
  }, [fixed, clock, ticking]);

  if (fixed !== undefined) return fixed;
  if (!hydrated) return null;
  return clock ?? Date.now();
}

/** A tick that is frequent while `target` is fresh and rare once it is old. */
export function ageTick(target: ClockInput): (now: number) => number {
  const at = toMs(target);
  return (now) => {
    const age = Math.abs(now - at);
    if (age < 60_000) return 5_000;
    if (age < 3_600_000) return 30_000;
    if (age < 86_400_000) return 300_000;
    return 3_600_000;
  };
}

const dateFormats = new Map<string, Intl.DateTimeFormat>();
const numberFormats = new Map<string, Intl.NumberFormat>();

/** A cached `Intl.DateTimeFormat`; an unknown zone falls back to UTC instead of throwing. */
export function dateTimeFormat(
  locale: string | undefined,
  options: Intl.DateTimeFormatOptions,
): Intl.DateTimeFormat {
  const key = `${locale ?? ''}|${JSON.stringify(options)}`;
  let f = dateFormats.get(key);
  if (!f) {
    // Never throw in render: an unknown zone falls back to UTC, and a locale tag the
    // runtime rejects (`en_US`) falls back to the server locale.
    try {
      f = new Intl.DateTimeFormat(locale, options);
    } catch {
      try {
        f = new Intl.DateTimeFormat(locale, { ...options, timeZone: 'UTC' });
      } catch {
        f = new Intl.DateTimeFormat(SERVER_LOCALE, { ...options, timeZone: 'UTC' });
      }
    }
    dateFormats.set(key, f);
  }
  return f;
}

/** A cached `Intl.NumberFormat`. */
export function numberFormat(
  locale: string | undefined,
  options: Intl.NumberFormatOptions = {},
): Intl.NumberFormat {
  const key = `${locale ?? ''}|${JSON.stringify(options)}`;
  let f = numberFormats.get(key);
  if (!f) {
    try {
      f = new Intl.NumberFormat(locale, options);
    } catch {
      // A rejected locale tag, or options the runtime refuses for it.
      try {
        f = new Intl.NumberFormat(SERVER_LOCALE, options);
      } catch {
        f = new Intl.NumberFormat(SERVER_LOCALE);
      }
    }
    numberFormats.set(key, f);
  }
  return f;
}
