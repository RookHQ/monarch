'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ClockGlyph, ErrorGlyph, SuccessGlyph } from '../../../internal/icons.js';

/** Anything `new Date()` accepts: a Date, epoch milliseconds or an ISO 8601 string. */
export type SlaDateInput = Date | number | string;

/**
 * The state of a service-level target.
 * - `on-track`: running, with time to spare.
 * - `at-risk`: running, and the due time is close (see `warnBefore`).
 * - `breached`: the due time has passed without the target being met.
 * - `paused`: the clock is stopped, for example while waiting on the customer.
 * - `met`: the target was achieved in time.
 * - `none`: no target applies.
 */
export type SlaState = 'on-track' | 'at-risk' | 'breached' | 'paused' | 'met' | 'none';

/** Visible and announced strings. Override them to translate. */
export interface SlaIndicatorMessages {
  /** State names, used as the label when there is no countdown and in the accessible text. */
  states: Record<SlaState, string>;
  /** Label while time is left, such as "2h 15m left". */
  remaining: (duration: string) => string;
  /** Label once the due time has passed, such as "Overdue 40m". */
  overdue: (duration: string) => string;
  /** Formats a span of milliseconds (always positive). Default "<1m", "45m", "2h 15m", "3d 4h". */
  duration: (ms: number) => string;
  /** Visually hidden prefix naming the target, such as "First response". */
  target: (label: string) => string;
  /** Visually hidden suffix with the absolute due time. */
  due: (formatted: string) => string;
  /** Visually hidden note when an earlier target on the same item was breached. */
  breachedBefore: string;
  /** Announced (politely) when `announceChanges` is on and the state changes. */
  changed: (state: string, target: string | undefined) => string;
}

/** Formats milliseconds as "<1m", "45m", "2h 15m" or "3d 4h". */
export function formatSlaDuration(ms: number): string {
  const minutes = Math.floor(Math.abs(ms) / 60_000);
  if (minutes < 1) return '<1m';
  const d = Math.floor(minutes / 1440);
  const h = Math.floor((minutes % 1440) / 60);
  const m = minutes % 60;
  if (d > 0) return h ? `${d}d ${h}h` : `${d}d`;
  if (h > 0) return m ? `${h}h ${m}m` : `${h}h`;
  return `${m}m`;
}

export const DEFAULT_SLA_MESSAGES: SlaIndicatorMessages = {
  states: {
    'on-track': 'On track',
    'at-risk': 'At risk',
    breached: 'Breached',
    paused: 'Paused',
    met: 'Met',
    none: 'No SLA',
  },
  remaining: (d) => `${d} left`,
  overdue: (d) => `Overdue ${d}`,
  duration: formatSlaDuration,
  target: (label) => `${label}: `,
  due: (formatted) => `, due ${formatted}`,
  breachedBefore: ', an earlier target was breached',
  changed: (state, target) => (target ? `${target}: ${state}` : `SLA ${state}`),
};

function toMs(input: SlaDateInput | null | undefined): number | null {
  if (input === null || input === undefined) return null;
  const ms = (input instanceof Date ? input : new Date(input)).getTime();
  return Number.isNaN(ms) ? null : ms;
}

/**
 * Derives the state to show from the server's state, the due time and the
 * clock. A running target whose due time has passed is `breached` even if the
 * server has not caught up; one within `warnBefore` of its due time is `at-risk`.
 * `paused`, `met` and `none` are never changed.
 */
export function resolveSlaState(
  state: SlaState,
  dueAt: SlaDateInput | null | undefined,
  now: number,
  warnBefore?: number,
): SlaState {
  if (state !== 'on-track' && state !== 'at-risk') return state;
  const due = toMs(dueAt);
  if (due === null) return state;
  const remaining = due - now;
  if (remaining <= 0) return 'breached';
  if (state === 'on-track' && warnBefore !== undefined && remaining <= warnBefore) return 'at-risk';
  return state;
}

/** Hydration-safe clock: server and first client render agree, then it ticks. */
function useClock(injected: number | null, live: boolean | number): number {
  const [clock, setClock] = useState<number>(() => injected ?? Date.now());
  useEffect(() => {
    if (injected !== null) return;
    setClock(Date.now());
    if (live === false) return;
    const timer = setInterval(() => setClock(Date.now()), live === true ? 30_000 : live);
    return () => clearInterval(timer);
  }, [injected, live]);
  return injected ?? clock;
}

const DEFAULT_DUE_FORMAT: Intl.DateTimeFormatOptions = { dateStyle: 'medium', timeStyle: 'short' };

