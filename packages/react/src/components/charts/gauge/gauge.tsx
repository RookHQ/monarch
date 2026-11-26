'use client';

import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { createValueFormatter, type ValueFormatter } from '../../../internal/charts/index.js';
import { useClientLocale } from '../../../internal/dates/client-time.js';

/** Colour of a band or of the value arc. Status tones carry meaning; `accent` and `neutral` do not. */
export type GaugeTone = 'neutral' | 'accent' | 'info' | 'success' | 'warning' | 'danger';

/** A stretch of the scale with a meaning, such as "Healthy" from 0 to 70. */
export interface GaugeBand {
  /** Start of the band (inclusive). */
  from: number;
  /** End of the band (exclusive, except at `max`). */
  to: number;
  tone: GaugeTone;
  /** Name of the band, added to the spoken value ("72%, Healthy") and shown under the value. */
  label?: string | undefined;
}

/** `arc` fills the track to the value; `needle` draws the bands in full and points at the value. */
export type GaugeIndicator = 'arc' | 'needle';

export interface GaugeMessages {
  /** Accessible name when there is no `label` or `aria-label`. Default "Gauge". */
  name: string;
  /** Spoken value: "{value}, {band}" or "{value} (from {min} to {max})". */
  valueText: (value: string, min: string, max: string, band: string | undefined) => string;
  /** Shown and spoken when `value` is null. Default "No data". */
  noValue: string;
  /** Spoken name of the target marker. Default "Target {value}". */
  target: (value: string) => string;
}

const DEFAULT_MESSAGES: GaugeMessages = {
  name: 'Gauge',
  valueText: (value, min, max, band) =>
    band ? `${value}, ${band}` : `${value} (from ${min} to ${max})`,
  noValue: 'No data',
  target: (value) => `Target ${value}`,
};

export interface GaugeProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The reading. `null` shows an empty track and a dash. Values outside `min`…`max` are clamped for drawing; the text shows the real value. */
  value: number | null;
  /** Start of the scale. Default 0. */
  min?: number | undefined;
  /** End of the scale. Default 100. */
  max?: number | undefined;
  /** Meaningful stretches of the scale (good, at risk, critical), drawn along the arc. */
  bands?: readonly GaugeBand[] | undefined;
  /**
   * Colour of the value arc or needle. `auto` (default) takes the tone of the
   * band the value falls in, or `accent` without bands. Exposed as `data-tone`.
   */
  tone?: GaugeTone | 'auto' | undefined;
  /** `arc` (default) or `needle`. Exposed as `data-indicator`. */
  indicator?: GaugeIndicator | undefined;
  /** A target or threshold on the scale, marked with a tick across the track. */
  target?: number | undefined;
  /** Visible label under the gauge; also its accessible name. */
  label?: ReactNode | undefined;
  /** Secondary line under the label, such as the period or the unit. */
  description?: ReactNode | undefined;
  /** Largest width in px; the gauge shrinks with its container. Default 200. */
  size?: number | undefined;
  /** Track thickness as a share of `size`. Default 0.12. */
  thickness?: number | undefined;
  /** Print `min` and `max` under the ends of the arc. Default `true`. */
  showRange?: boolean | undefined;
  /** BCP 47 locale for numbers. Pass it when rendering on the server. */
  locale?: string | undefined;
  /** `Intl.NumberFormat` options for the value, range and target, e.g. `{ style: 'percent' }`. */
  valueFormat?: Intl.NumberFormatOptions | undefined;
  /** Formats every number. Wins over `valueFormat`. */
  formatValue?: ValueFormatter | undefined;
  /** Interface text, for translation. */
  messages?: Partial<GaugeMessages> | undefined;
}

function bandAt(bands: readonly GaugeBand[], value: number, max: number): GaugeBand | undefined {
  return bands.find((b) => value >= b.from && (value < b.to || (b.to >= max && value <= b.to)));
}

function r2(n: number): number {
  return Math.round(n * 100) / 100;
}

/**
 * A semi-circular meter for one reading against a scale: utilisation, a
 * health score, a quota used. Bands give the scale meaning, an optional
 * target is marked, and the value is written as text in the middle, so it
 * never relies on the arc or on colour alone. Exposed as a `meter`.
 */
