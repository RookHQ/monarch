import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  ArrowDownGlyph,
  ErrorGlyph,
  SuccessGlyph,
  WarningGlyph,
  BotGlyph,
} from '../../../internal/icons.js';

/** The colour family of a score band. Always paired with the band's text label. */
export type AiScoreTone = 'danger' | 'warning' | 'success' | 'info' | 'neutral' | 'ai';

/** A named range of the score, from `min` up to the next band's `min`. */
export interface AiScoreBand {
  /** Lowest score (0–100) in this band. */
  min: number;
  /** Shown next to the score, e.g. "High". Never left to colour alone. */
  label: string;
  /** Colour of the ring or bar while the score is in this band. Default `neutral`. */
  tone?: AiScoreTone | undefined;
}

/** One input that moved the score, positive (raised it) or negative (lowered it). */
export interface AiScoreFactor {
  label: ReactNode;
  /** Points this factor added (positive) or removed (negative). */
  impact: number;
  /** A short explanation, shown under the label. */
  detail?: ReactNode | undefined;
}

/** Default bands: Low below 40, Medium from 40, High from 70. */
export const DEFAULT_AI_SCORE_BANDS: readonly AiScoreBand[] = [
  { min: 0, label: 'Low', tone: 'danger' },
  { min: 40, label: 'Medium', tone: 'warning' },
  { min: 70, label: 'High', tone: 'success' },
];

/** A band with its computed upper bound. */
export interface AiScoreBandRange extends AiScoreBand {
  /** Highest score in the band: the next band's `min` minus one, or 100. */
  max: number;
}

/** Sorts bands by `min` and adds each band's `max`. */
export function aiScoreBandRanges(
  bands: readonly AiScoreBand[] = DEFAULT_AI_SCORE_BANDS,
): AiScoreBandRange[] {
  const sorted = [...bands].sort((a, b) => a.min - b.min);
  return sorted.map((band, i) => {
    const next = sorted[i + 1];
    return { ...band, max: next ? Math.max(band.min, next.min - 1) : 100 };
  });
}

/** The band a score falls in: the one with the highest `min` at or below it. */
export function aiScoreBand(
  value: number,
  bands: readonly AiScoreBand[] = DEFAULT_AI_SCORE_BANDS,
): AiScoreBandRange | undefined {
  const ranges = aiScoreBandRanges(bands);
  let found: AiScoreBandRange | undefined;
  for (const band of ranges) if (value >= band.min) found = band;
  return found ?? ranges[0];
}

export interface AiScoreMessages {
  /** Default label of what is scored, when `label` is not given. */
  label: string;
  /** The AI provenance mark next to the label. */
  aiGenerated: string;
  /** Hidden text after the number, read as "72 out of 100". */
  outOf: (max: number) => string;
  /** Shown instead of a number when `value` is `null`. */
  unavailable: string;
  /** Caption of the factor table. */
  factors: string;
  factorColumn: string;
  impactColumn: string;
  /** Summary of the band legend disclosure. */
  bands: string;
  bandColumn: string;
  rangeColumn: string;
  /** Marks the band the score is in, in the legend. */
  currentBand: string;
}

export const AI_SCORE_MESSAGES: AiScoreMessages = {
  label: 'Score',
  aiGenerated: 'AI score',
  outOf: (max) => `out of ${max}`,
  unavailable: 'No score yet',
  factors: 'What affects this score',
  factorColumn: 'Factor',
  impactColumn: 'Impact',
  bands: 'How to read this score',
  bandColumn: 'Band',
  rangeColumn: 'Range',
  currentBand: 'this score',
};

export interface AiScoreProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The score from 0 to 100; values outside are clamped. `null` while there is no score. */
  value: number | null;
  /** What is scored, e.g. "Lead score". Names the group. Default "Score". */
  label?: ReactNode | undefined;
  /** Named ranges with a label and tone. Default Low (0), Medium (40), High (70). */
  bands?: readonly AiScoreBand[] | undefined;
  /** Why the model gave this score, in plain words. */
  rationale?: ReactNode | undefined;
  /** The inputs that moved the score, shown as a table with signed impacts. */
  factors?: readonly AiScoreFactor[] | undefined;
  /** `ring` (default) is a circular gauge with the number in its centre; `bar` is a horizontal bar. */
  variant?: 'ring' | 'bar' | undefined;
  /** Ring size. Default `md`. */
  size?: 'sm' | 'md' | undefined;
  /** Show the band legend disclosure. Default `true`. */
  showBands?: boolean | undefined;
  /** BCP 47 locale for numbers. Default: the runtime locale. */
  locale?: string | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiScoreMessages> | undefined;
}

const TONE_GLYPH: Partial<Record<AiScoreTone, ReactNode>> = {
  danger: <ErrorGlyph />,
  warning: <WarningGlyph />,
  success: <SuccessGlyph />,
};

/**
 * A 0–100 score produced by a model, as a ring with the number in its centre
 * or as a bar, with its band label ("High"), the rationale and a table of the
 * factors that raised or lowered it. Everything shown graphically is also
 * real text, so the score never depends on colour or on the graphic.
 */
