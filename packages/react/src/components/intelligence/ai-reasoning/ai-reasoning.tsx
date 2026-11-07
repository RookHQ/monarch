'use client';

import { forwardRef, useEffect, useRef, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph, BotGlyph } from '../../../internal/icons.js';
import type { AiReasoningPart } from '../ai-conversation/index.js';
import { AiStreamingText } from '../ai-streaming-text/index.js';

/** Every visible and assistive-technology string. English defaults. */
export interface AiReasoningMessages {
  /** Visually hidden prefix that identifies the region as the assistant’s reasoning. */
  prefix: string;
  /** Summary while the model is reasoning. */
  thinking: string;
  /** Summary once done, with the formatted duration (see `duration`). */
  thoughtFor: (duration: string) => string;
  /** Summary once done when no duration is known. */
  thought: string;
  /** Summary of reasoning whose text is withheld (`redacted`). */
  reasoningHidden: string;
  /**
   * Formats a duration in milliseconds, e.g. `12s` or `1m 5s`. The default
   * uses `Intl.NumberFormat` unit formatting in `locale`.
   */
  duration: (ms: number, locale?: string) => string;
}

function unit(value: number, u: 'second' | 'minute', locale?: string): string {
  try {
    return new Intl.NumberFormat(locale, {
      style: 'unit',
      unit: u,
      unitDisplay: 'narrow',
    }).format(value);
  } catch {
    return `${value}${u === 'second' ? 's' : 'm'}`;
  }
}

/**
 * Formats a reasoning duration: under a minute as seconds (`12s`), otherwise
 * minutes and seconds (`1m 5s`), localised with `Intl.NumberFormat`.
 */
export function formatReasoningDuration(ms: number, locale?: string): string {
  const total = Math.max(1, Math.round(ms / 1000));
  if (total < 60) return unit(total, 'second', locale);
  const m = Math.floor(total / 60);
  const s = total % 60;
  return s
    ? `${unit(m, 'minute', locale)} ${unit(s, 'second', locale)}`
    : unit(m, 'minute', locale);
}

export const AI_REASONING_MESSAGES: AiReasoningMessages = {
  prefix: 'Assistant reasoning:',
  thinking: 'Thinking…',
  thoughtFor: (d) => `Thought for ${d}`,
  thought: 'Thought',
  reasoningHidden: 'Reasoning hidden',
  duration: formatReasoningDuration,
};

export interface AiReasoningProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'part'
> {
  /** The reasoning part from the message model. */
  part: AiReasoningPart;
  /**
   * Whether finished reasoning starts open (history, a debugging view).
   * Streaming reasoning is always open and collapses when it finishes, unless
   * the person opened or closed it themselves meanwhile. Default false.
   */
  defaultOpen?: boolean | undefined;
  /** Called when the disclosure opens or closes, by the person or automatically. */
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** BCP 47 locale for the duration. */
  locale?: string | undefined;
  /** Replaces any of the English strings. */
  messages?: Partial<AiReasoningMessages> | undefined;
}

/**
 * The model’s reasoning for one answer, as a disclosure. While it streams the
 * summary reads "Thinking…" and the text is shown live; when it finishes it
 * collapses to "Thought for 12s". Redacted reasoning shows "Reasoning hidden"
 * and no body. Plugs into `AiMessageView` through `renderPart`.
 */
export const AiReasoning = forwardRef<HTMLDivElement, AiReasoningProps>(function AiReasoning(
  { part, defaultOpen = false, onOpenChange, locale, messages: messagesProp, className, ...rest },
  ref,
) {
  const m = { ...AI_REASONING_MESSAGES, ...messagesProp };
  const streaming = part.state === 'streaming';
  const [open, setOpen] = useState(streaming || defaultOpen);
  /** The person toggled it while it streamed: do not auto-collapse. */
  const touched = useRef(false);
  const startedAt = useRef<number | null>(null);
  const [measuredMs, setMeasuredMs] = useState<number | undefined>(undefined);
  const wasStreaming = useRef(streaming);

  useEffect(() => {
    if (streaming) {
      if (startedAt.current === null) startedAt.current = Date.now();
      if (!wasStreaming.current) {
        // Reasoning restarted (a regeneration): show it live again.
        touched.current = false;
        setOpen(true);
        onOpenChange?.(true);
      }
    } else if (wasStreaming.current) {
      if (startedAt.current !== null) setMeasuredMs(Date.now() - startedAt.current);
      startedAt.current = null;
      if (!touched.current) {
        setOpen(false);
        onOpenChange?.(false);
      }
    }
    wasStreaming.current = streaming;
    // onOpenChange is an event callback; re-running on its identity would re-collapse.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [streaming]);

  const durationMs = part.durationMs ?? measuredMs;
  const label = streaming
    ? m.thinking
    : part.redacted
      ? m.reasoningHidden
      : durationMs !== undefined
        ? m.thoughtFor(m.duration(durationMs, locale))
        : m.thought;

  const hasBody = !part.redacted && part.text.length > 0;

  if (!hasBody) {
    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-reasoning', className)}
        data-state={part.state}
        data-redacted={part.redacted || undefined}
      >
        <span className="mn-ai-reasoning__summary" data-static="">
          <BotGlyph className="mn-ai-reasoning__icon" />
          <span className="mn-visually-hidden">{m.prefix} </span>
          <span className="mn-ai-reasoning__label">{label}</span>
        </span>
      </div>
    );
  }

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-ai-reasoning', className)}
      data-state={part.state}
      data-open={open || undefined}
    >
      <details
        className="mn-ai-reasoning__details"
        open={open}
        onToggle={(e) => {
          // Also fires after React sets `open` itself; only a change counts.
          const next = e.currentTarget.open;
          if (next === open) return;
          if (streaming) touched.current = true;
          setOpen(next);
          onOpenChange?.(next);
        }}
      >
        <summary className="mn-ai-reasoning__summary">
          <ChevronRightGlyph className="mn-ai-reasoning__chevron" />
          <span className="mn-visually-hidden">{m.prefix} </span>
          <span className="mn-ai-reasoning__label">{label}</span>
        </summary>
        <AiStreamingText
          className="mn-ai-reasoning__body"
          text={part.text}
          streaming={streaming}
          announce={false}
        />
      </details>
    </div>
  );
});
