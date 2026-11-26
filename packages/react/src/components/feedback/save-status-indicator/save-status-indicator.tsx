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
import { CheckGlyph, DotGlyph, WarningGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { Spinner } from '../spinner/index.js';

/**
 * Where saving stands: nothing to report, edits not yet saved, a save in
 * flight, saved, or the last save failed.
 */
export type SaveStatus = 'idle' | 'unsaved' | 'saving' | 'saved' | 'error';

/** Every visible or announced string of SaveStatusIndicator, for translation. */
export interface SaveStatusMessages {
  /** Default "Unsaved changes". */
  unsaved: string;
  /** Default "Saving…". */
  saving: string;
  /** Default "Saved". */
  saved: string;
  /** Default "Couldn't save". */
  error: string;
  /** The retry button. Default "Retry". */
  retry: string;
}

export const defaultSaveStatusMessages: SaveStatusMessages = {
  unsaved: 'Unsaved changes',
  saving: 'Saving…',
  saved: 'Saved',
  error: "Couldn't save",
  retry: 'Retry',
};

export interface SaveStatusIndicatorProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The current state. */
  status: SaveStatus;
  /** When the last successful save finished. Shown as relative time after "Saved" ("Saved · 2 minutes ago"). */
  savedAt?: DateInput | null;
  /**
   * The reference time for the relative text. Pass the same value on the
   * server and the client for identical server rendering.
   */
  now?: DateInput;
  /** BCP 47 locale for the relative time. Default: the runtime locale. */
  locale?: string;
  /** Replaces "Couldn't save" with a specific message, e.g. "You are offline". */
  error?: ReactNode;
  /** Shows a Retry button while `status` is `error`. */
  onRetry?: (() => void) | undefined;
  /**
   * Also announce "Saving…" to screen readers. Default `false`: only the
   * outcome (Saved, or the error) is announced, so frequent autosaves stay quiet.
   */
  announceSaving?: boolean;
  /** Replaces any of the default strings. */
  messages?: Partial<SaveStatusMessages>;
}

/**
 * A small inline status for saving: "Saving…", "Saved · 2 minutes ago" or
 * "Couldn't save · Retry". The outcome is announced politely to screen
 * readers; the visible text stays readable in place. Use it in an editor
 * header, next to a form's actions, or through AutosaveIndicator.
 */
export const SaveStatusIndicator = forwardRef<HTMLDivElement, SaveStatusIndicatorProps>(
  function SaveStatusIndicator(
    {
      status,
      savedAt,
      now,
      locale,
      error,
      onRetry,
      announceSaving = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...defaultSaveStatusMessages, ...messagesProp };
    const errorText = typeof error === 'string' ? error : messages.error;
    // The live region keeps its text until the next outcome; it is cleared
    // first so the same outcome twice ("Saved", "Saved") is announced again.
    const [announcement, setAnnouncement] = useState('');
    const previous = useRef(status);
    useEffect(() => {
      if (previous.current === status) return;
      previous.current = status;
      const next =
        status === 'saved'
          ? messages.saved
          : status === 'error'
            ? errorText
            : status === 'saving' && announceSaving
              ? messages.saving
              : null;
      if (next === null) return;
      setAnnouncement('');
      const frame = setTimeout(() => setAnnouncement(next), 50);
      return () => clearTimeout(frame);
    }, [status, messages.saved, messages.saving, errorText, announceSaving]);

    const icon =
      status === 'saving' ? (
        <Spinner size="sm" />
      ) : status === 'saved' ? (
        <CheckGlyph />
      ) : status === 'error' ? (
        <WarningGlyph />
      ) : status === 'unsaved' ? (
        <DotGlyph />
      ) : null;
    const text =
      status === 'saving'
        ? messages.saving
        : status === 'saved'
          ? messages.saved
          : status === 'unsaved'
            ? messages.unsaved
            : status === 'error'
              ? (error ?? messages.error)
              : null;
    const showTime = status === 'saved' && savedAt !== undefined && savedAt !== null;

    return (
      <div
        ref={ref}
        {...rest}
        data-status={status}
        className={cx('mn-save-status-indicator', className)}
      >
        {icon ? (
          <span className="mn-save-status-indicator__icon" aria-hidden>
            {icon}
          </span>
        ) : null}
        {text !== null ? <span className="mn-save-status-indicator__text">{text}</span> : null}
        {showTime ? (
          <>
            <span className="mn-save-status-indicator__separator" aria-hidden>
              ·
            </span>
            <RelativeTime
              className="mn-save-status-indicator__time"
              date={savedAt}
              live
              tooltip={false}
              {...(now !== undefined ? { now } : {})}
              {...(locale ? { locale } : {})}
            />
          </>
        ) : null}
        {status === 'error' && onRetry ? (
          <Button
            size="xs"
            variant="ghost"
            className="mn-save-status-indicator__retry"
            onClick={onRetry}
          >
            {messages.retry}
          </Button>
        ) : null}
        <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
          {announcement}
        </span>
      </div>
    );
  },
);
