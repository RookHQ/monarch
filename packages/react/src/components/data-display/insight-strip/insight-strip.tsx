'use client';
import {
  forwardRef,
  useId,
  useLayoutEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import {
  AiInsightCallout,
  type AiInsightAction,
  type AiInsightCalloutProps,
  type AiInsightOrigin,
  type AiInsightSeverity,
} from '../../intelligence/ai-insight-callout/index.js';

/** One insight in the strip. */
export interface InsightStripItem {
  /** Stable key. */
  id: string;
  /** How much it matters: `critical`, `warning`, `info` or `positive`. */
  severity: AiInsightSeverity;
  /**
   * Where it came from, always shown: `rule` for a fixed rule people wrote
   * ("3 domains expire in 30 days"), `ai` only when a model produced it.
   */
  origin: AiInsightOrigin;
  /** The insight in one line. */
  title: ReactNode;
  /** Why it was raised, in plain words. */
  why: ReactNode;
  /** The figures or records behind it, shown in a collapsed disclosure. */
  evidence?: ReactNode | undefined;
  /** One follow-up action, e.g. "Review renewals". */
  action?: AiInsightAction | undefined;
}

/** The words InsightStrip shows. Override them to translate. */
export interface InsightStripMessages {
  /** The toggle that reveals the hidden insights. Receives how many are hidden. */
  showMore: (hidden: number) => string;
  showLess: string;
  /** Shown when there are no insights. */
  empty: string;
  /** Spoken name of the loading placeholder. */
  loading: string;
}

export const INSIGHT_STRIP_MESSAGES: InsightStripMessages = {
  showMore: (hidden) => `Show ${hidden} more`,
  showLess: 'Show fewer',
  empty: 'Nothing needs attention right now.',
  loading: 'Loading insights',
};

const SEVERITY_ORDER: Record<AiInsightSeverity, number> = {
  critical: 0,
  warning: 1,
  info: 2,
  positive: 3,
};

/** Sorts insights most severe first, keeping the given order within a severity. */
export function sortInsights<T extends Pick<InsightStripItem, 'severity'>>(
  insights: readonly T[],
): T[] {
  return insights
    .map((insight, index) => ({ insight, index }))
    .sort(
      (a, b) =>
        SEVERITY_ORDER[a.insight.severity] - SEVERITY_ORDER[b.insight.severity] ||
        a.index - b.index,
    )
    .map(({ insight }) => insight);
}

export interface InsightStripProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The insights. Rule-based and AI insights can be mixed; each shows its origin. */
  insights: readonly InsightStripItem[];
  /** A heading for the strip, e.g. "Needs attention". With it the strip is a named region. */
  title?: ReactNode;
  /** Level of `title`. Each insight's title is one level below. Default `2`. */
  headingLevel?: 2 | 3 | 4 | 5 | undefined;
  /** `grid` (default) wraps into one to three columns by width; `stack` is one column; `scroll` is one sideways-scrolling row. */
  layout?: 'grid' | 'stack' | 'scroll' | undefined;
  /** Show at most this many, with a "Show N more" toggle for the rest. Default: all. */
  max?: number | undefined;
  /** Order most severe first (critical, warning, info, positive). Default `true`. */
  sort?: boolean | undefined;
  /**
   * Shows a dismiss button on every insight. The app removes the insight;
   * focus moves to the next insight's dismiss button, or to the strip.
   */
  onDismiss?: ((id: string) => void) | undefined;
  /** Placeholder cards while insights load. */
  loading?: boolean | undefined;
  /** Replaces the strip's content with this failure. */
  error?: ReactNode;
  /** With `error`, shows Try again calling this. */
  onRetry?: (() => unknown) | undefined;
  /** Replaces the default empty text. `null` renders nothing at all when there are no insights. */
  empty?: ReactNode;
  /** Strings, for localisation. */
  messages?: Partial<InsightStripMessages> | undefined;
  /** Strings passed to every AiInsightCallout (severity prefixes, origin labels). */
  calloutMessages?: AiInsightCalloutProps['messages'];
}

/**
 * A row of insights for the top of a dashboard, built from AiInsightCallout so
 * every insight says honestly whether a rule or a model raised it. Sorted by
 * severity, with an optional limit and per-insight dismiss.
 */