export const AiScore = forwardRef<HTMLDivElement, AiScoreProps>(function AiScore(
  {
    value: rawValue,
    label,
    bands = DEFAULT_AI_SCORE_BANDS,
    rationale,
    factors,
    variant = 'ring',
    size = 'md',
    showBands = true,
    locale,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...AI_SCORE_MESSAGES, ...messagesProp };
  const labelId = useId();
  const has = rawValue !== null && Number.isFinite(rawValue);
  const value = has ? Math.min(100, Math.max(0, rawValue)) : 0;
  const ranges = aiScoreBandRanges(bands);
  const band = has ? aiScoreBand(value, bands) : undefined;
  const tone = band?.tone ?? 'neutral';
  const number = new Intl.NumberFormat(locale, { maximumFractionDigits: 0 });
  const signed = new Intl.NumberFormat(locale, {
    maximumFractionDigits: 1,
    signDisplay: 'exceptZero',
  });
  const maxImpact = Math.max(1, ...(factors ?? []).map((f) => Math.abs(f.impact)));

  const readout = has ? (
    <span className="mn-ai-score__value">
      <span className="mn-ai-score__number">{number.format(Math.round(value))}</span>
      <span className="mn-ai-score__max" aria-hidden>
        /{number.format(100)}
      </span>
      <span className="mn-visually-hidden"> {messages.outOf(100)}</span>
    </span>
  ) : (
    <span className="mn-ai-score__value mn-ai-score__value--empty">{messages.unavailable}</span>
  );

  return (
    <div
      ref={ref}
      role="group"
      aria-labelledby={labelId}
      {...rest}
      className={cx('mn-ai-score', className)}
      data-variant={variant}
      data-size={size}
      data-tone={tone}
      data-empty={has ? undefined : ''}
    >
      <div className="mn-ai-score__header">
        <span id={labelId} className="mn-ai-score__label">
          {label ?? messages.label}
        </span>
        <span className="mn-ai-score__ai">
          <BotGlyph />
          {messages.aiGenerated}
        </span>
      </div>

      <div className="mn-ai-score__display">
        {variant === 'ring' ? (
          <div className="mn-ai-score__ring">
            <svg className="mn-ai-score__svg" viewBox="0 0 36 36" aria-hidden focusable={false}>
              <circle className="mn-ai-score__track" cx="18" cy="18" r="15.5" pathLength={100} />
              {has ? (
                <circle
                  className="mn-ai-score__arc"
                  cx="18"
                  cy="18"
                  r="15.5"
                  pathLength={100}
                  strokeDasharray={`${value} 100`}
                />
              ) : null}
            </svg>
            <span className="mn-ai-score__center">{readout}</span>
          </div>
        ) : (
          <div className="mn-ai-score__bar-wrap">
            <div className="mn-ai-score__bar-text">{readout}</div>
            <div className="mn-ai-score__bar" aria-hidden>
              <div className="mn-ai-score__fill" style={{ inlineSize: `${value}%` }} />
              {ranges.slice(1).map((r) => (
                <span
                  key={r.min}
                  className="mn-ai-score__tick"
                  style={{ insetInlineStart: `${Math.min(100, Math.max(0, r.min))}%` }}
                />
              ))}
            </div>
          </div>
        )}
        {band ? (
          <span className="mn-ai-score__band" data-tone={tone}>
            {TONE_GLYPH[tone] ? (
              <span className="mn-ai-score__band-icon" aria-hidden>
                {TONE_GLYPH[tone]}
              </span>
            ) : null}
            {band.label}
          </span>
        ) : null}
      </div>

      {rationale ? <div className="mn-ai-score__rationale">{rationale}</div> : null}

      {factors && factors.length > 0 ? (
        <table className="mn-ai-score__factors">
          <caption className="mn-ai-score__caption">{messages.factors}</caption>
          <thead>
            <tr>
              <th scope="col">{messages.factorColumn}</th>
              <th scope="col" className="mn-ai-score__impact-head">
                {messages.impactColumn}
              </th>
            </tr>
          </thead>
          <tbody>
            {factors.map((factor, i) => {
              const direction = factor.impact > 0 ? 'up' : factor.impact < 0 ? 'down' : 'none';
              return (
                <tr key={i} className="mn-ai-score__factor" data-direction={direction}>
                  <th scope="row" className="mn-ai-score__factor-label">
                    {factor.label}
                    {factor.detail ? (
                      <span className="mn-ai-score__factor-detail">{factor.detail}</span>
                    ) : null}
                  </th>
                  <td className="mn-ai-score__impact">
                    <span className="mn-ai-score__impact-bar" aria-hidden>
                      <span
                        className="mn-ai-score__impact-fill"
                        style={{ inlineSize: `${(Math.abs(factor.impact) / maxImpact) * 50}%` }}
                      />
                    </span>
                    <span className="mn-ai-score__impact-text">
                      {direction === 'none' ? null : (
                        <ArrowDownGlyph className="mn-ai-score__impact-icon" aria-hidden />
                      )}
                      {signed.format(factor.impact)}
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      ) : null}

      {showBands && ranges.length > 0 ? (
        <details className="mn-ai-score__legend">
          <summary className="mn-ai-score__summary">{messages.bands}</summary>
          <table className="mn-ai-score__legend-table">
            <thead>
              <tr>
                <th scope="col">{messages.bandColumn}</th>
                <th scope="col">{messages.rangeColumn}</th>
              </tr>
            </thead>
            <tbody>
              {ranges.map((r) => {
                const current = band?.min === r.min;
                return (
                  <tr key={r.min} data-current={current || undefined}>
                    <th scope="row">
                      <span
                        className="mn-ai-score__swatch"
                        data-tone={r.tone ?? 'neutral'}
                        aria-hidden
                      />
                      {r.label}
                      {current ? (
                        <span className="mn-ai-score__current"> ({messages.currentBand})</span>
                      ) : null}
                    </th>
                    <td>
                      {number.format(r.min)}–{number.format(r.max)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </details>
      ) : null}
    </div>
  );
});
