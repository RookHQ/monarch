'use client';

import { forwardRef, useEffect, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { BotGlyph } from '../../../internal/icons.js';

export interface AiThinkingIndicatorMessages {
  /** The label shown and announced while the assistant has not answered yet. */
  thinking: string;
  /** Formats the elapsed time shown after the label, e.g. `5s` or `1m 5s`. */
  elapsed: (seconds: number) => string;
}

const DEFAULT_MESSAGES: AiThinkingIndicatorMessages = {
  thinking: 'Thinking…',
  elapsed: (seconds) => {
    if (seconds < 60) return `${seconds}s`;
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return s ? `${m}m ${s}s` : `${m}m`;
  },
};

export type AiThinkingIndicatorVariant = 'shimmer' | 'mark';

export interface AiThinkingIndicatorProps extends HTMLAttributes<HTMLDivElement> {
  /**
   * `shimmer` (default) sweeps a soft band across the label; `mark` shows a
   * gently pulsing assistant (bot) mark before a still label. Both become
   * static under reduced motion and in forced colours.
   */
  variant?: AiThinkingIndicatorVariant;
  /**
   * Replaces "Thinking…", e.g. "Searching your files…". Announced politely
   * once when the indicator appears and again only if it changes.
   */
  label?: string;
  /**
   * When the assistant started working, as a `Date`, epoch milliseconds or an
   * ISO 8601 string. Defaults to the moment the indicator mounted, so pass it
   * when the indicator can remount mid-turn (a virtualised list, say).
   */
  startedAt?: Date | number | string;
  /**
   * Seconds before the elapsed time appears after the label. Default 3. Pass
   * `Infinity` to never show it.
   */
  showElapsedAfter?: number;
  /** Strings, for localisation. */
  messages?: Partial<AiThinkingIndicatorMessages>;
}

function toTime(value: Date | number | string | undefined): number | null {
  if (value === undefined) return null;
  const t =
    value instanceof Date ? value.getTime() : typeof value === 'number' ? value : Date.parse(value);
  return Number.isFinite(t) ? t : null;
}

const STATIC_QUERY = '(prefers-reduced-motion: reduce), (forced-colors: active)';

/**
 * True when motion should stop (reduced motion or forced colours). The CSS
 * media queries already stop the animation; this mirrors it as
 * `data-static` so apps and tests can see it. False on the server.
 */
function useStaticMotion() {
  const [isStatic, setStatic] = useState(false);
  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return;
    const mq = window.matchMedia(STATIC_QUERY);
    const update = () => setStatic(mq.matches);
    update();
    mq.addEventListener?.('change', update);
    return () => mq.removeEventListener?.('change', update);
  }, []);
  return isStatic;
}

/**
 * Shows that the assistant is working before its first token arrives: a
 * "Thinking…" label with a soft shimmer (or a pulsing bot mark) and, after a
 * few seconds, how long it has been thinking.
 */
export const AiThinkingIndicator = forwardRef<HTMLDivElement, AiThinkingIndicatorProps>(
  function AiThinkingIndicator(
    {
      variant = 'shimmer',
      label,
      startedAt,
      showElapsedAfter = 3,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const text = label ?? messages.thinking;
    const isStatic = useStaticMotion();
    const startTime = toTime(startedAt);
    const [elapsed, setElapsed] = useState(0);
    // Filled after mount: a live region that already holds text when it is
    // inserted is not reliably announced.
    const [announced, setAnnounced] = useState(false);

    useEffect(() => {
      setAnnounced(true);
    }, []);

    useEffect(() => {
      const start = startTime ?? Date.now();
      const tick = () => setElapsed(Math.max(0, Math.floor((Date.now() - start) / 1000)));
      tick();
      const id = setInterval(tick, 1000);
      return () => clearInterval(id);
    }, [startTime]);

    const showElapsed = elapsed >= showElapsedAfter;

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-thinking-indicator', className)}
        data-variant={variant}
        data-static={isStatic || undefined}
        data-elapsed={showElapsed || undefined}
      >
        {variant === 'mark' ? (
          <span className="mn-ai-thinking-indicator__mark" aria-hidden>
            <BotGlyph />
          </span>
        ) : null}
        <span className="mn-ai-thinking-indicator__label" aria-hidden>
          {text}
        </span>
        <span role="status" className="mn-visually-hidden">
          {announced ? text : ''}
        </span>
        {showElapsed ? (
          <span className="mn-ai-thinking-indicator__elapsed">{messages.elapsed(elapsed)}</span>
        ) : null}
      </div>
    );
  },
);
