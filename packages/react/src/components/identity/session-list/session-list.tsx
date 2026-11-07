'use client';

import {
  forwardRef,
  useCallback,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { Badge } from '../../data-display/badge/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { ConfirmDialog } from '../../overlays/confirm-dialog/index.js';
import { SectionCard } from '../../layout/section-card/index.js';

/** The kind of device a session runs on; picks the icon. */
export type SessionDeviceKind = 'desktop' | 'mobile' | 'tablet' | 'unknown';

/** One signed-in session (a browser or app on a device). */
export interface SessionListItem {
  /** Passed to `onSignOut`. */
  id: string;
  /** What the person recognises, e.g. "Chrome on macOS". `describeUserAgent()` builds it from a user-agent string. */
  label: string;
  /** Picks the icon. Default `desktop`. */
  kind?: SessionDeviceKind | undefined;
  /** A quieter line, e.g. an approximate location or IP address. */
  detail?: ReactNode;
  /** Shown on hover over the label, e.g. the full user-agent string. */
  hint?: string | undefined;
  /** When this session signed in. */
  signedInAt?: DateInput | undefined;
  /** When it was last used. Not shown for the current session (it is in use now). */
  lastActiveAt?: DateInput | undefined;
  /** This is the session the person is using now. */
  current?: boolean | undefined;
}

/** Visible strings, for translation. */
export interface SessionListMessages {
  /** Card title. Default "Where you’re signed in". */
  title: string;
  /** Line under the title. */
  description: string;
  /** Accessible name of the list. Default "Signed-in sessions". */
  listLabel: string;
  /** Badge on the current session. Default "This device". */
  current: string;
  /** Before the sign-in time. Default "Signed in". */
  signedIn: string;
  /** Before the last-active time. Default "Last active". */
  lastActive: string;
  /** Row button. Default "Sign out". */
  signOut: string;
  /** Accessible name of a row button. Default `Sign out ${label}`. */
  signOutLabel: (label: string) => string;
  /** Header button. Default "Sign out other devices". */
  signOutOthers: string;
  /** Confirmation title for one session. */
  confirmTitle: (label: string, current: boolean) => string;
  /** Confirmation description for one session. */
  confirmDescription: (current: boolean) => string;
  /** Confirmation title for every other session. Default "Sign out other devices?". */
  othersTitle: string;
  /** Confirmation description for every other session. */
  othersDescription: (count: number) => string;
  /** Default "Cancel". */
  cancel: string;
  /** Announced after one session is signed out. */
  signedOut: (label: string) => string;
  /** Announced after every other session is signed out. */
  signedOutOthers: (count: number) => string;
  /** Announced while loading. Default "Loading sessions". */
  loading: string;
  /** Empty state title. Default "No active sessions". */
  emptyTitle: string;
  /** Error state title. Default "Couldn’t load your sessions". */
  errorTitle: string;
}

const plural = (n: number, one: string, many: string) => (n === 1 ? one : many);

export const defaultSessionListMessages: SessionListMessages = {
  title: 'Where you’re signed in',
  description: 'Devices and browsers signed in to your account. Sign out any you don’t recognise.',
  listLabel: 'Signed-in sessions',
  current: 'This device',
  signedIn: 'Signed in',
  lastActive: 'Last active',
  signOut: 'Sign out',
  signOutLabel: (label) => `Sign out ${label}`,
  signOutOthers: 'Sign out other devices',
  confirmTitle: (label, current) => (current ? 'Sign out of this device?' : `Sign out ${label}?`),
  confirmDescription: (current) =>
    current
      ? 'You’ll need to sign in again on this device.'
      : 'That device will need to sign in again.',
  othersTitle: 'Sign out other devices?',
  othersDescription: (n) =>
    `${n} ${plural(n, 'other device', 'other devices')} will be signed out and need to sign in again. This device stays signed in.`,
  cancel: 'Cancel',
  signedOut: (label) => `Signed out ${label}`,
  signedOutOthers: (n) => `Signed out ${n} ${plural(n, 'other device', 'other devices')}`,
  loading: 'Loading sessions',
  emptyTitle: 'No active sessions',
  errorTitle: 'Couldn’t load your sessions',
};

export interface SessionListProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'title' | 'children'
> {
  /** The person's sessions, current one included. */
  sessions: SessionListItem[];
  /** `loading`, `error` (with `onRetry`) or `ready` (default). An empty `sessions` list shows the empty state. */
  state?: 'ready' | 'loading' | 'error' | undefined;
  /** Overrides the card title. */
  title?: ReactNode;
  /** Overrides the line under the title. */
  description?: ReactNode;
  /** Heading element of the title. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | 'h4' | 'h5' | 'h6' | undefined;
  /** Retries loading in the `error` state. */
  onRetry?: (() => void) | undefined;
  /** The retry is in flight. */
  retrying?: boolean | undefined;
  /**
   * Signs one session out. Return a promise to keep the confirmation pending;
   * a rejection shows its message in the dialog. Without it rows have no button.
   */
  onSignOut?: ((id: string) => unknown) | undefined;
  /**
   * Signs out every session except the current one, with their ids. Shows the
   * "Sign out other devices" button when there are other sessions.
   */
  onSignOutOthers?: ((ids: string[]) => unknown) | undefined;
  /**
   * Ask before signing out. Default `true`. Set `false` when the product opens
   * its own confirmation (for example RevokeAccessFlow) from the callbacks.
   */
  confirm?: boolean | undefined;
  /** A note under the list, e.g. how long open apps stay signed in. */
  footnote?: ReactNode;
  /** Reference time for relative times, for identical server and client output. */
  now?: DateInput | undefined;
  /** Locale for relative times. */
  locale?: string | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<SessionListMessages> | undefined;
}

type Pending = { kind: 'one'; session: SessionListItem } | { kind: 'others' } | null;

/**
 * Where the person is signed in: one row per session with the device, when it
 * signed in and was last active, a "This device" badge, and sign-out for one
 * session or for every other session, each behind a confirmation.
 */
export const SessionList = forwardRef<HTMLDivElement, SessionListProps>(function SessionList(
  {
    sessions,
    state = 'ready',
    title,
    description,
    headingLevel = 'h2',
    onRetry,
    retrying = false,
    onSignOut,
    onSignOutOthers,
    confirm = true,
    footnote,
    now,
    locale,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultSessionListMessages, ...messages };
  const [pending, setPending] = useState<Pending>(null);
  const [announcement, setAnnouncement] = useState('');
  const opener = useRef<HTMLElement | null>(null);
  const rootRef = useRef<HTMLDivElement | null>(null);
  const others = sessions.filter((s) => !s.current);

  const setRefs = useCallback(
    (node: HTMLDivElement | null) => {
      rootRef.current = node;
      if (typeof ref === 'function') ref(node);
      else if (ref) ref.current = node;
    },
    [ref],
  );

  const run = async (target: Exclude<Pending, null>) => {
    if (target.kind === 'one') {
      await onSignOut?.(target.session.id);
      setAnnouncement(m.signedOut(target.session.label));
    } else {
      const ids = others.map((s) => s.id);
      await onSignOutOthers?.(ids);
      setAnnouncement(m.signedOutOthers(ids.length));
    }
  };

  const start = (target: Exclude<Pending, null>, button: HTMLElement) => {
    opener.current = button;
    setAnnouncement('');
    if (confirm) setPending(target);
    else void Promise.resolve(run(target)).catch(() => undefined);
  };

  const cardState = state === 'ready' && sessions.length === 0 ? 'empty' : state;

  return (
    <div
      ref={setRefs}
      tabIndex={-1}
      {...rest}
      className={cx('mn-session-list', className)}
      data-state={cardState}
    >
      <SectionCard
        className="mn-session-list__card"
        headingLevel={headingLevel}
        retrying={retrying}
        {...(onRetry ? { onRetry } : {})}
        title={title ?? m.title}
        description={description ?? m.description}
        state={cardState}
        loadingLabel={m.loading}
        emptyTitle={m.emptyTitle}
        errorTitle={m.errorTitle}
        actions={
          onSignOutOthers && others.length > 0 && cardState === 'ready' ? (
            <Button
              size="sm"
              className="mn-session-list__others"
              onClick={(event) => start({ kind: 'others' }, event.currentTarget)}
            >
              {m.signOutOthers}
            </Button>
          ) : undefined
        }
        footer={footnote ? <p className="mn-session-list__footnote">{footnote}</p> : undefined}
      >
        <ul className="mn-session-list__list" aria-label={m.listLabel}>
          {sessions.map((session) => (
            <li
              key={session.id}
              className="mn-session-list__item"
              data-current={session.current || undefined}
            >
              <span className="mn-session-list__icon" aria-hidden="true">
                <DeviceGlyph kind={session.kind ?? 'desktop'} />
              </span>
              <div className="mn-session-list__main">
                <div className="mn-session-list__name-row">
                  <span className="mn-session-list__name" title={session.hint}>
                    {session.label}
                  </span>
                  {session.current ? (
                    <Badge tone="success" size="sm">
                      {m.current}
                    </Badge>
                  ) : null}
                </div>
                {session.detail ? (
                  <span className="mn-session-list__detail">{session.detail}</span>
                ) : null}
                {session.signedInAt !== undefined ||
                (!session.current && session.lastActiveAt !== undefined) ? (
                  <span className="mn-session-list__times">
                    {session.signedInAt !== undefined ? (
                      <span>
                        {m.signedIn}{' '}
                        <RelativeTime
                          date={session.signedInAt}
                          tooltip={false}
                          {...(now !== undefined ? { now } : {})}
                          {...(locale ? { locale } : {})}
                        />
                      </span>
                    ) : null}
                    {!session.current && session.lastActiveAt !== undefined ? (
                      <span>
                        {m.lastActive}{' '}
                        <RelativeTime
                          date={session.lastActiveAt}
                          tooltip={false}
                          {...(now !== undefined ? { now } : {})}
                          {...(locale ? { locale } : {})}
                        />
                      </span>
                    ) : null}
                  </span>
                ) : null}
              </div>
              {onSignOut ? (
                <Button
                  size="sm"
                  variant="ghost"
                  className="mn-session-list__action"
                  aria-label={m.signOutLabel(session.label)}
                  onClick={(event) => start({ kind: 'one', session }, event.currentTarget)}
                >
                  {m.signOut}
                </Button>
              ) : null}
            </li>
          ))}
        </ul>
      </SectionCard>
      <p className="mn-session-list__status" role="status" aria-live="polite">
        {announcement}
      </p>
      {confirm ? (
        <ConfirmDialog
          open={pending !== null}
          onOpenChange={(open) => {
            if (!open) setPending(null);
          }}
          tone="warning"
          title={
            pending?.kind === 'one'
              ? m.confirmTitle(pending.session.label, pending.session.current === true)
              : m.othersTitle
          }
          description={
            pending?.kind === 'one'
              ? m.confirmDescription(pending.session.current === true)
              : m.othersDescription(others.length)
          }
          confirmLabel={pending?.kind === 'others' ? m.signOutOthers : m.signOut}
          cancelLabel={m.cancel}
          onConfirm={() => (pending ? run(pending) : undefined)}
          onCloseAutoFocus={(event) => {
            event.preventDefault();
            const next = rootRef.current?.querySelector<HTMLElement>('.mn-session-list__action');
            const back = opener.current?.isConnected ? opener.current : (next ?? rootRef.current);
            back?.focus();
          }}
        />
      ) : null}
    </div>
  );
});

/** What `describeUserAgent` can tell from a user-agent string. */
export interface UserAgentDescription {
  /** "Chrome on macOS", or whichever half is known, or the `unknown` text. */
  label: string;
  browser: string | null;
  os: string | null;
  /** Phone, tablet or desktop, from the string's own hints. */
  kind: SessionDeviceKind;
}

/**
 * Turns a user-agent string into "Chrome on macOS" and a device kind. It
 * states only what the string says; anything it cannot tell is `null`.
 */
export function describeUserAgent(
  userAgent: string | null | undefined,
  unknown = 'Unknown device',
): UserAgentDescription {
  if (!userAgent) return { label: unknown, browser: null, os: null, kind: 'unknown' };
  const ua = userAgent;
  const os = /iPhone/.test(ua)
    ? 'iPhone'
    : /iPad/.test(ua)
      ? 'iPad'
      : /Android/.test(ua)
        ? 'Android'
        : /CrOS/.test(ua)
          ? 'ChromeOS'
          : /Mac OS X|Macintosh/.test(ua)
            ? 'macOS'
            : /Windows/.test(ua)
              ? 'Windows'
              : /Linux/.test(ua)
                ? 'Linux'
                : null;
  const browser = /Edg\//.test(ua)
    ? 'Edge'
    : /OPR\/|Opera/.test(ua)
      ? 'Opera'
      : /Firefox\/|FxiOS/.test(ua)
        ? 'Firefox'
        : /Chrome\/|CriOS/.test(ua)
          ? 'Chrome'
          : /Safari\//.test(ua)
            ? 'Safari'
            : null;
  const kind: SessionDeviceKind = /iPad|Tablet/.test(ua)
    ? 'tablet'
    : /Mobile|iPhone|Android/.test(ua)
      ? 'mobile'
      : os || browser
        ? 'desktop'
        : 'unknown';
  const label = browser && os ? `${browser} on ${os}` : (browser ?? os ?? unknown);
  return { label, browser, os, kind };
}

function DeviceGlyph({ kind, ...props }: { kind: SessionDeviceKind } & SVGProps<SVGSVGElement>) {
  const common = {
    width: 16,
    height: 16,
    viewBox: '0 0 16 16',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.5,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    ...props,
  };
  if (kind === 'mobile') {
    return (
      <svg {...common}>
        <rect x="4.5" y="1.5" width="7" height="13" rx="1.5" />
        <path d="M7.25 12.25h1.5" />
      </svg>
    );
  }
  if (kind === 'tablet') {
    return (
      <svg {...common}>
        <rect x="2.5" y="1.5" width="11" height="13" rx="1.5" />
        <path d="M7.25 12.25h1.5" />
      </svg>
    );
  }
  return (
    <svg {...common}>
      <rect x="2.5" y="3" width="11" height="7.5" rx="1" />
      <path d="M1 13h14" />
    </svg>
  );
}
