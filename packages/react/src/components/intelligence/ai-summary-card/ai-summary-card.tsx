'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ErrorGlyph, RetryGlyph, WarningGlyph, BotGlyph } from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { Button } from '../../actions/button/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import type { AiSourcePart } from '../ai-conversation/index.js';
import { AiMarkdown } from '../ai-markdown/index.js';

/**
 * Where the summary is in its life. `generating` shows a placeholder (or dims
 * the previous summary); `stale` warns that the data changed since it was
 * written; `error` says it could not be generated.
 */
export type AiSummaryCardStatus = 'ready' | 'generating' | 'stale' | 'error';

export interface AiSummaryCardMessages {
  /** The AI provenance label above the title. */
  aiSummary: string;
  /** Label of the refresh button. */
  refresh: string;
  /** Text before the relative time, e.g. "Generated" + "3 hours ago". */
  generated: string;
  /** Shown and announced politely while a summary is being written. */
  generating: string;
  /** Announced politely when a summary finishes generating. */
  updated: string;
  /** Shown when the summary may be out of date. */
  stale: string;
  /** Shown (assertively) when the summary could not be generated. */
  error: string;
  /** Shown when there is no summary and nothing is generating. */
  empty: string;
  /** Label of the source list. */
  sources: string;
}

export const AI_SUMMARY_CARD_MESSAGES: AiSummaryCardMessages = {
  aiSummary: 'AI summary',
  refresh: 'Refresh',
  generated: 'Generated',
  generating: 'Generating summary…',
  updated: 'Summary updated.',
  stale: 'The data changed since this summary was written. Refresh to update it.',
  error: 'The summary couldn’t be generated.',
  empty: 'No summary yet.',
  sources: 'Sources',
};

export interface AiSummaryCardProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** What is summarised, e.g. "Account summary". Becomes the card's accessible name. */
  title: ReactNode;
  /** The summary as Markdown. Treated as untrusted model output (no raw HTML). */
  summary: string;
  /** When the summary was written. Shown as relative time with the absolute time on focus. */
  generatedAt?: DateInput | undefined;
  /** Reference time for `generatedAt`, for identical server and client rendering. */
  now?: DateInput | undefined;
  /** The documents or records the summary was written from, as a numbered list. */
  sources?: readonly AiSourcePart[] | undefined;
  /** Shows a Refresh button that asks for a new summary. */
  onRefresh?: (() => void) | undefined;
  /** A refresh is in progress: Refresh shows a spinner and the card is busy. */
  refreshing?: boolean | undefined;
  /** Default `ready`. */
  status?: AiSummaryCardStatus | undefined;
  /** Detail shown under the error line when `status` is `error`. */
  error?: ReactNode | undefined;
  /** Heading level of the title. Summary headings start one level below. Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | undefined;
  /** Extra content after the summary, e.g. a "Copy" or "Open in assistant" button. */
  actions?: ReactNode | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiSummaryCardMessages> | undefined;
}

function hostOf(url: string): string | undefined {
  try {
    return new URL(url).host || undefined;
  } catch {
    return undefined;
  }
}

/**
 * A model-written summary of a record or page: a labelled card with the
 * Markdown summary, when it was generated, its sources and a Refresh action,
 * with generating, stale and error states. It is always marked as an AI
 * summary, visibly and for assistive technology.
 */
