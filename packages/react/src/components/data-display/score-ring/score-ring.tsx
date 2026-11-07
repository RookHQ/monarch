'use client';

import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';

export type ScoreRingTone = 'neutral' | 'accent' | 'info' | 'success' | 'warning' | 'danger';

/** A band of the score scale: values at or above `min` take `tone`. */
export interface ScoreRingThreshold {
  min: number;
  tone: ScoreRingTone;
}

/** Ranker's bands on a 0–100 scale: 80+ success, 60+ info, 40+ warning, below 40 danger. */
export const defaultScoreRingThresholds: readonly ScoreRingThreshold[] = [
  { min: 80, tone: 'success' },
  { min: 60, tone: 'info' },
  { min: 40, tone: 'warning' },
  { min: -Infinity, tone: 'danger' },
];

export interface ScoreRingMessages {
  /** Accessible name when there is no `label` or `aria-label`. */
  name: string;
  /** Spoken value, e.g. "78 out of 100". */
  valueText: (value: string, max: number) => string;
  /** The small "/ 100" under the number. */
  ofMax: (max: number) => string;
  /** Spoken and shown when `value` is null. */
  noValue: string;
}

export const defaultScoreRingMessages: ScoreRingMessages = {
  name: 'Score',
  valueText: (value, max) => `${value} out of ${max}`,
  ofMax: (max) => `/ ${max}`,
  noValue: 'No score yet',
};

export interface ScoreRingProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The score. `null` shows an empty ring and a dash. Values are clamped to 0…`max`. */
  value: number | null;
  /** Top of the scale. Default 100. */
  max?: number;
  /** Ring diameter in px. Default 120. */
  size?: number;
  /** Stroke width in px. Default: a tenth of `size`, at least 4. */
  thickness?: number;
  /**
   * Colour of the arc. `auto` (default) picks a status tone from `thresholds`;
   * any other tone is fixed. Use `accent` for a brand-coloured score with no
   * good/bad meaning.
   */
  tone?: ScoreRingTone | 'auto';
  /** Bands used by `tone="auto"`, checked from the highest `min`. Default `defaultScoreRingThresholds`. */
  thresholds?: readonly ScoreRingThreshold[];
  /** Visible label under the ring; also its accessible name. */
  label?: ReactNode;
  /** Secondary line under the label, e.g. "Updated today". */
  description?: ReactNode;
  /** Show "/ 100" under the number. Default true when `size` is at least 72. */
  showMax?: boolean;
  /** Formats the number in the centre and in the spoken value. Default: rounded to a whole number. */
  formatValue?: ((value: number) => string) | undefined;
  messages?: Partial<ScoreRingMessages>;
}

function toneFor(value: number, thresholds: readonly ScoreRingThreshold[]): ScoreRingTone {
  const sorted = [...thresholds].sort((a, b) => b.min - a.min);
  return sorted.find((t) => value >= t.min)?.tone ?? 'neutral';
}

/**
 * A circular score: an arc filled to `value / max` with the number written in
 * the centre as text. Exposed as a meter, so screen readers hear "Score, 78
 * out of 100".
 */
export const ScoreRing = forwardRef<HTMLDivElement, ScoreRingProps>(function ScoreRing(
  {
    value,
    max = 100,
    size = 120,
    thickness,
    tone = 'auto',
    thresholds = defaultScoreRingThresholds,
    label,
    description,
    showMax,
    formatValue = (v) => String(Math.round(v)),
    messages,
    className,
    style,
    ...rest
  },
  ref,
) {
  const m = { ...defaultScoreRingMessages, ...messages };
  const labelId = useId();
  const safeMax = max > 0 ? max : 100;
  const has = value !== null && Number.isFinite(value);
  const clamped = has ? Math.min(safeMax, Math.max(0, value)) : 0;
  const stroke = thickness ?? Math.max(4, Math.round(size / 10));
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const offset = c - (clamped / safeMax) * c;
  const resolvedTone = tone === 'auto' ? (has ? toneFor(clamped, thresholds) : 'neutral') : tone;
  const text = has ? formatValue(clamped) : '–';
  const named = rest['aria-label'] !== undefined || rest['aria-labelledby'] !== undefined;
  const nameProps = named
    ? {}
    : {
        'aria-labelledby': [
          label ? `${labelId}-label` : `${labelId}-name`,
          has ? '' : `${labelId}-empty`,
        ]
          .filter(Boolean)
          .join(' '),
      };
  const roleProps = has
    ? {
        role: 'meter',
        'aria-valuemin': 0,
        'aria-valuemax': safeMax,
        'aria-valuenow': clamped,
        'aria-valuetext': m.valueText(text, safeMax),
      }
    : { role: 'img' };
  const showMaxText = (showMax ?? size >= 72) && has;

  return (
    <div
      ref={ref}
      {...roleProps}
      {...nameProps}
      {...rest}
      data-tone={resolvedTone}
      data-empty={has ? undefined : ''}
      className={cx('mn-score-ring', className)}
      style={{ ...style, ['--mn-score-ring-size' as string]: `${size}px` }}
    >
      <div className="mn-score-ring__dial">
        <svg
          className="mn-score-ring__svg"
          width={size}
          height={size}
          viewBox={`0 0 ${size} ${size}`}
          aria-hidden="true"
          focusable="false"
        >
          <circle
            className="mn-score-ring__track"
            cx={size / 2}
            cy={size / 2}
            r={r}
            strokeWidth={stroke}
            fill="none"
          />
          {has && clamped > 0 ? (
            <circle
              className="mn-score-ring__arc"
              cx={size / 2}
              cy={size / 2}
              r={r}
              strokeWidth={stroke}
              strokeDasharray={c}
              strokeDashoffset={offset}
              strokeLinecap="round"
              fill="none"
              transform={`rotate(-90 ${size / 2} ${size / 2})`}
            />
          ) : null}
        </svg>
        <span className="mn-score-ring__center" aria-hidden="true">
          <span className="mn-score-ring__value">{text}</span>
          {showMaxText ? <span className="mn-score-ring__max">{m.ofMax(safeMax)}</span> : null}
        </span>
        {label ? null : (
          <span className="mn-visually-hidden" id={`${labelId}-name`}>
            {m.name}
          </span>
        )}
        {has ? null : (
          <span className="mn-visually-hidden" id={`${labelId}-empty`}>
            {m.noValue}
          </span>
        )}
      </div>
      {label ? (
        <span className="mn-score-ring__label" id={`${labelId}-label`}>
          {label}
        </span>
      ) : null}
      {description ? <span className="mn-score-ring__description">{description}</span> : null}
    </div>
  );
});
