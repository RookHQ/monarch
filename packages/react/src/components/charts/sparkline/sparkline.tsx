'use client';

import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  areaPath,
  barPath,
  chartColor,
  createValueFormatter,
  isolatedPoints,
  linePath,
  linearScale,
  seriesStats,
  useElementWidth,
  type ChartColor,
  type ChartDomainBound,
  type Point,
  type ValueFormatter,
} from '../../../internal/charts/index.js';

export type SparklineVariant = 'line' | 'area' | 'bar';
export type SparklineTone = 'current' | 'neutral' | 'accent' | 'success' | 'warning' | 'danger';
export type SparklineMarkers = 'none' | 'last' | 'all';

export interface SparklineThresholds {
  /** The latest value at or past this is `warning`. */
  warning?: number | undefined;
  /** The latest value at or past this is `danger`. */
  danger?: number | undefined;
  /** `above` (default): higher is worse, as for latency. `below`: lower is worse, as for uptime. */
  direction?: 'above' | 'below' | undefined;
}

export interface SparklineLabels {
  /** Shown when there are too few points to draw. Default "Not enough data". */
  notEnoughData?: string | undefined;
  /** The text alternative. Default "{label}: latest {last}, low {min}, high {max}". */
  summary?: ((label: string, last: string, min: string, max: string) => string) | undefined;
  /** Appended when a threshold sets the tone. Defaults "Warning" and "Critical". */
  warning?: string | undefined;
  danger?: string | undefined;
}

export interface SparklineProps extends Omit<
  HTMLAttributes<HTMLSpanElement>,
  'children' | 'color'
> {
  /** Values in order. `null` (or a non-finite number) is a gap: the line breaks there. */
  values: readonly (number | null)[];
  /** `line` (default), `area` (line plus soft fill) or `bar` (a sparkbar). Exposed as `data-variant`. */
  variant?: SparklineVariant | undefined;
  /** Width in px. Ignored with `responsive`. Default 80. */
  width?: number | undefined;
  /** Height in px. Default 24. */
  height?: number | undefined;
  /** Stretch to the container's width; the stroke keeps its thickness. */
  responsive?: boolean | undefined;
  /** Lower is better (a rank): smaller values are drawn higher. */
  invert?: boolean | undefined;
  /** Fix the value range, so sparklines in a table share one scale. Default: each fits its own values. */
  domain?: readonly [ChartDomainBound, ChartDomainBound] | undefined;
  /**
   * Colour with meaning: `current` (default, inherits text colour), `neutral`,
   * `accent`, `success`, `warning`, `danger`. Never the only signal: show the
   * value or delta beside it. Exposed as `data-tone`.
   */
  tone?: SparklineTone | undefined;
  /** A series colour instead of a tone, to match a series in a nearby chart. */
  color?: ChartColor | undefined;
  /** Picks the tone from the latest value (wins over `tone`); the summary names the state. */
  thresholds?: SparklineThresholds | undefined;
  /** Dots on `last` (default) point, `all` points or `none`. Lone points between gaps always get one. */
  markers?: SparklineMarkers | undefined;
  /**
   * Accessible name, such as "p95 latency, last 14 days". With it the
   * sparkline is an image whose text alternative gives the latest, low and
   * high values; without it the sparkline is decorative (`aria-hidden`), so put
   * the numbers in text nearby.
   */
  label?: string | undefined;
  /** Replaces the generated text alternative (needs `label`). */
  summary?: string | undefined;
  /** BCP 47 locale for the summary numbers. */
  locale?: string | undefined;
  /** `Intl.NumberFormat` options for the summary numbers. */
  valueFormat?: Intl.NumberFormatOptions | undefined;
  /** Formats the summary numbers. Wins over `valueFormat`. */
  formatValue?: ValueFormatter | undefined;
  labels?: SparklineLabels | undefined;
}

const PAD = 2;

function toneFor(last: number | undefined, t: SparklineThresholds | undefined) {
  if (!t || last === undefined) return undefined;
  const past = (limit: number | undefined) =>
    limit !== undefined && (t.direction === 'below' ? last <= limit : last >= limit);
  if (past(t.danger)) return 'danger' as const;
  if (past(t.warning)) return 'warning' as const;
  return 'neutral' as const;
}

/**
 * A word-sized trend line, area or sparkbar for tables, stat cards and
 * lists. Gaps break the line, `invert` suits ranks, thresholds set the tone,
 * and a `label` turns it into an image with a min / max / latest summary.
 */
