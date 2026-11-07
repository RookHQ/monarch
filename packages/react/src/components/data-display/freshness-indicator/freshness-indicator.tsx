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
import { RetryGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { RelativeTime, type DateInput } from '../relative-time/index.js';

export type FreshnessStatus = 'fresh' | 'stale' | 'expired' | 'never' | 'checking' | 'error';

export interface FreshnessIndicatorMessages {
  /** Before the time: "Updated 3 minutes ago". */
  updated: string;
  never: string;
  checking: string;
  /** Status word shown after the time when the data is stale. */
  stale: string;
  /** Status word shown after the time when the data is expired. */
  expired: string;
  /** Shown when the status could not be read and there is no `error` text. */
  error: string;
  refresh: string;
  refreshing: string;
  /** Announced after a refresh finishes. */
  refreshed: string;
  /** Announced when `onRefresh` rejects. */
  refreshFailed: string;
}

export const defaultFreshnessIndicatorMessages: FreshnessIndicatorMessages = {
  updated: 'Updated',
  never: 'Never updated',
  checking: 'Checking…',
  stale: 'Out of date',
  expired: 'Very out of date',
  error: 'Status unavailable',
  refresh: 'Refresh',
  refreshing: 'Refreshing…',
  refreshed: 'Refreshed.',
  refreshFailed: 'Refresh failed.',
};

/** Works out the status from the age of `updatedAt`. */
export function freshnessStatus(
  updatedAt: DateInput | null | undefined,
  options: { staleAfter?: number; expiredAfter?: number; now?: DateInput } = {},
): Extract<FreshnessStatus, 'fresh' | 'stale' | 'expired' | 'never'> {
  if (updatedAt === null || updatedAt === undefined || updatedAt === '') return 'never';
  const when = (updatedAt instanceof Date ? updatedAt : new Date(updatedAt)).getTime();
  if (Number.isNaN(when)) return 'never';
  const nowInput = options.now ?? Date.now();
  const now = (nowInput instanceof Date ? nowInput : new Date(nowInput)).getTime();
  const age = now - when;
  if (options.expiredAfter !== undefined && age > options.expiredAfter) return 'expired';
  if (options.staleAfter !== undefined && age > options.staleAfter) return 'stale';
  return 'fresh';
}

export interface FreshnessIndicatorProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** When the data was last updated or synced. `null` means never. */
  updatedAt: DateInput | null | undefined;
  /** Age in ms after which the data counts as stale (warning). Omit to never be stale. */
  staleAfter?: number | undefined;
  /** Age in ms after which the data counts as expired (danger). */
  expiredAfter?: number | undefined;
  /**
   * Overrides the computed status, e.g. `checking` while the status itself is
   * loading or `error` when it could not be read.
   */
  status?: FreshnessStatus | undefined;
  /** Names the data or source: "Directory", "Registry". Shown first. */
  source?: ReactNode;
  /** Extra facts appended with middle dots: "Events on", a failures link. */
  details?: ReactNode[];
  /** Error text for the `error` status, or the last error of a sync. */
  error?: ReactNode;
  /**
   * Adds a Refresh button. A returned promise keeps the button busy until it
   * settles and the outcome is announced; a rejection is announced as
   * `refreshFailed` (show the reason yourself, e.g. in a toast).
   */
  onRefresh?: (() => unknown) | undefined;
  /** Busy state for the Refresh button when the product tracks it. */
  refreshing?: boolean;
  /** Extra controls at the end, e.g. a "Manage" link. */
  actions?: ReactNode;
  /** `inline` (default) is a single line of muted text; `banner` is a bordered bar. */
  variant?: 'inline' | 'banner';
  /** Reference time for the status and the relative text. */
  now?: DateInput | undefined;
  locale?: string | undefined;
  /** Re-render the relative time as it ages. Default true. */
  live?: boolean;
  messages?: Partial<FreshnessIndicatorMessages>;
}

/**
 * How fresh a data set or sync is: "Updated 3 minutes ago", turning to
 * warning and danger as it ages, with an optional Refresh button.
 */
