'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useState,
  useSyncExternalStore,
  type HTMLAttributes,
  type KeyboardEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';

/** Anything `new Date()` accepts: a Date, epoch milliseconds or an ISO 8601 string. */
export type DateInput = Date | number | string;

export interface FormatRelativeTimeOptions {
  /** The reference time. Default: the current time. */
  now?: DateInput;
  /** BCP 47 locale, e.g. `en-GB`. Default: the runtime locale. */
  locale?: string;
  /** `auto` writes "yesterday" and "now"; `always` writes "1 day ago". Default `auto`. */
  numeric?: 'auto' | 'always';
  /** Unit length: "3 minutes ago" (`long`), "3 min. ago" (`short`), "3m ago" (`narrow`). Default `long`. */
  format?: 'long' | 'short' | 'narrow';
}

const UNITS: ReadonlyArray<[Intl.RelativeTimeFormatUnit, number, number]> = [
  // unit, seconds per unit, switch to the next unit at this many of this one
  ['second', 1, 60],
  ['minute', 60, 60],
  ['hour', 3600, 24],
  ['day', 86400, 7],
  ['week', 604800, 4.345],
  ['month', 2629800, 12],
  ['year', 31557600, Infinity],
];

function toDate(input: DateInput): Date {
  return input instanceof Date ? input : new Date(input);
}

/**
 * Picks the largest unit that keeps the value below its rollover, after
 * rounding, so 59.6 minutes reads "1 hour ago" and not "60 minutes ago".
 */
export function relativeTimeParts(
  date: DateInput,
  now: DateInput = Date.now(),
): { value: number; unit: Intl.RelativeTimeFormatUnit } {
  const seconds = (toDate(date).getTime() - toDate(now).getTime()) / 1000;
  for (const [unit, size, rollover] of UNITS) {
    const value = Math.round(seconds / size);
    if (Math.abs(value) < rollover) return { value: value === 0 ? 0 : value, unit };
  }
  return { value: Math.round(seconds / 31557600), unit: 'year' };
}

/** Formats a date relative to `now` with `Intl.RelativeTimeFormat` ("3 hours ago", "in 2 days"). */
export function formatRelativeTime(
  date: DateInput,
  options: FormatRelativeTimeOptions = {},
): string {
  const { now = Date.now(), locale, numeric = 'auto', format = 'long' } = options;
  const { value, unit } = relativeTimeParts(date, now);
  return new Intl.RelativeTimeFormat(locale, { numeric, style: format }).format(value, unit);
}

/** How often to re-render a live timestamp: often while it is fresh, rarely once it is old. */
function tickFor(date: Date, now: number): number {
  const age = Math.abs(now - date.getTime());
  if (age < 60_000) return 5_000;
  if (age < 3_600_000) return 30_000;
  if (age < 86_400_000) return 300_000;
  return 3_600_000;
}

const noopSubscribe = () => () => {};
/** False on the server and during hydration, true afterwards and on client-only mounts. */
function useHydrated(): boolean {
  return useSyncExternalStore(
    noopSubscribe,
    () => true,
    () => false,
  );
}
const DEFAULT_ABSOLUTE: Intl.DateTimeFormatOptions = { dateStyle: 'medium', timeStyle: 'short' };

export interface RelativeTimeProps extends Omit<
  HTMLAttributes<HTMLTimeElement>,
  'children' | 'dateTime'
> {
  /** The moment to describe. A Date, epoch milliseconds or an ISO 8601 string. */
  date: DateInput;
  /**
   * The reference time. Pass it from the server (and the same value on the
   * client) for byte-identical server rendering; without it the text is
   * computed from the clock and corrected right after hydration.
   */
  now?: DateInput;
  /** BCP 47 locale for both the relative and the absolute text. Default: the runtime locale. */
  locale?: string;
  /** IANA time zone for the absolute text, e.g. `Europe/London`. Default: the viewer's zone. */
  timeZone?: string;
  /** `auto` writes "yesterday" and "now"; `always` writes "1 day ago". Default `auto`. */
  numeric?: 'auto' | 'always';
  /** Unit length: `long` "3 minutes ago", `short` "3 min. ago", `narrow` "3m ago". Default `long`. */
  format?: 'long' | 'short' | 'narrow';
  /** `Intl.DateTimeFormat` options for the absolute time. Default `{ dateStyle: 'medium', timeStyle: 'short' }`. */
  absoluteFormat?: Intl.DateTimeFormatOptions;
  /**
   * Show the absolute date instead of relative text once the moment is more
   * than this many milliseconds away, e.g. `7 * 86_400_000` for "a week".
   */
  absoluteAfter?: number;
  /**
   * Re-render as time passes: `true` picks an interval from the age (5 s for
   * the first minute, then 30 s, 5 min and 1 h); a number is a fixed interval
   * in milliseconds. Default `false`.
   */
  live?: boolean | number;
  /**
   * Show the absolute time in a tooltip on hover and keyboard focus. Makes the
   * timestamp a tab stop. Default `true`; set `false` in dense lists where
   * the absolute time is shown elsewhere.
   */
  tooltip?: boolean;
}