export const Sparkline = forwardRef<HTMLSpanElement, SparklineProps>(function Sparkline(
  {
    values,
    variant = 'line',
    width: widthProp = 80,
    height = 24,
    responsive = false,
    invert = false,
    domain = ['auto', 'auto'],
    tone = 'current',
    color,
    thresholds,
    markers = 'last',
    label,
    summary,
    locale,
    valueFormat,
    formatValue,
    labels,
    className,
    style,
    ...rest
  },
  ref,
) {
  const [measureRef, measured] = useElementWidth<HTMLSpanElement>();
  const width = responsive ? (measured ?? widthProp) : widthProp;
  const clean = values.map((v) => (typeof v === 'number' && Number.isFinite(v) ? v : null));
  const stats = seriesStats(clean);
  const enough = variant === 'bar' ? (stats?.count ?? 0) >= 1 : (stats?.count ?? 0) >= 2;
  const thresholdTone = toneFor(stats?.last, thresholds);
  const resolvedTone = thresholdTone ?? tone;
  const fmt = createValueFormatter(locale, valueFormat, formatValue);

  const setRefs = (node: HTMLSpanElement | null) => {
    if (responsive) measureRef(node);
    if (typeof ref === 'function') ref(node);
    else if (ref) ref.current = node;
  };

  const rootProps = {
    ...rest,
    ref: setRefs,
    className: cx('mn-sparkline', className),
    'data-variant': variant,
    'data-tone': color === undefined ? resolvedTone : undefined,
    'data-responsive': responsive || undefined,
    style: {
      ...(color !== undefined ? { ['--_c' as string]: chartColor(color) } : {}),
      ...(responsive ? {} : { width: widthProp }),
      height,
      ...style,
    },
  };

  if (!enough || !stats) {
    const text = labels?.notEnoughData ?? 'Not enough data';
    return (
      <span {...rootProps} data-empty="">
        <span className="mn-sparkline__empty">
          {label ? <span className="mn-visually-hidden">{label}: </span> : null}
          {text}
        </span>
      </span>
    );
  }

  let lo = domain[0] === 'auto' ? stats.min : domain[0];
  let hi = domain[1] === 'auto' ? stats.max : domain[1];
  if (variant === 'bar') {
    lo = Math.min(lo, 0);
    hi = Math.max(hi, 0);
  }
  if (lo === hi) {
    lo -= 1;
    hi += 1;
  }
  const top = PAD;
  const bottom = height - PAD;
  const y = linearScale([lo, hi], invert ? [top, bottom] : [bottom, top]);
  const count = clean.length;

  let marks;
  if (variant === 'bar') {
    const band = width / count;
    const gap = band > 4 ? 1 : 0;
    const zero = y(Math.max(lo, Math.min(0, hi)));
    marks = clean.map((v, i) => {
      if (v === null) return null;
      const p = y(v);
      const up = invert ? p > zero : p < zero;
      const yTop = Math.min(p, zero);
      const h = Math.max(1, Math.abs(zero - p));
      return (
        <path
          key={i}
          className="mn-sparkline__bar"
          d={barPath(
            i * band + gap / 2,
            yTop,
            Math.max(1, band - gap),
            h,
            1,
            up ? 'top' : 'bottom',
          )}
        />
      );
    });
  } else {
    const step = count > 1 ? (width - PAD * 2) / (count - 1) : 0;
    const pts: (Point | null)[] = clean.map((v, i) =>
      v === null ? null : { x: PAD + i * step, y: y(v) },
    );
    const lastIndex = clean.reduce<number>((acc, v, i) => (v === null ? acc : i), -1);
    const dots: Point[] =
      markers === 'all'
        ? pts.filter((p): p is Point => p !== null)
        : [
            ...isolatedPoints(pts),
            ...(markers === 'last' && lastIndex >= 0
              ? [pts[lastIndex]].filter((p): p is Point => Boolean(p))
              : []),
          ];
    const baseline = invert ? top : bottom;
    marks = (
      <>
        {variant === 'area' && <path className="mn-sparkline__area" d={areaPath(pts, baseline)} />}
        <path className="mn-sparkline__line" d={linePath(pts)} />
        {(!responsive || measured !== undefined) &&
          dots.map((p, i) => (
            <circle key={i} className="mn-sparkline__dot" cx={p.x} cy={p.y} r={2} />
          ))}
      </>
    );
  }

  const status =
    thresholdTone === 'danger'
      ? (labels?.danger ?? 'Critical')
      : thresholdTone === 'warning'
        ? (labels?.warning ?? 'Warning')
        : undefined;
  const text = label
    ? (summary ??
      [
        (
          labels?.summary ??
          ((l: string, last: string, min: string, max: string) =>
            `${l}: latest ${last}, low ${min}, high ${max}`)
        )(label, fmt(stats.last), fmt(stats.min), fmt(stats.max)),
        status,
      ]
        .filter(Boolean)
        .join('. '))
    : undefined;

  return (
    <span
      {...rootProps}
      {...(text ? { role: 'img', 'aria-label': text } : { 'aria-hidden': true })}
    >
      <svg
        className="mn-sparkline__svg"
        width={responsive ? '100%' : width}
        height={height}
        viewBox={`0 0 ${width} ${height}`}
        preserveAspectRatio={responsive && measured === undefined ? 'none' : undefined}
        aria-hidden="true"
        focusable="false"
      >
        {marks}
      </svg>
    </span>
  );
});