export const Gauge = forwardRef<HTMLDivElement, GaugeProps>(function Gauge(
  {
    value,
    min = 0,
    max = 100,
    bands = [],
    tone = 'auto',
    indicator = 'arc',
    target,
    label,
    description,
    size = 200,
    thickness = 0.12,
    showRange = true,
    locale: localeProp,
    valueFormat,
    formatValue,
    messages,
    className,
    style,
    ...rest
  },
  ref,
) {
  const locale = useClientLocale(localeProp);
  const m = { ...DEFAULT_MESSAGES, ...messages };
  const id = useId();
  const fmt = createValueFormatter(locale, valueFormat, formatValue);
  const lo = Math.min(min, max);
  const hi = max > min ? max : min + 1;
  const has = value !== null && Number.isFinite(value);
  const clamped = has ? Math.min(hi, Math.max(lo, value)) : lo;
  const share = (v: number) => (Math.min(hi, Math.max(lo, v)) - lo) / (hi - lo);
  const band = has ? bandAt(bands, value, hi) : undefined;
  const resolvedTone: GaugeTone =
    tone === 'auto'
      ? has
        ? (band?.tone ?? (bands.length ? 'neutral' : 'accent'))
        : 'neutral'
      : tone;

  // Geometry in a fixed 200-unit coordinate system; the SVG scales to `size`.
  const W = 200;
  const stroke = Math.max(4, Math.min(48, W * thickness));
  const ox = W / 2;
  const oy = W / 2;
  const r = (W - stroke) / 2;
  const rangeRoom = showRange ? 16 : 4;
  const H = oy + rangeRoom;
  const at = (t: number, radius = r) => {
    const a = Math.PI * (1 - t);
    return { x: r2(ox + radius * Math.cos(a)), y: r2(oy - radius * Math.sin(a)) };
  };
  const arc = (t0: number, t1: number, radius = r) => {
    if (t1 - t0 <= 0) return '';
    const p0 = at(t0, radius);
    const p1 = at(t1, radius);
    return `M${p0.x},${p0.y}A${r2(radius)},${r2(radius)} 0 0 1 ${p1.x},${p1.y}`;
  };

  const text = has ? fmt(value) : '–';
  const valueText = has ? m.valueText(text, fmt(lo), fmt(hi), band?.label) : m.noValue;
  const named = rest['aria-label'] !== undefined || rest['aria-labelledby'] !== undefined;
  const nameProps = named ? {} : { 'aria-labelledby': label ? `${id}-label` : `${id}-name` };
  const describedBy =
    [
      // An empty gauge is an image: its "no data" text is its description.
      has ? '' : `${id}-empty`,
      description ? `${id}-desc` : '',
      target !== undefined ? `${id}-target` : '',
    ]
      .filter(Boolean)
      .join(' ') || undefined;
  const roleProps = has
    ? {
        role: 'meter',
        'aria-valuemin': lo,
        'aria-valuemax': hi,
        'aria-valuenow': clamped,
        'aria-valuetext': valueText,
      }
    : { role: 'img' };

  const t = share(clamped);
  const needle = at(t, r - stroke / 2 - 6);
  const bandInset = indicator === 'arc' ? r + stroke / 2 + 4 : r;
  const bandStroke = indicator === 'arc' ? 3 : stroke;
  // Bands outside the arc need room around it.
  const pad = indicator === 'arc' && bands.length ? 8 : 0;

  return (
    <div
      ref={ref}
      {...roleProps}
      {...nameProps}
      aria-describedby={describedBy}
      {...rest}
      className={cx('mn-gauge', className)}
      data-tone={resolvedTone}
      data-indicator={indicator}
      data-empty={has ? undefined : ''}
      data-band={band?.label}
      style={{ ...style, ['--mn-gauge-size' as string]: `${size}px` }}
    >
      <svg
        className="mn-gauge__svg"
        viewBox={`${-pad} ${-pad} ${W + pad * 2} ${H + pad}`}
        aria-hidden="true"
        focusable="false"
      >
        <path className="mn-gauge__track" d={arc(0, 1)} strokeWidth={stroke} />
        {bands.map((b, i) => (
          <path
            key={i}
            className="mn-gauge__band"
            data-tone={b.tone}
            d={arc(share(b.from), share(b.to), bandInset)}
            strokeWidth={bandStroke}
          />
        ))}
        {indicator === 'arc' && has && t > 0 ? (
          <path className="mn-gauge__value" d={arc(0, t)} strokeWidth={stroke} />
        ) : null}
        {target !== undefined ? (
          <line
            className="mn-gauge__target"
            x1={at(share(target), r - stroke / 2 - 3).x}
            y1={at(share(target), r - stroke / 2 - 3).y}
            x2={at(share(target), r + stroke / 2 + 3).x}
            y2={at(share(target), r + stroke / 2 + 3).y}
          />
        ) : null}
        {indicator === 'needle' && has ? (
          <g className="mn-gauge__needle">
            <line x1={ox} y1={oy} x2={needle.x} y2={needle.y} />
            <circle cx={ox} cy={oy} r={6} />
          </g>
        ) : null}
        <text
          className="mn-gauge__text"
          x={ox}
          y={indicator === 'needle' ? oy - 20 : oy - 6}
          textAnchor="middle"
        >
          {text}
        </text>
        {showRange ? (
          <>
            <text className="mn-gauge__range" x={stroke / 2} y={oy + 14} textAnchor="middle">
              {fmt(lo)}
            </text>
            <text className="mn-gauge__range" x={W - stroke / 2} y={oy + 14} textAnchor="middle">
              {fmt(hi)}
            </text>
          </>
        ) : null}
      </svg>
      {band?.label ? (
        <span className="mn-gauge__band-label" aria-hidden="true">
          {band.label}
        </span>
      ) : null}
      {label ? (
        <span className="mn-gauge__label" id={`${id}-label`}>
          {label}
        </span>
      ) : (
        <span className="mn-visually-hidden" id={`${id}-name`}>
          {m.name}
        </span>
      )}
      {description ? (
        <span className="mn-gauge__description" id={`${id}-desc`}>
          {description}
        </span>
      ) : null}
      {has ? null : (
        <span className="mn-visually-hidden" id={`${id}-empty`}>
          {m.noValue}
        </span>
      )}
      {target !== undefined ? (
        <span className="mn-visually-hidden" id={`${id}-target`}>
          {m.target(fmt(target))}
        </span>
      ) : null}
    </div>
  );
});
