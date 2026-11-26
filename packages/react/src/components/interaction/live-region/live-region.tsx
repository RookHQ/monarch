'use client';

import {
  createContext,
  forwardRef,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';

/** Root class, declared before any other `mn-` string so dist/manifest.json reads it as the root. */
const ROOT = 'mn-live-region';

/** How urgently assistive technology reads a message. */
export type LivePoliteness = 'polite' | 'assertive';

export interface LiveRegionProps extends HTMLAttributes<HTMLDivElement> {
  /**
   * `polite` (default) waits until the screen reader is idle: use it for
   * results, saves and progress. `assertive` interrupts: keep it for errors and
   * for the steps of a drag the user is doing right now.
   */
  politeness?: LivePoliteness | undefined;
  /** Read the whole region on every change, not only the changed part. Default true. */
  atomic?: boolean | undefined;
  /** Hide the region visually (it is still read). Default true; set false to show the text as well. */
  visuallyHidden?: boolean | undefined;
  /** The message. Change it to announce; render the region before the first message. */
  children?: ReactNode;
}

/**
 * A live region: text placed in it is read by screen readers without moving
 * focus. Render it empty first and change its children to announce, or use
 * `useAnnouncer()` to announce from an event handler.
 */
export const LiveRegion = forwardRef<HTMLDivElement, LiveRegionProps>(function LiveRegion(
  { politeness = 'polite', atomic = true, visuallyHidden = true, className, children, ...rest },
  ref,
) {
  return (
    <div
      ref={ref}
      {...rest}
      aria-live={politeness}
      aria-atomic={atomic}
      className={cx(ROOT, visuallyHidden && 'mn-visually-hidden', className)}
      data-politeness={politeness}
    >
      {children}
    </div>
  );
});

/** What `useAnnouncer()` returns. */
export interface Announcer {
  /**
   * Reads `message` aloud. The same text twice in a row is read twice. The
   * default politeness is `polite`.
   */
  announce: (message: string, politeness?: LivePoliteness) => void;
  /** Empties both regions, so a stale message is not found later by browsing. */
  clear: () => void;
}

const AnnouncerContext = createContext<Announcer | null>(null);

const NBSP = String.fromCharCode(160);

/** Same text again gets a trailing no-break space, so screen readers see a change. */
function nextText(previous: string, message: string) {
  return previous === message ? `${message}${NBSP}` : message;
}

/* ------------------------------------------------------------------ */
/* Page-level fallback: two hidden regions appended to <body> on demand. */

const globalRegions: Partial<Record<LivePoliteness, HTMLElement>> = {};

function globalRegion(politeness: LivePoliteness): HTMLElement | null {
  if (typeof document === 'undefined') return null;
  const existing = globalRegions[politeness];
  if (existing && existing.isConnected) return existing;
  const el = document.createElement('div');
  el.className = `${ROOT} mn-visually-hidden`;
  el.setAttribute('aria-live', politeness);
  el.setAttribute('aria-atomic', 'true');
  el.setAttribute('data-politeness', politeness);
  el.setAttribute('data-mn-announcer', 'global');
  document.body.appendChild(el);
  globalRegions[politeness] = el;
  return el;
}

const GLOBAL_CLEAR_AFTER = 7000;
let globalTimer: ReturnType<typeof setTimeout> | null = null;

const globalAnnouncer: Announcer = {
  announce(message, politeness = 'polite') {
    const el = globalRegion(politeness);
    if (!el) return;
    const other = globalRegions[politeness === 'polite' ? 'assertive' : 'polite'];
    if (other) other.textContent = '';
    el.textContent = nextText(el.textContent ?? '', message);
    // Like AnnouncerProvider: old text must not stay in the page for a screen
    // reader to come across later.
    if (globalTimer) clearTimeout(globalTimer);
    globalTimer = setTimeout(() => globalAnnouncer.clear(), GLOBAL_CLEAR_AFTER);
  },
  clear() {
    if (globalTimer) clearTimeout(globalTimer);
    globalTimer = null;
    for (const el of Object.values(globalRegions)) if (el) el.textContent = '';
  },
};

/* ------------------------------------------------------------------ */

export interface AnnouncerProviderProps {
  children?: ReactNode;
  /**
   * Empty the regions this many milliseconds after each message, so old text
   * is not found when browsing. Default 7000; 0 keeps the last message.
   */
  clearAfter?: number | undefined;
}

/**
 * Renders a polite and an assertive live region and gives `useAnnouncer()`
 * below it somewhere to speak. Optional: without a provider, `useAnnouncer()`
 * uses two regions it appends to `<body>`. Put one inside a modal dialog so
 * announcements made while it is open are not hidden by `aria-modal`.
 */
export function AnnouncerProvider({ children, clearAfter = 7000 }: AnnouncerProviderProps) {
  const [polite, setPolite] = useState('');
  const [assertive, setAssertive] = useState('');
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(
    () => () => {
      if (timer.current) clearTimeout(timer.current);
    },
    [],
  );

  const clear = useCallback(() => {
    setPolite('');
    setAssertive('');
  }, []);

  const announce = useCallback(
    (message: string, politeness: LivePoliteness = 'polite') => {
      if (politeness === 'assertive') {
        setPolite('');
        setAssertive((prev) => nextText(prev, message));
      } else {
        setAssertive('');
        setPolite((prev) => nextText(prev, message));
      }
      if (timer.current) clearTimeout(timer.current);
      if (clearAfter > 0) timer.current = setTimeout(clear, clearAfter);
    },
    [clear, clearAfter],
  );

  const value = useMemo(() => ({ announce, clear }), [announce, clear]);

  return (
    <AnnouncerContext.Provider value={value}>
      {children}
      <LiveRegion politeness="polite" data-mn-announcer="">
        {polite}
      </LiveRegion>
      <LiveRegion politeness="assertive" data-mn-announcer="">
        {assertive}
      </LiveRegion>
    </AnnouncerContext.Provider>
  );
}

/**
 * Announce from an event handler or effect: `const { announce } =
 * useAnnouncer(); announce('3 results')`. Uses the nearest `AnnouncerProvider`,
 * or page-level regions on `<body>` when there is none. Safe on the server
 * (announcing there does nothing).
 */
export function useAnnouncer(): Announcer {
  return useContext(AnnouncerContext) ?? globalAnnouncer;
}