export const InsightStrip = forwardRef<HTMLElement, InsightStripProps>(function InsightStrip(
  {
    insights,
    title,
    headingLevel = 2,
    layout = 'grid',
    max,
    sort = true,
    onDismiss,
    loading = false,
    error,
    onRetry,
    empty,
    messages: messagesProp,
    calloutMessages,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...INSIGHT_STRIP_MESSAGES, ...messagesProp };
  const titleId = useId();
  const listId = useId();
  const [expanded, setExpanded] = useState(false);
  const rootRef = useRef<HTMLElement | null>(null);
  const headingRef = useRef<HTMLHeadingElement | null>(null);
  const pendingFocus = useRef<number | null>(null);

  const ordered = sort ? sortInsights(insights) : insights.slice();
  const limit = max !== undefined && max > 0 && !expanded ? max : ordered.length;
  const shown = ordered.slice(0, limit);
  const hidden = ordered.length - shown.length;
  const canCollapse = max !== undefined && max > 0 && ordered.length > max;

  useLayoutEffect(() => {
    if (pendingFocus.current === null) return;
    const index = pendingFocus.current;
    pendingFocus.current = null;
    const root = rootRef.current;
    if (!root) return;
    const buttons = root.querySelectorAll<HTMLButtonElement>('.mn-ai-insight-callout__dismiss');
    const next = buttons[Math.min(index, buttons.length - 1)];
    if (next) next.focus();
    else (headingRef.current ?? root).focus();
  }, [insights]);

  const hasError = !loading && error !== undefined && error !== null && error !== false;
  const Heading = `h${headingLevel}` as const;
  const calloutLevel = (title ? headingLevel + 1 : undefined) as 3 | 4 | 5 | 6 | undefined;

  let content: ReactNode;
  if (loading) {
    content = (
      <div className="mn-insight-strip__list" role="img" aria-label={messages.loading}>
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} shape="rect" height="6rem" className="mn-insight-strip__skeleton" />
        ))}
      </div>
    );
  } else if (hasError) {
    content = onRetry ? (
      <InlineErrorRetry onRetry={onRetry}>{error}</InlineErrorRetry>
    ) : (
      <p className="mn-insight-strip__error" role="alert">
        {error}
      </p>
    );
  } else if (ordered.length === 0) {
    if (empty === null) return null;
    content = empty ?? (
      <EmptyState
        size="compact"
        layout="line"
        headingLevel="p"
        kind="success"
        title={messages.empty}
      />
    );
  } else {
    content = (
      <>
        <ul id={listId} className="mn-insight-strip__list">
          {shown.map((insight, index) => (
            <li key={insight.id} className="mn-insight-strip__item">
              <AiInsightCallout
                severity={insight.severity}
                origin={insight.origin}
                title={insight.title}
                why={insight.why}
                evidence={insight.evidence}
                action={insight.action}
                headingLevel={calloutLevel}
                messages={calloutMessages}
                onDismiss={
                  onDismiss
                    ? () => {
                        pendingFocus.current = index;
                        onDismiss(insight.id);
                      }
                    : undefined
                }
              />
            </li>
          ))}
        </ul>
        {canCollapse ? (
          <Button
            size="sm"
            variant="ghost"
            className="mn-insight-strip__toggle"
            aria-expanded={expanded}
            aria-controls={listId}
            onClick={() => setExpanded((open) => !open)}
          >
            {expanded ? messages.showLess : messages.showMore(hidden)}
          </Button>
        ) : null}
      </>
    );
  }

  const Root = title ? 'section' : 'div';
  return (
    <Root
      ref={(node: HTMLElement | null) => {
        rootRef.current = node;
        if (typeof ref === 'function') ref(node);
        else if (ref) ref.current = node;
      }}
      aria-labelledby={title ? titleId : undefined}
      aria-busy={loading || undefined}
      tabIndex={title ? undefined : -1}
      {...rest}
      className={cx('mn-insight-strip', className)}
      data-layout={layout}
      data-state={
        loading ? 'loading' : hasError ? 'error' : ordered.length === 0 ? 'empty' : undefined
      }
    >
      {title ? (
        <Heading id={titleId} ref={headingRef} tabIndex={-1} className="mn-insight-strip__title">
          {title}
        </Heading>
      ) : null}
      {content}
    </Root>
  );
});