/**
 * A `<time datetime>` with localised relative text ("3 hours ago") and the
 * absolute time on hover and focus.
 */
export const RelativeTime = forwardRef<HTMLTimeElement, RelativeTimeProps>(function RelativeTime(
  {
    date,
    now: nowProp,
    locale,
    timeZone,
    numeric = 'auto',
    format = 'long',
    absoluteFormat = DEFAULT_ABSOLUTE,
    absoluteAfter,
    live = false,
    tooltip = true,
    className,
    onKeyDown,
    onBlur,
    ...rest
  },
  ref,
) {
  const tipId = useId();
  const injected = nowProp === undefined ? undefined : toDate(nowProp).getTime();
  // The clock is read once for the first render, so server and client agree
  // when `now` is injected; otherwise the effect below corrects it after mount.
  const [clock, setClock] = useState<number>(() => injected ?? Date.now());
  const [dismissed, setDismissed] = useState(false);
  const hydrated = useHydrated();
  const now = injected ?? clock;
  const when = toDate(date);
  const valid = !Number.isNaN(when.getTime());

  useEffect(() => {
    if (injected === undefined) setClock(Date.now());
  }, [injected, hydrated]);

  useEffect(() => {
    if (!live || injected !== undefined || !valid) return;
    const delay = typeof live === 'number' ? live : tickFor(when, Date.now());
    const timer = setTimeout(() => setClock(Date.now()), delay);
    return () => clearTimeout(timer);
    // Re-arms after every tick because `clock` changes.
  }, [live, injected, valid, when.getTime(), clock]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!valid) {
    return (
      <time ref={ref} {...rest} className={cx('mn-relative-time', className)} data-invalid="">
        —
      </time>
    );
  }

  const absolute = new Intl.DateTimeFormat(locale, {
    ...absoluteFormat,
    ...(timeZone ? { timeZone } : {}),
  }).format(when);
  const useAbsolute = absoluteAfter !== undefined && Math.abs(now - when.getTime()) > absoluteAfter;
  const text = useAbsolute
    ? absolute
    : formatRelativeTime(when, { now, numeric, format, ...(locale ? { locale } : {}) });
  const showTip = tooltip && !useAbsolute;

  return (
    <span
      // Server text depends on the server's clock, locale and zone. Hydration keeps
      // it (suppressHydrationWarning), then this key swaps in the client's text.
      key={hydrated ? 'client' : 'server'}
      className={cx('mn-relative-time', className)}
      data-dismissed={dismissed || undefined}
    >
      {/* The APG tooltip pattern: a focusable, non-interactive trigger whose only key is Escape. */}
      {/* eslint-disable jsx-a11y/no-noninteractive-element-interactions, jsx-a11y/no-noninteractive-tabindex */}
      <time
        ref={ref}
        {...rest}
        className="mn-relative-time__text"
        dateTime={when.toISOString()}
        tabIndex={showTip ? 0 : rest.tabIndex}
        aria-describedby={showTip ? tipId : rest['aria-describedby']}
        suppressHydrationWarning
        onKeyDown={(event: KeyboardEvent<HTMLTimeElement>) => {
          if (event.key === 'Escape' && showTip) setDismissed(true);
          onKeyDown?.(event);
        }}
        onBlur={(event) => {
          setDismissed(false);
          onBlur?.(event);
        }}
        onPointerLeave={() => setDismissed(false)}
      >
        {text}
      </time>
      {/* eslint-enable jsx-a11y/no-noninteractive-element-interactions, jsx-a11y/no-noninteractive-tabindex */}
      {showTip ? (
        <span id={tipId} role="tooltip" className="mn-relative-time__tip" suppressHydrationWarning>
          {absolute}
        </span>
      ) : null}
    </span>
  );
});