export const FreshnessIndicator = forwardRef<HTMLDivElement, FreshnessIndicatorProps>(
  function FreshnessIndicator(
    {
      updatedAt,
      staleAfter,
      expiredAfter,
      status: statusProp,
      source,
      details,
      error,
      onRefresh,
      refreshing: refreshingProp,
      actions,
      variant = 'inline',
      now,
      locale,
      live = true,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultFreshnessIndicatorMessages, ...messages };
    const [pending, setPending] = useState(false);
    const [announcement, setAnnouncement] = useState('');
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    const computed = freshnessStatus(updatedAt, {
      ...(staleAfter !== undefined ? { staleAfter } : {}),
      ...(expiredAfter !== undefined ? { expiredAfter } : {}),
      ...(now !== undefined ? { now } : {}),
    });
    const status = statusProp ?? computed;
    const refreshing = refreshingProp ?? pending;

    // Announce status changes after the first render (never on mount).
    const prevStatus = useRef(status);
    useEffect(() => {
      if (prevStatus.current === status) return;
      prevStatus.current = status;
      if (status === 'stale') setAnnouncement(m.stale);
      else if (status === 'expired') setAnnouncement(m.expired);
      else if (status === 'error') setAnnouncement(typeof error === 'string' ? error : m.error);
    }, [status, error, m.stale, m.expired, m.error]);

    const refresh = () => {
      if (!onRefresh || refreshing) return;
      const result = onRefresh();
      if (result && typeof (result as Promise<unknown>).then === 'function') {
        setPending(true);
        setAnnouncement('');
        (result as Promise<unknown>).then(
          () => {
            if (!mounted.current) return;
            setPending(false);
            setAnnouncement(m.refreshed);
          },
          () => {
            if (!mounted.current) return;
            setPending(false);
            setAnnouncement(m.refreshFailed);
          },
        );
      }
    };

    const hasTime = status !== 'never' && computed !== 'never' && status !== 'checking';
    const tone =
      status === 'fresh'
        ? 'success'
        : status === 'stale'
          ? 'warning'
          : status === 'expired' || status === 'error'
            ? 'danger'
            : status === 'checking'
              ? 'info'
              : 'neutral';

    const parts: ReactNode[] = [];
    if (status === 'checking') parts.push(m.checking);
    else if (status === 'error' && !hasTime) parts.push(error ?? m.error);
    else if (!hasTime) parts.push(m.never);
    else {
      parts.push(
        <>
          {m.updated}{' '}
          <RelativeTime
            date={updatedAt as DateInput}
            live={live}
            {...(now !== undefined ? { now } : {})}
            {...(locale !== undefined ? { locale } : {})}
          />
        </>,
      );
      if (status === 'stale')
        parts.push(<strong className="mn-freshness-indicator__flag">{m.stale}</strong>);
      if (status === 'expired')
        parts.push(<strong className="mn-freshness-indicator__flag">{m.expired}</strong>);
      if (status === 'error')
        parts.push(<span className="mn-freshness-indicator__error">{error ?? m.error}</span>);
    }
    if (details) parts.push(...details);

    return (
      <div
        ref={ref}
        {...rest}
        data-status={status}
        data-tone={tone}
        data-variant={variant}
        className={cx('mn-freshness-indicator', className)}
      >
        <p className="mn-freshness-indicator__text">
          <span className="mn-freshness-indicator__dot" aria-hidden="true" />
          {source ? (
            <>
              <span className="mn-freshness-indicator__source">{source}</span>
              <span aria-hidden="true">{' · '}</span>
            </>
          ) : null}
          {parts.map((part, i) => (
            <span key={i} className="mn-freshness-indicator__part">
              {i > 0 ? <span aria-hidden="true">{' · '}</span> : null}
              {i > 0 ? <span className="mn-visually-hidden">, </span> : null}
              {part}
            </span>
          ))}
        </p>
        {onRefresh || actions ? (
          <div className="mn-freshness-indicator__actions">
            {onRefresh ? (
              <Button
                size="xs"
                variant="ghost"
                loading={refreshing}
                leadingIcon={refreshing ? undefined : <RetryGlyph />}
                onClick={refresh}
              >
                {refreshing ? m.refreshing : m.refresh}
              </Button>
            ) : null}
            {actions}
          </div>
        ) : null}
        <span role="status" aria-live="polite" aria-atomic="true" className="mn-visually-hidden">
          {announcement}
        </span>
      </div>
    );
  },
);
