import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { PartialGlyph, SuccessGlyph, WarningGlyph } from '../../../internal/icons.js';
import { Tooltip, TooltipProvider } from '../../overlays/tooltip/index.js';

/** A confidence band. */
export type AiConfidenceLevel = 'low' | 'medium' | 'high';

/** Band edges on the 0–1 scale: below `low` is low, at or above `high` is high. */
export interface AiConfidenceThresholds {
  low: number;
  high: number;
}

export const DEFAULT_CONFIDENCE_THRESHOLDS: AiConfidenceThresholds = { low: 0.6, high: 0.85 };

/** Clamps a 0–1 value; `NaN` and missing values become 0. */
function clamp01(value: number) {
  return Number.isFinite(value) ? Math.min(1, Math.max(0, value)) : 0;
}

/**
 * The band for a 0–1 confidence value: below `thresholds.low` is `low`, at or
 * above `thresholds.high` is `high`, anything between is `medium`.
 */
export function aiConfidenceLevel(
  value: number,
  thresholds: Partial<AiConfidenceThresholds> = DEFAULT_CONFIDENCE_THRESHOLDS,
): AiConfidenceLevel {
  const { low, high } = { ...DEFAULT_CONFIDENCE_THRESHOLDS, ...thresholds };
  const v = clamp01(value);
  if (v < low) return 'low';
  if (v >= high) return 'high';
  return 'medium';
}

export interface AiConfidenceMeterMessages {
  /** Visible label per band. Low reads "Needs check" so people know to verify it. */
  labels: Record<AiConfidenceLevel, string>;
  /** Accessible name of the bar variant. */
  name: string;
  /** Formats the value, 0–1, e.g. "72%". */
  formatValue: (value: number) => string;
  /** The value text read with the label, e.g. "Needs check, 42% confidence". */
  valueText: (label: string, formatted: string) => string;
}

const DEFAULT_MESSAGES: AiConfidenceMeterMessages = {
  labels: { low: 'Needs check', medium: 'Medium confidence', high: 'High confidence' },
  name: 'Confidence',
  formatValue: (value) =>
    new Intl.NumberFormat(undefined, { style: 'percent', maximumFractionDigits: 0 }).format(value),
  valueText: (label, formatted) => `${label}, ${formatted} confidence`,
};

const ICON: Record<AiConfidenceLevel, ReactNode> = {
  low: <WarningGlyph />,
  medium: <PartialGlyph />,
  high: <SuccessGlyph />,
};

export interface AiConfidenceMeterProps extends HTMLAttributes<HTMLDivElement> {
  /** How confident the model is, from 0 to 1. Values outside the range are clamped. */
  value: number;
  /** Band edges. Default `{ low: 0.6, high: 0.85 }`; either edge can be given alone. */
  thresholds?: Partial<AiConfidenceThresholds> | undefined;
  /**
   * Why the model is (or is not) confident. Shown under the bar, or on hover
   * and focus for the badge; always available to assistive technology.
   */
  explanation?: ReactNode | undefined;
  /** `badge`: a compact pill for fields and table cells. `bar`: a labelled meter. Default `badge`. */
  variant?: 'badge' | 'bar';
  /** Shows the percentage next to the label. Default `false` for `badge`, `true` for `bar`. */
  showValue?: boolean;
  /** `sm` for table cells and dense lists. Default `md`. */
  size?: 'sm' | 'md';
  /** Strings, for localisation. `labels` merges per band. */
  messages?: Partial<Omit<AiConfidenceMeterMessages, 'labels'>> & {
    labels?: Partial<Record<AiConfidenceLevel, string>>;
  };
}

/**
 * How sure the AI is about a value, as low, medium or high. Each band has a
 * text label and an icon as well as a colour; low reads "Needs check" by
 * default. A compact badge or a labelled bar.
 */
export const AiConfidenceMeter = forwardRef<HTMLDivElement, AiConfidenceMeterProps>(
  function AiConfidenceMeter(
    {
      value,
      thresholds,
      explanation,
      variant = 'badge',
      showValue,
      size = 'md',
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      labels: { ...DEFAULT_MESSAGES.labels, ...messagesProp?.labels },
    };
    const v = clamp01(value);
    const level = aiConfidenceLevel(v, thresholds);
    const label = messages.labels[level];
    const formatted = messages.formatValue(v);
    const withValue = showValue ?? variant === 'bar';
    const explanationId = useId();
    const hasExplanation = explanation !== undefined && explanation !== null && explanation !== '';
    const valueText = messages.valueText(label, formatted);

    if (variant === 'bar') {
      return (
        <div
          ref={ref}
          {...rest}
          className={cx('mn-ai-confidence-meter', className)}
          data-variant="bar"
          data-level={level}
          data-size={size}
        >
          <div className="mn-ai-confidence-meter__header">
            <span className="mn-ai-confidence-meter__label">
              <span className="mn-ai-confidence-meter__icon" aria-hidden>
                {ICON[level]}
              </span>
              {label}
            </span>
            {withValue ? (
              <span className="mn-ai-confidence-meter__value" aria-hidden>
                {formatted}
              </span>
            ) : null}
          </div>
          <div
            role="meter"
            aria-label={messages.name}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={Math.round(v * 100)}
            aria-valuetext={valueText}
            aria-describedby={hasExplanation ? explanationId : undefined}
            className="mn-ai-confidence-meter__track"
          >
            <span className="mn-ai-confidence-meter__fill" style={{ inlineSize: `${v * 100}%` }} />
          </div>
          {hasExplanation ? (
            <div id={explanationId} className="mn-ai-confidence-meter__explanation">
              {explanation}
            </div>
          ) : null}
        </div>
      );
    }

    const badge = (
      <div
        ref={ref}
        {...rest}
        // Focusable only so the explanation tooltip can open from the keyboard.
        tabIndex={hasExplanation ? (rest.tabIndex ?? 0) : rest.tabIndex}
        className={cx('mn-ai-confidence-meter', className)}
        data-variant="badge"
        data-level={level}
        data-size={size}
      >
        <span className="mn-ai-confidence-meter__icon" aria-hidden>
          {ICON[level]}
        </span>
        <span className="mn-ai-confidence-meter__label" aria-hidden>
          {label}
        </span>
        {withValue ? (
          <span className="mn-ai-confidence-meter__value" aria-hidden>
            {formatted}
          </span>
        ) : null}
        <span className="mn-visually-hidden">{valueText}</span>
        {hasExplanation ? <span className="mn-visually-hidden">. {explanation}</span> : null}
      </div>
    );

    if (!hasExplanation) return badge;
    return (
      <TooltipProvider>
        <Tooltip content={explanation}>{badge}</Tooltip>
      </TooltipProvider>
    );
  },
);
