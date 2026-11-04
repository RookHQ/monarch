import { forwardRef, type ComponentPropsWithoutRef, type ElementRef } from 'react';
import * as ProgressPrimitive from '@radix-ui/react-progress';
import { cx } from '../../../internal/cx.js';

export type ProgressTone = 'accent' | 'success' | 'warning' | 'danger' | 'neutral';
export type ProgressSegmentTone =
  ProgressTone | 'info' | 'data-1' | 'data-2' | 'data-3' | 'data-4' | 'data-5';

/** One part of a stacked bar. */
export interface ProgressSegment {
  /** Amount, in the same units as `max`. */
  value: number;
  /** Names the segment in the text alternative, e.g. "Done". */
  label: string;
  /** Fill colour. Default: the data palette in order (`data-1`, `data-2`, …). */
  tone?: ProgressSegmentTone;
}

export interface ProgressProps extends Omit<
  ComponentPropsWithoutRef<typeof ProgressPrimitive.Root>,
  'value'
> {
  /** 0–`max`. Omit for indeterminate. */
  value?: number | null;
  max?: number;
  /** Accessible name, e.g. "Upload progress". Required unless aria-labelledby is set. */
  label?: string;
  size?: 'sm' | 'md';
  /**
   * Fill colour. Default `accent`. With `semantics="meter"` and no tone, the
   * tone is `neutral` (a normal quota is not a status), follows `thresholds`,
   * and a value over `max` is `danger`.
   */
  tone?: ProgressTone;
  /**
   * `progressbar` (default) for a task moving towards completion;
   * `meter` for a measured quantity within a known range, such as a quota or
   * capacity (`role="meter"`). A meter is never indeterminate.
   */
  semantics?: 'progressbar' | 'meter';
  /**
   * Meter only: values at or above which the automatic tone becomes
   * `warning` and `danger`, e.g. `{ warning: 80, danger: 95 }` of 100.
   */
  thresholds?: { warning?: number; danger?: number };
  /**
   * Stacked parts that add up to the filled amount, e.g. done / in review /
   * in progress. `value` is then the sum of the segments.
   */
  segments?: ProgressSegment[];
  /**
   * Text alternative for the value (`aria-valuetext`), e.g. "7.2 of 10 GB used".
   * Default for meters and segmented bars: generated from the values.
   */
  valueText?: string;
}

function clampPct(value: number, max: number) {
  if (!(max > 0)) return 0;
  return Math.min(100, Math.max(0, (value / max) * 100));
}

function round(n: number) {
  return Math.round(n * 100) / 100;
}

const SEGMENT_DEFAULTS: ProgressSegmentTone[] = ['data-1', 'data-2', 'data-3', 'data-4', 'data-5'];

/**
 * Linear progress bar; `value` undefined renders an indeterminate animation.
 * Also a quota meter (`semantics="meter"`) and a stacked bar (`segments`).
 */
export const Progress = forwardRef<ElementRef<typeof ProgressPrimitive.Root>, ProgressProps>(
  function Progress(
    {
      value,
      max = 100,
      label,
      size = 'md',
      tone,
      semantics = 'progressbar',
      thresholds,
      segments,
      valueText,
      getValueLabel,
      className,
      ...rest
    },
    ref,
  ) {
    const meter = semantics === 'meter';
    if (!meter && !segments) {
      // The original 0.3 progress bar, unchanged (Radix Progress).
      const pct =
        value === undefined || value === null
          ? undefined
          : Math.min(100, Math.max(0, (value / max) * 100));
      return (
        <ProgressPrimitive.Root
          ref={ref}
          value={value ?? null}
          max={max}
          aria-label={label}
          {...(valueText !== undefined
            ? { getValueLabel: () => valueText }
            : getValueLabel && { getValueLabel })}
          {...rest}
          data-size={size}
          data-tone={tone ?? 'accent'}
          data-indeterminate={pct === undefined || undefined}
          className={cx('mn-progress', className)}
        >
          <ProgressPrimitive.Indicator
            className="mn-progress__bar"
            style={pct === undefined ? undefined : { width: `${pct}%` }}
          />
        </ProgressPrimitive.Root>
      );
    }

    // Meter and segmented bars: hand-built so the role and value text are exact.
    const parts = segments?.map((s, i) => ({
      ...s,
      tone: s.tone ?? SEGMENT_DEFAULTS[i % SEGMENT_DEFAULTS.length] ?? 'data-1',
    }));
    const amount = parts ? parts.reduce((sum, s) => sum + Math.max(0, s.value), 0) : (value ?? 0);
    const over = amount > max;
    let resolvedTone: ProgressTone = tone ?? (meter ? 'neutral' : 'accent');
    if (meter && tone === undefined) {
      if (over || (thresholds?.danger !== undefined && amount >= thresholds.danger)) {
        resolvedTone = 'danger';
      } else if (thresholds?.warning !== undefined && amount >= thresholds.warning) {
        resolvedTone = 'warning';
      }
    }
    const generatedText = parts
      ? `${parts.map((s) => `${s.label} ${round(s.value)}`).join(', ')} (${round(amount)} of ${round(max)})`
      : `${round(amount)} of ${round(max)}${over ? ', over the limit' : ''}`;
    const role = meter ? 'meter' : 'progressbar';
    let offset = 0;
    return (
      <div
        ref={ref}
        role={role}
        aria-label={label}
        aria-valuemin={0}
        aria-valuemax={max}
        aria-valuenow={Math.min(amount, max)}
        aria-valuetext={valueText ?? generatedText}
        {...rest}
        data-size={size}
        data-tone={resolvedTone}
        data-semantics={semantics}
        data-segmented={parts ? '' : undefined}
        data-over={over || undefined}
        data-value={amount}
        data-max={max}
        className={cx('mn-progress', className)}
      >
        {parts ? (
          parts.map((s, i) => {
            const width = Math.max(0, Math.min(clampPct(s.value, max), 100 - offset));
            const start = offset;
            offset += width;
            return (
              <div
                // Segments are positional; their order is their identity.
                key={i}
                className="mn-progress__segment"
                data-tone={s.tone}
                style={{ insetInlineStart: `${start}%`, width: `${width}%` }}
              />
            );
          })
        ) : (
          <div className="mn-progress__bar" style={{ width: `${clampPct(amount, max)}%` }} />
        )}
      </div>
    );
  },
);
