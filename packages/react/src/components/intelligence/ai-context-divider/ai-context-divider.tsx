import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { dayLabel, type DayInput } from '../../../internal/day-groups.js';

/** What the divider marks in a thread. */
export type AiContextDividerKind = 'day' | 'context-reset' | 'summarized';

export interface AiContextDividerMessages {
  today: string;
  yesterday: string;
  /** Label of a context reset. */
  contextReset: string;
  /** Explains a context reset, read with the label. */
  contextResetDescription: string;
  /** Label of a summarized section. */
  summarized: string;
  /** Disclosure label that reveals the summary. */
  showSummary: string;
  /** Visually hidden prefix that identifies the summary as AI-generated. */
  summaryPrefix: string;
  /** Button that loads or shows the original messages. */
  expand: string;
}

export const AI_CONTEXT_DIVIDER_MESSAGES: AiContextDividerMessages = {
  today: 'Today',
  yesterday: 'Yesterday',
  contextReset: 'Context reset',
  contextResetDescription: 'The assistant does not see the messages above this line.',
  summarized: 'Earlier messages were summarized',
  showSummary: 'Show summary',
  summaryPrefix: 'AI-generated summary:',
  expand: 'Show earlier messages',
};

export interface AiContextDividerProps extends HTMLAttributes<HTMLDivElement> {
  /**
   * `day` labels a calendar day ("Today", "Monday, 3 March"); `context-reset`
   * marks where the assistant's memory of the conversation starts again;
   * `summarized` marks earlier messages that were condensed to save context.
   */
  kind: AiContextDividerKind;
  /** The day for `day` dividers, and the moment of a reset or summary (shown as a time element). */
  date?: DayInput | undefined;
  /** For `summarized`: the summary the assistant now works from, behind a disclosure. */
  summary?: ReactNode | undefined;
  /** For `summarized`: shows a button that reveals the original messages. */
  onExpand?: (() => void) | undefined;
  /** BCP 47 locale for the day label. */
  locale?: string | undefined;
  /** IANA time zone for the day label. Default: the viewer's. */
  timeZone?: string | undefined;
  /** Reference time for "Today" and "Yesterday" (tests and stories). */
  now?: DayInput | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiContextDividerMessages> | undefined;
}

function isoOf(date: DayInput | undefined): string | undefined {
  if (date === undefined) return undefined;
  const d = date instanceof Date ? date : new Date(date);
  return Number.isNaN(d.getTime()) ? undefined : d.toISOString();
}

/**
 * A labelled rule between messages in a thread: a calendar day, a context
 * reset ("the assistant does not see the messages above this line") or a
 * point where earlier messages were summarized, with the summary behind a
 * disclosure and an optional way to show the originals.
 */
export const AiContextDivider = forwardRef<HTMLDivElement, AiContextDividerProps>(
  function AiContextDivider(
    {
      kind,
      date,
      summary,
      onExpand,
      locale,
      timeZone,
      now,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...AI_CONTEXT_DIVIDER_MESSAGES, ...messagesProp };
    const dateTime = isoOf(date);
    const label =
      kind === 'day'
        ? date === undefined
          ? ''
          : dayLabel(date, { locale, timeZone, now }, { today: m.today, yesterday: m.yesterday })
        : kind === 'context-reset'
          ? m.contextReset
          : m.summarized;
    const interactive = kind === 'summarized' && (summary !== undefined || onExpand !== undefined);
    const description = kind === 'context-reset' ? m.contextResetDescription : undefined;

    const text =
      kind === 'day' && dateTime ? (
        <time dateTime={dateTime} className="mn-ai-context-divider__label">
          {label}
        </time>
      ) : (
        <span className="mn-ai-context-divider__label">{label}</span>
      );

    const rule = (
      <div className="mn-ai-context-divider__rule">
        <span className="mn-ai-context-divider__line" aria-hidden />
        {text}
        <span className="mn-ai-context-divider__line" aria-hidden />
      </div>
    );

    if (!interactive) {
      return (
        <div
          ref={ref}
          role="separator"
          aria-label={description ? `${label}. ${description}` : label}
          {...rest}
          className={cx('mn-ai-context-divider', className)}
          data-kind={kind}
        >
          {rule}
          {description ? (
            <p className="mn-ai-context-divider__description" aria-hidden>
              {description}
            </p>
          ) : null}
        </div>
      );
    }

    return (
      <div
        ref={ref}
        role="group"
        aria-label={label}
        {...rest}
        className={cx('mn-ai-context-divider', className)}
        data-kind={kind}
      >
        {rule}
        <div className="mn-ai-context-divider__controls">
          {summary !== undefined ? (
            <details className="mn-ai-context-divider__details">
              <summary className="mn-ai-context-divider__summary">{m.showSummary}</summary>
              <div className="mn-ai-context-divider__summary-body">
                <span className="mn-visually-hidden">{m.summaryPrefix} </span>
                {summary}
              </div>
            </details>
          ) : null}
          {onExpand ? (
            <button type="button" className="mn-ai-context-divider__expand" onClick={onExpand}>
              {m.expand}
            </button>
          ) : null}
        </div>
      </div>
    );
  },
);
