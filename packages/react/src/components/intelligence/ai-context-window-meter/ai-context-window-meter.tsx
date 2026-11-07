import { forwardRef, useId, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { ErrorGlyph, WarningGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import {
  Progress,
  type ProgressSegment,
  type ProgressSegmentTone,
} from '../../data-display/progress-bar/index.js';

/** One part of what fills the context window, e.g. "System prompt" or "Attachments". */
export interface AiContextWindowPart {
  label: string;
  tokens: number;
}

/** How full the context window is. */
export type AiContextWindowState = 'ok' | 'warning' | 'full';

/** Every string AiContextWindowMeter shows or announces. English defaults. */
export interface AiContextWindowMeterMessages {
  /** Heading and accessible name of the bar. */
  label: string;
  /** Formats a token count, e.g. "45,000". */
  formatNumber: (value: number) => string;
  /** Formats a 0–1 share, e.g. "35%". */
  formatPercent: (value: number) => string;
  /** The summary line and the bar's value text. */
  amount: (used: string, limit: string, percent: string) => string;
  /** Accessible name of the legend list. */
  legend: string;
  /** Legend entry for tokens not covered by `breakdown`. */
  other: string;
  /** A legend entry's amount, e.g. "12,000 tokens". */
  tokens: (formatted: string) => string;
  /** Shown at or above `warnAt`. */
  warning: string;
  /** Shown when the window is full. */
  full: string;
  /** The summarize button. */
  summarize: string;
}

const DEFAULT_MESSAGES: AiContextWindowMeterMessages = {
  label: 'Context window',
  formatNumber: (value) => new Intl.NumberFormat(undefined).format(value),
  formatPercent: (value) =>
    new Intl.NumberFormat(undefined, { style: 'percent', maximumFractionDigits: 0 }).format(value),
  amount: (used, limit, percent) => `${used} of ${limit} tokens (${percent})`,
  legend: 'What fills the context window',
  other: 'Other',
  tokens: (formatted) => `${formatted} tokens`,
  warning: 'The conversation is nearly at its context limit. Earlier messages may be left out.',
  full: 'The context window is full. Earlier messages are being left out.',
  summarize: 'Summarize earlier messages',
};

const TONES: ProgressSegmentTone[] = ['data-1', 'data-2', 'data-3', 'data-4', 'data-5'];

/** `full` at or over the limit, `warning` at or above `warnAt` (0–1), otherwise `ok`. */
export function aiContextWindowState(
  used: number,
  limit: number,
  warnAt = 0.8,
): AiContextWindowState {
  if (!(limit > 0)) return 'ok';
  const share = Math.max(0, used) / limit;
  if (share >= 1) return 'full';
  if (share >= warnAt) return 'warning';
  return 'ok';
}

export interface AiContextWindowMeterProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children'
> {
  /** Tokens currently in the context window. */
  used: number;
  /** The model's context window size, in tokens. */
  limit: number;
  /**
   * What the tokens are made of, drawn as coloured segments with a legend.
   * Tokens in `used` that no part covers are shown as "Other".
   */
  breakdown?: readonly AiContextWindowPart[] | undefined;
  /** Shows "Summarize earlier messages", which calls this. */
  onSummarize?: (() => void) | undefined;
  /** A summary is being made: the button shows a spinner and ignores clicks, keeping focus. */
  summarizing?: boolean | undefined;
  /** Share of the limit (0–1) at which the meter warns. Default `0.8`. */
  warnAt?: number | undefined;
  /** Hide the heading and summary line, for a toolbar. The bar keeps its name. Default `false`. */
  compact?: boolean | undefined;
  /** Strings and number formatting, for localisation. */
  messages?: Partial<AiContextWindowMeterMessages> | undefined;
}

/**
 * How full the model's context window is: a segmented bar of what fills it
 * (instructions, files, earlier messages) with a legend, a warning as it
 * nears the limit, and an optional "Summarize earlier messages" action.
 */
export const AiContextWindowMeter = forwardRef<HTMLDivElement, AiContextWindowMeterProps>(
  function AiContextWindowMeter(
    {
      used,
      limit,
      breakdown,
      onSummarize,
      summarizing = false,
      warnAt = 0.8,
      compact = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const noticeId = useId();
    const total = Math.max(0, used);
    const max = limit > 0 ? limit : 0;
    const state = aiContextWindowState(total, max, warnAt);
    const percent = m.formatPercent(max > 0 ? Math.min(1, total / max) : 0);
    const amount = m.amount(m.formatNumber(total), m.formatNumber(max), percent);

    let segments: (ProgressSegment & { tone: ProgressSegmentTone })[] | undefined;
    if (breakdown && breakdown.length > 0) {
      segments = breakdown.map((part, i) => ({
        label: part.label,
        value: Math.max(0, part.tokens),
        tone: TONES[i % TONES.length] ?? 'data-1',
      }));
      const covered = segments.reduce((sum, s) => sum + s.value, 0);
      if (total > covered)
        segments.push({ label: m.other, value: total - covered, tone: 'neutral' });
    }
    const notice = state === 'full' ? m.full : state === 'warning' ? m.warning : undefined;

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-context-window-meter', className)}
        data-state={state}
        data-compact={compact || undefined}
      >
        {compact ? null : (
          <div className="mn-ai-context-window-meter__header">
            <span className="mn-ai-context-window-meter__label">{m.label}</span>
            <span className="mn-ai-context-window-meter__amount">{amount}</span>
          </div>
        )}
        <Progress
          semantics="meter"
          label={m.label}
          value={total}
          max={max}
          {...(segments
            ? { segments }
            : { tone: state === 'full' ? 'danger' : state === 'warning' ? 'warning' : 'neutral' })}
          valueText={amount}
          aria-describedby={notice ? noticeId : undefined}
          className="mn-ai-context-window-meter__bar"
        />
        {segments && !compact ? (
          <ul className="mn-ai-context-window-meter__legend" aria-label={m.legend}>
            {segments.map((s, i) => (
              <li
                // Legend entries follow the segment order.
                key={i}
                className="mn-ai-context-window-meter__item"
              >
                <span
                  className="mn-ai-context-window-meter__swatch"
                  data-tone={s.tone}
                  aria-hidden
                />
                <span className="mn-ai-context-window-meter__item-label">{s.label}</span>
                <span className="mn-ai-context-window-meter__item-value">
                  {m.tokens(m.formatNumber(s.value))}
                </span>
              </li>
            ))}
          </ul>
        ) : null}
        <div role="status" className="mn-ai-context-window-meter__status">
          {notice ? (
            <p id={noticeId} className="mn-ai-context-window-meter__notice">
              <span className="mn-ai-context-window-meter__icon" aria-hidden>
                {state === 'full' ? <ErrorGlyph /> : <WarningGlyph />}
              </span>
              {notice}
            </p>
          ) : null}
        </div>
        {onSummarize ? (
          <div className="mn-ai-context-window-meter__actions">
            <Button
              size="sm"
              variant={state === 'ok' ? 'ghost' : 'secondary'}
              loading={summarizing}
              onClick={onSummarize}
            >
              {m.summarize}
            </Button>
          </div>
        ) : null}
      </div>
    );
  },
);
