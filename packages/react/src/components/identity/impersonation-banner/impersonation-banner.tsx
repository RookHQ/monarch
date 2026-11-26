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
import { isThenable } from '../../../internal/dialog-helpers.js';
import { Button } from '../../actions/button/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';

/** Visible strings, for translation. */
export interface ImpersonationBannerMessages {
  /** The main line. Default `You are viewing as ${name}`. */
  viewingAs: (name: ReactNode) => ReactNode;
  /** Before the start time. Default "Started". */
  started: string;
  /** Before the reason. Default "Reason:". */
  reason: string;
  /** The stop button. Default "Stop". */
  stop: string;
  /** Accessible name of the stop button. Default `Stop viewing as ${name}`. */
  stopLabel: (name: string) => string;
  /** Shown when `onStop` rejects without a message. */
  stopError: string;
}

export const defaultImpersonationBannerMessages: ImpersonationBannerMessages = {
  viewingAs: (name) => <>You are viewing as {name}</>,
  started: 'Started',
  reason: 'Reason:',
  stop: 'Stop',
  stopLabel: (name) => `Stop viewing as ${name}`,
  stopError: 'Couldn’t stop. Try again.',
};

export interface ImpersonationBannerProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Name of the person being viewed as. */
  name: string;
  /** A second identifier, e.g. their email, shown after the name. */
  detail?: ReactNode;
  /** Why the session was started (a ticket or case number). */
  reason?: ReactNode;
  /** When viewing-as began, shown as relative time. */
  startedAt?: DateInput | undefined;
  /** Reference time for the relative start time, for identical server and client output. */
  now?: DateInput | undefined;
  /** Locale for the relative start time. */
  locale?: string | undefined;
  /**
   * Ends the session. Return a promise to show a pending Stop button; a
   * rejection shows its message in the banner.
   */
  onStop: () => unknown;
  /** Controlled pending state, for a stop the product tracks itself. */
  stopping?: boolean | undefined;
  /** Stick to the top of the scrolling container. Default `true`. */
  sticky?: boolean | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<ImpersonationBannerMessages> | undefined;
}

/**
 * A persistent, high-contrast bar shown while a support or admin person is
 * viewing the product as someone else: "You are viewing as Sam · Stop". It
 * cannot be dismissed; the only way out is to stop.
 */
export const ImpersonationBanner = forwardRef<HTMLDivElement, ImpersonationBannerProps>(
  function ImpersonationBanner(
    {
      name,
      detail,
      reason,
      startedAt,
      now,
      locale,
      onStop,
      stopping: stoppingProp,
      sticky = true,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultImpersonationBannerMessages, ...messages };
    const [running, setRunning] = useState(false);
    const [error, setError] = useState<ReactNode>(null);
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);
    const stopping = stoppingProp ?? running;

    const stop = () => {
      if (stopping) return;
      setError(null);
      let result: unknown;
      try {
        result = onStop();
      } catch (failure) {
        setError(failure instanceof Error && failure.message ? failure.message : m.stopError);
        return;
      }
      if (!isThenable(result)) return;
      setRunning(true);
      Promise.resolve(result).then(
        () => {
          if (mounted.current) setRunning(false);
        },
        (failure: unknown) => {
          if (!mounted.current) return;
          setRunning(false);
          setError(failure instanceof Error && failure.message ? failure.message : m.stopError);
        },
      );
    };

    return (
      <div
        ref={ref}
        role="status"
        {...rest}
        className={cx('mn-impersonation-banner', className)}
        data-sticky={sticky || undefined}
        data-stopping={stopping || undefined}
      >
        <span className="mn-impersonation-banner__icon" aria-hidden="true">
          <EyeIcon />
        </span>
        <div className="mn-impersonation-banner__text">
          <p className="mn-impersonation-banner__title">
            {m.viewingAs(<strong className="mn-impersonation-banner__name">{name}</strong>)}
            {detail ? <span className="mn-impersonation-banner__detail"> ({detail})</span> : null}
          </p>
          {reason || startedAt !== undefined ? (
            <p className="mn-impersonation-banner__meta">
              {startedAt !== undefined ? (
                <span>
                  {m.started}{' '}
                  <RelativeTime
                    date={startedAt}
                    tooltip={false}
                    {...(now !== undefined ? { now } : {})}
                    {...(locale ? { locale } : {})}
                  />
                </span>
              ) : null}
              {reason ? (
                <span>
                  {m.reason} {reason}
                </span>
              ) : null}
            </p>
          ) : null}
          {error ? (
            <p className="mn-impersonation-banner__error" role="alert">
              {error}
            </p>
          ) : null}
        </div>
        <Button
          size="sm"
          variant="warning"
          className="mn-impersonation-banner__stop"
          aria-label={m.stopLabel(name)}
          loading={stopping}
          onClick={stop}
        >
          {m.stop}
        </Button>
      </div>
    );
  },
);

function EyeIcon() {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M1.5 8S4 3.5 8 3.5 14.5 8 14.5 8 12 12.5 8 12.5 1.5 8 1.5 8Z" />
      <circle cx="8" cy="8" r="2" />
    </svg>
  );
}