function PauseGlyph() {
  return (
    <svg
      viewBox="0 0 16 16"
      width="16"
      height="16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      aria-hidden
      focusable={false}
    >
      <circle cx="8" cy="8" r="6.25" />
      <path d="M6.5 5.75v4.5M9.5 5.75v4.5" />
    </svg>
  );
}

const ICONS: Record<SlaState, (() => ReactNode) | null> = {
  'on-track': () => <ClockGlyph aria-hidden />,
  'at-risk': () => <ClockGlyph aria-hidden />,
  breached: () => <ErrorGlyph aria-hidden />,
  paused: () => <PauseGlyph />,
  met: () => <SuccessGlyph aria-hidden />,
  none: null,
};

export interface SlaIndicatorProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  /** The state reported by the server. Running targets are re-derived from `dueAt` and the clock. */
  state: SlaState;
  /** When the target is due. Drives the countdown ("2h left") and the overdue time. */
  dueAt?: SlaDateInput | null | undefined;
  /**
   * Turn an `on-track` target `at-risk` once it is due within this many
   * milliseconds, e.g. `60 * 60_000` for an hour. Default: trust `state`.
   */
  warnBefore?: number | undefined;
  /** Name of the target, such as "First response" or "Resolution". Part of the accessible text. */
  target?: string | undefined;
  /** An earlier target on the same item was breached; said in the accessible text. */
  breachedBefore?: boolean | undefined;
  /**
   * The reference time. Pass it from the server (and the same value on the
   * client) for identical server rendering; it also stops the live clock.
   * Without it the countdown reads the clock after mount.
   */
  now?: SlaDateInput | undefined;
  /** Re-derive the countdown as time passes: `true` every 30 seconds (default), a number of milliseconds, or `false`. */
  live?: boolean | number;
  /** `chip` (default) is a compact pill for lists and headers; `text` is an icon and text for panels. */
  variant?: 'chip' | 'text';
  /** Render the `none` state as "No SLA" instead of a dash. Default `false`. */
  showNone?: boolean;
  /**
   * Announce state changes (such as at risk → breached) in a polite live region.
   * Turn on for one prominent indicator, such as a detail header, not for every row of a list.
   */
  announceChanges?: boolean;
  /** BCP 47 locale for the absolute due time. */
  locale?: string | undefined;
  /** Called when the derived state changes after mount, such as when a countdown runs out. */
  onStateChange?: ((state: SlaState) => void) | undefined;
  /** Override visible and announced strings. */
  messages?: Partial<SlaIndicatorMessages>;
}

/**
 * A service-level target at a glance: on track, at risk, breached, paused or
 * met, with the time left or overdue. Colour is backed by an icon and text,
 * and the countdown keeps itself honest between refetches.
 */
export const SlaIndicator = forwardRef<HTMLSpanElement, SlaIndicatorProps>(function SlaIndicator(
  {
    state: stateProp,
    dueAt,
    warnBefore,
    target,
    breachedBefore = false,
    now: nowProp,
    live = true,
    variant = 'chip',
    showNone = false,
    announceChanges = false,
    locale,
    onStateChange,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_SLA_MESSAGES, ...messagesProp };
  const states = { ...DEFAULT_SLA_MESSAGES.states, ...messagesProp?.states };
  const due = toMs(dueAt);
  const ticking =
    live !== false && due !== null && (stateProp === 'on-track' || stateProp === 'at-risk');
  const now = useClock(toMs(nowProp), ticking ? live : false);
  const state = resolveSlaState(stateProp, dueAt, now, warnBefore);

  const [announcement, setAnnouncement] = useState('');
  const previous = useRef(state);
  const onChange = useRef(onStateChange);
  onChange.current = onStateChange;
  useEffect(() => {
    if (previous.current === state) return;
    previous.current = state;
    onChange.current?.(state);
    if (announceChanges) setAnnouncement(messages.changed(states[state], target));
    // `messages` is rebuilt every render; only a state change should announce.
  }, [state]); // eslint-disable-line react-hooks/exhaustive-deps

  let label: string;
  if (state === 'none') label = showNone ? states.none : '—';
  else if (state === 'breached')
    label =
      due !== null && due < now ? messages.overdue(messages.duration(now - due)) : states.breached;
  else if ((state === 'on-track' || state === 'at-risk') && due !== null)
    label = messages.remaining(messages.duration(due - now));
  else label = states[state];

  const dueText =
    due !== null && state !== 'met' && state !== 'none'
      ? new Intl.DateTimeFormat(locale, DEFAULT_DUE_FORMAT).format(new Date(due))
      : null;
  const showsStateName = label === states[state];
  const icon = ICONS[state];

  return (
    <span
      ref={ref}
      title={dueText ?? undefined}
      {...rest}
      className={cx('mn-sla-indicator', className)}
      data-state={state}
      data-variant={variant}
    >
      {icon ? <span className="mn-sla-indicator__icon">{icon()}</span> : null}
      <span className="mn-sla-indicator__label">
        {target ? <span className="mn-visually-hidden">{messages.target(target)}</span> : null}
        {state === 'none' && !showNone ? (
          <>
            <span aria-hidden>—</span>
            <span className="mn-visually-hidden">{states.none}</span>
          </>
        ) : (
          <span suppressHydrationWarning>{label}</span>
        )}
        {!showsStateName && state !== 'none' ? (
          <span className="mn-visually-hidden"> ({states[state]})</span>
        ) : null}
        {dueText ? (
          <span className="mn-visually-hidden" suppressHydrationWarning>
            {messages.due(dueText)}
          </span>
        ) : null}
        {breachedBefore && state !== 'breached' ? (
          <span className="mn-visually-hidden">{messages.breachedBefore}</span>
        ) : null}
      </span>
      {announceChanges ? (
        <span className="mn-visually-hidden" role="status" aria-live="polite">
          {announcement}
        </span>
      ) : null}
    </span>
  );
});