export const AiSummaryCard = forwardRef<HTMLElement, AiSummaryCardProps>(function AiSummaryCard(
  {
    title,
    summary,
    generatedAt,
    now,
    sources,
    onRefresh,
    refreshing = false,
    status = 'ready',
    error,
    headingLevel = 3,
    actions,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...AI_SUMMARY_CARD_MESSAGES, ...messagesProp };
  const titleId = useId();
  const sourcesId = useId();
  const busy = refreshing || status === 'generating';
  const Heading = `h${headingLevel}` as const;
  const [announcement, setAnnouncement] = useState('');
  const wasBusy = useRef(busy);

  // Announce politely only on changes, never on mount.
  useEffect(() => {
    if (busy && !wasBusy.current) setAnnouncement(messages.generating);
    else if (!busy && wasBusy.current) setAnnouncement(status === 'error' ? '' : messages.updated);
    wasBusy.current = busy;
  }, [busy, status, messages.generating, messages.updated]);

  const hasSummary = summary.trim().length > 0;
  const shownStatus = refreshing && status === 'ready' ? 'generating' : status;

  return (
    <article
      ref={ref}
      aria-labelledby={titleId}
      aria-busy={busy || undefined}
      {...rest}
      className={cx('mn-ai-summary-card', className)}
      data-status={shownStatus}
    >
      <div className="mn-ai-summary-card__header">
        <div className="mn-ai-summary-card__titles">
          <span className="mn-ai-summary-card__eyebrow">
            <BotGlyph />
            {messages.aiSummary}
          </span>
          <Heading id={titleId} className="mn-ai-summary-card__title">
            {title}
          </Heading>
        </div>
        {onRefresh ? (
          <Button
            size="sm"
            variant="ghost"
            leadingIcon={<RetryGlyph />}
            loading={busy}
            aria-describedby={titleId}
            className="mn-ai-summary-card__refresh"
            onClick={onRefresh}
          >
            {messages.refresh}
          </Button>
        ) : null}
      </div>

      {status === 'stale' ? (
        <p className="mn-ai-summary-card__notice" data-kind="stale">
          <WarningGlyph aria-hidden className="mn-ai-summary-card__notice-icon" />
          <span>{messages.stale}</span>
        </p>
      ) : null}
      {status === 'error' ? (
        <div role="alert" className="mn-ai-summary-card__notice" data-kind="error">
          <ErrorGlyph aria-hidden className="mn-ai-summary-card__notice-icon" />
          <div>
            <p className="mn-ai-summary-card__error-title">{messages.error}</p>
            {error ? <div className="mn-ai-summary-card__error-detail">{error}</div> : null}
          </div>
        </div>
      ) : null}

      <div className="mn-ai-summary-card__body">
        {hasSummary ? (
          <AiMarkdown
            className="mn-ai-summary-card__summary"
            text={summary}
            headingLevelOffset={headingLevel}
          />
        ) : busy ? (
          <div className="mn-ai-summary-card__placeholder">
            <p className="mn-ai-summary-card__generating">{messages.generating}</p>
            <Skeleton shape="text" width="100%" />
            <Skeleton shape="text" width="92%" />
            <Skeleton shape="text" width="64%" />
          </div>
        ) : status === 'error' ? null : (
          <p className="mn-ai-summary-card__empty">{messages.empty}</p>
        )}
      </div>

      {sources && sources.length > 0 ? (
        <div className="mn-ai-summary-card__sources">
          <p id={sourcesId} className="mn-ai-summary-card__sources-label">
            {messages.sources}
          </p>
          <ol aria-labelledby={sourcesId} className="mn-ai-summary-card__source-list">
            {sources.map((source) => {
              const safe = source.url && isSafeUrl(source.url) ? source.url : undefined;
              const meta = source.product ?? (safe ? hostOf(safe) : undefined);
              return (
                <li key={source.id} className="mn-ai-summary-card__source" data-kind={source.kind}>
                  {safe ? (
                    <a
                      href={safe}
                      rel="noopener noreferrer nofollow"
                      className="mn-ai-summary-card__source-link"
                    >
                      {source.title}
                    </a>
                  ) : (
                    <span className="mn-ai-summary-card__source-title">{source.title}</span>
                  )}
                  {meta ? <span className="mn-ai-summary-card__source-meta">{meta}</span> : null}
                </li>
              );
            })}
          </ol>
        </div>
      ) : null}

      {generatedAt !== undefined || actions ? (
        <div className="mn-ai-summary-card__footer">
          {generatedAt !== undefined ? (
            <p className="mn-ai-summary-card__meta">
              {messages.generated}{' '}
              <RelativeTime date={generatedAt} {...(now !== undefined ? { now } : {})} />
            </p>
          ) : null}
          {actions ? <div className="mn-ai-summary-card__actions">{actions}</div> : null}
        </div>
      ) : null}

      <span role="status" className="mn-visually-hidden">
        {announcement}
      </span>
    </article>
  );
});
