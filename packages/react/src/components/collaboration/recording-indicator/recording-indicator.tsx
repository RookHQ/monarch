'use client';

import { forwardRef, useEffect, useRef, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { formatCallDuration } from '../call-stage/index.js';

/** Every string a recording indicator shows or announces. */
export interface RecordingIndicatorMessages {
  /** The short visible badge text. Hidden from screen readers, which hear `recording` instead. */
  rec: string;
  /** Spoken name of the badge while recording. */
  recording: string;
  /** Visible and spoken text while paused. */
  paused: string;
  /** Announced when recording starts. */
  started: string;
  /** Announced when recording stops. */
  stopped: string;
  /** Announced when recording is paused. */
  pausedAnnouncement: string;
  /** Announced when recording resumes. */
  resumed: string;
  /** Name of the stop button. */
  stop: string;
}

export const RECORDING_INDICATOR_MESSAGES: RecordingIndicatorMessages = {
  rec: 'REC',
  recording: 'Recording',
  paused: 'Paused',
  started: 'Recording started',
  stopped: 'Recording stopped',
  pausedAnnouncement: 'Recording paused',
  resumed: 'Recording resumed',
  stop: 'Stop recording',
};

const toMs = (value: string | number | Date) =>
  value instanceof Date ? value.getTime() : typeof value === 'number' ? value : Date.parse(value);

export interface RecordingIndicatorProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Whether recording is on. When false only the (empty) live region is rendered. */
  recording: boolean;
  /** Recording is paused: the badge says so and the time stops. */
  paused?: boolean | undefined;
  /** When recording started (ISO string, ms since epoch or Date); the time ticks from it. */
  since?: string | number | Date | undefined;
  /**
   * Recorded time in milliseconds, when the product tracks it itself (for
   * example excluding pauses). Takes precedence over `since` and does not tick.
   */
  elapsed?: number | undefined;
  /** Show the recorded time next to the badge. Default true when `since` or `elapsed` is given. */
  showElapsed?: boolean | undefined;
  /** Stops recording. Adds a stop button. */
  onStop?: (() => void) | undefined;
  /** `badge` (default) on light surfaces; `overlay` on video, with a dark translucent fill. */
  variant?: 'badge' | 'overlay' | undefined;
  /** Translated strings. */
  messages?: Partial<RecordingIndicatorMessages> | undefined;
}

/**
 * A “REC” badge with the recorded time, shown while a call or session is
 * recorded. Starting, pausing, resuming and stopping are announced once to
 * screen readers; the ticking time is not.
 */
export const RecordingIndicator = forwardRef<HTMLDivElement, RecordingIndicatorProps>(
  function RecordingIndicator(
    {
      recording,
      paused = false,
      since,
      elapsed,
      showElapsed,
      onStop,
      variant = 'badge',
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...RECORDING_INDICATOR_MESSAGES, ...messagesProp };
    const [now, setNow] = useState<number | null>(null);
    const [announcement, setAnnouncement] = useState('');
    const previous = useRef<{ recording: boolean; paused: boolean } | null>(null);
    const start = since === undefined ? NaN : toMs(since);
    const ticking = recording && !paused && elapsed === undefined && Number.isFinite(start);

    useEffect(() => {
      setNow(Date.now());
      if (!ticking) return;
      const timer = window.setInterval(() => setNow(Date.now()), 1000);
      return () => window.clearInterval(timer);
    }, [ticking]);

    // Announce changes, not the state on first render.
    useEffect(() => {
      const prev = previous.current;
      previous.current = { recording, paused };
      if (!prev) return;
      if (recording !== prev.recording) setAnnouncement(recording ? m.started : m.stopped);
      else if (recording && paused !== prev.paused)
        setAnnouncement(paused ? m.pausedAnnouncement : m.resumed);
    }, [recording, paused, m.started, m.stopped, m.pausedAnnouncement, m.resumed]);

    const ms = elapsed ?? (now === null || !Number.isFinite(start) ? 0 : Math.max(0, now - start));
    const withTime = showElapsed ?? (elapsed !== undefined || since !== undefined);

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-recording-indicator', className)}
        data-recording={recording || undefined}
        data-paused={(recording && paused) || undefined}
        data-variant={variant}
      >
        {recording ? (
          <span className="mn-recording-indicator__badge">
            <span className="mn-recording-indicator__dot" aria-hidden />
            <span className="mn-recording-indicator__label" aria-hidden>
              {paused ? m.paused : m.rec}
            </span>
            <span className="mn-visually-hidden">
              {paused ? `${m.recording}, ${m.paused}` : m.recording}
            </span>
            {withTime ? (
              <time
                className="mn-recording-indicator__time"
                dateTime={`PT${Math.floor(ms / 1000)}S`}
              >
                {formatCallDuration(ms)}
              </time>
            ) : null}
          </span>
        ) : null}
        {recording && onStop ? (
          <button
            type="button"
            className="mn-recording-indicator__stop"
            aria-label={m.stop}
            onClick={onStop}
          >
            <span className="mn-recording-indicator__stop-glyph" aria-hidden />
          </button>
        ) : null}
        <span className="mn-visually-hidden" role="status">
          {announcement}
        </span>
      </div>
    );
  },
);