/** One target in a `SlaTargetList`. */
export interface SlaTarget {
  /** Stable key. */
  id: string;
  /** Name of the target, such as "First response". */
  label: ReactNode;
  /** The server's state for this target. */
  state: SlaState;
  /** When it is or was due. */
  dueAt?: SlaDateInput | null | undefined;
  /** When it was met or closed, shown instead of the countdown for finished targets. */
  completedAt?: SlaDateInput | null | undefined;
}

export interface SlaTargetListProps extends Omit<HTMLAttributes<HTMLUListElement>, 'children'> {
  /** The targets, in the order they apply. */
  targets: SlaTarget[];
  /** Passed to every indicator: see `SlaIndicator`. */
  warnBefore?: number | undefined;
  /** Passed to every indicator: see `SlaIndicator`. */
  now?: SlaDateInput | undefined;
  /** Passed to every indicator: see `SlaIndicator`. */
  live?: boolean | number;
  /** BCP 47 locale for absolute times. */
  locale?: string | undefined;
  /** Shown when `targets` is empty. Default "No SLA targets apply." */
  emptyMessage?: ReactNode;
  /** Override strings of the indicators. */
  messages?: Partial<SlaIndicatorMessages>;
}

/**
 * Every target of one item (first response, next response, resolution) with
 * its state and time left, for a detail side panel.
 */
export const SlaTargetList = forwardRef<HTMLUListElement, SlaTargetListProps>(
  function SlaTargetList(
    {
      targets,
      warnBefore,
      now,
      live = true,
      locale,
      emptyMessage = 'No SLA targets apply.',
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    if (targets.length === 0) {
      return <p className={cx('mn-sla-target-list__empty', className)}>{emptyMessage}</p>;
    }
    const format = new Intl.DateTimeFormat(locale, DEFAULT_DUE_FORMAT);
    return (
      <ul ref={ref} {...rest} className={cx('mn-sla-target-list', className)}>
        {targets.map((t) => {
          const completed = toMs(t.completedAt);
          return (
            <li key={t.id} className="mn-sla-target-list__item" data-state={t.state}>
              <span className="mn-sla-target-list__label">{t.label}</span>
              {completed !== null && (t.state === 'met' || t.state === 'breached') ? (
                <span className="mn-sla-target-list__time">
                  <time dateTime={new Date(completed).toISOString()} suppressHydrationWarning>
                    {format.format(new Date(completed))}
                  </time>
                  <SlaIndicator
                    state={t.state}
                    variant="text"
                    live={false}
                    {...(locale ? { locale } : {})}
                    {...(messages ? { messages } : {})}
                  />
                </span>
              ) : (
                <SlaIndicator
                  state={t.state}
                  dueAt={t.dueAt}
                  variant="text"
                  live={live}
                  showNone
                  {...(warnBefore !== undefined ? { warnBefore } : {})}
                  {...(now !== undefined ? { now } : {})}
                  {...(locale ? { locale } : {})}
                  {...(messages ? { messages } : {})}
                />
              )}
            </li>
          );
        })}
      </ul>
    );
  },
);
