'use client';

import { forwardRef, useId, type HTMLAttributes, type ReactNode, type SVGProps } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  CloseGlyph,
  ErrorGlyph,
  InfoGlyph,
  SuccessGlyph,
  WarningGlyph,
  BotGlyph,
} from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';

/** How much an insight matters. `critical` needs action now; `positive` is good news. */
export type AiInsightSeverity = 'info' | 'warning' | 'critical' | 'positive';

/**
 * Where an insight came from. `rule` is a fixed, deterministic rule written
 * by people ("invoice is 30 days overdue"); `ai` was produced by a model.
 */
export type AiInsightOrigin = 'rule' | 'ai';

/** An action offered by an insight. */
export interface AiInsightAction {
  label: ReactNode;
  onClick: () => void;
}

export interface AiInsightCalloutMessages {
  /** Hidden prefix read before the title, per severity, so severity is never colour alone. */
  severity: Record<AiInsightSeverity, string>;
  /** Visible origin label, per origin. */
  origin: Record<AiInsightOrigin, string>;
  /** Summary of the evidence disclosure. */
  evidence: string;
  /** Accessible name of the dismiss button. */
  dismiss: string;
}

export const AI_INSIGHT_CALLOUT_MESSAGES: AiInsightCalloutMessages = {
  severity: { info: 'Info', warning: 'Warning', critical: 'Critical', positive: 'Positive' },
  origin: { rule: 'Rule-based', ai: 'AI insight' },
  evidence: 'Show evidence',
  dismiss: 'Dismiss insight',
};

export interface AiInsightCalloutProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** How much the insight matters. Sets the icon, the hidden severity prefix and the tone. */
  severity: AiInsightSeverity;
  /** The insight in one line, e.g. "Renewal at risk". Becomes the accessible name. */
  title: ReactNode;
  /** Why it was raised, in plain words. Becomes the accessible description. */
  why: ReactNode;
  /** Supporting data (figures, records, a small table), shown in a collapsed disclosure. */
  evidence?: ReactNode | undefined;
  /** One follow-up action, shown as a small button. */
  action?: AiInsightAction | undefined;
  /**
   * Where the insight came from, always shown. Only `ai` gets the Bot glyph
   * and the AI colour; `rule` is labelled "Rule-based" in neutral colours.
   */
  origin: AiInsightOrigin;
  /** Shows a dismiss button. The app removes the callout and decides where focus goes. */
  onDismiss?: (() => void) | undefined;
  /** Renders the title as a heading of this level instead of a paragraph. */
  headingLevel?: 2 | 3 | 4 | 5 | 6 | undefined;
  /** Strings, for localisation. `severity` and `origin` merge per key. */
  messages?:
    | (Partial<Omit<AiInsightCalloutMessages, 'severity' | 'origin'>> & {
        severity?: Partial<Record<AiInsightSeverity, string>>;
        origin?: Partial<Record<AiInsightOrigin, string>>;
      })
    | undefined;
}

/** A ruled-list glyph for rule-based origin: a checklist, not an AI mark. */
function RuleGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      width={14}
      height={14}
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable={false}
      {...props}
    >
      <path d="M2 4l1.25 1.25L5.5 3M2 10l1.25 1.25L5.5 9M8 4.25h6M8 10.25h6" />
    </svg>
  );
}

const ICON: Record<AiInsightSeverity, ReactNode> = {
  info: <InfoGlyph />,
  warning: <WarningGlyph />,
  critical: <ErrorGlyph />,
  positive: <SuccessGlyph />,
};

/**
 * A proactive insight about the current record or page: what was noticed,
 * why, the evidence and one action. The origin is always shown honestly:
 * "AI insight" with the Bot glyph and AI colour for model output,
 * "Rule-based" in neutral colours for deterministic rules.
 */
export const AiInsightCallout = forwardRef<HTMLDivElement, AiInsightCalloutProps>(
  function AiInsightCallout(
    {
      severity,
      title,
      why,
      evidence,
      action,
      origin,
      onDismiss,
      headingLevel,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = {
      ...AI_INSIGHT_CALLOUT_MESSAGES,
      ...messagesProp,
      severity: { ...AI_INSIGHT_CALLOUT_MESSAGES.severity, ...messagesProp?.severity },
      origin: { ...AI_INSIGHT_CALLOUT_MESSAGES.origin, ...messagesProp?.origin },
    };
    const titleId = useId();
    const whyId = useId();
    const Title = headingLevel ? (`h${headingLevel}` as const) : 'p';

    return (
      <div
        ref={ref}
        role="group"
        aria-labelledby={titleId}
        aria-describedby={whyId}
        {...rest}
        className={cx('mn-ai-insight-callout', className)}
        data-severity={severity}
        data-origin={origin}
      >
        <span className="mn-ai-insight-callout__icon" aria-hidden>
          {ICON[severity]}
        </span>
        <div className="mn-ai-insight-callout__body">
          <div className="mn-ai-insight-callout__header">
            <Title id={titleId} className="mn-ai-insight-callout__title">
              <span className="mn-visually-hidden">{messages.severity[severity]}: </span>
              {title}
            </Title>
            <span className="mn-ai-insight-callout__origin" data-origin={origin}>
              {origin === 'ai' ? <BotGlyph /> : <RuleGlyph />}
              {messages.origin[origin]}
            </span>
          </div>
          <div id={whyId} className="mn-ai-insight-callout__why">
            {why}
          </div>
          {evidence ? (
            <details className="mn-ai-insight-callout__evidence">
              <summary className="mn-ai-insight-callout__summary">{messages.evidence}</summary>
              <div className="mn-ai-insight-callout__evidence-body">{evidence}</div>
            </details>
          ) : null}
          {action ? (
            <div className="mn-ai-insight-callout__actions">
              <Button size="sm" className="mn-ai-insight-callout__action" onClick={action.onClick}>
                {action.label}
              </Button>
            </div>
          ) : null}
        </div>
        {onDismiss ? (
          <IconButton
            size="sm"
            label={messages.dismiss}
            icon={<CloseGlyph />}
            className="mn-ai-insight-callout__dismiss"
            onClick={onDismiss}
          />
        ) : null}
      </div>
    );
  },
);
