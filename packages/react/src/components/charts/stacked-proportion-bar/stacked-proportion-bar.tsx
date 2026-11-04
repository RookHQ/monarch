import { forwardRef, type CSSProperties, type HTMLAttributes } from 'react';
import {
  ChartLegend,
  chartColor,
  createValueFormatter,
  defaultChartColor,
  type ChartColor,
  type ValueFormatter,
} from '../../../internal/charts/index.js';
import { cx } from '../../../internal/cx.js';
import { formatShare } from '../../../internal/format-number.js';

export { formatShare, type FormatShareOptions } from '../../../internal/format-number.js';

/** Status intent for a segment that means a state (operational, degraded, down). */
export type ProportionTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

interface ProportionSegmentBase {
  /** Stable id (React key, legend key). */
  key: string;
  /** Name shown in the legend and the accessible list, e.g. "Replied". Required: colour never carries the meaning alone. */
  label: string;
  /** Amount in the same unit as the other segments. */
  value: number;
}

/** A part coloured from the categorical data palette (the default). */
export interface ProportionSegment extends ProportionSegmentBase {
  /** Palette slot. Default: the segment's position (1, 2, …); pin it so colours follow the entity. */
  color?: ChartColor | undefined;
  tone?: never;
}

/**
 * A part that means a status, coloured by its intent token. Use only when
 * every segment is a state (status mode); never mix with palette segments.
 */
export interface ProportionStatusSegment extends ProportionSegmentBase {
  /** Status intent: `success`, `warning`, `danger`, `info` or `neutral` (no data, paused). */
  tone: ProportionTone;
  color?: never;
}

const TONE_COLOR: Record<ProportionTone, string> = {
  success: 'var(--mn-color-success-solid)',
  warning: 'var(--mn-color-warning-solid)',
  danger: 'var(--mn-color-danger-solid)',
  info: 'var(--mn-color-info-solid)',
  neutral: 'var(--mn-color-text-subtle)',
};

function isDev() {
  const env = (globalThis as { process?: { env?: Record<string, string | undefined> } }).process
    ?.env;
  return env?.NODE_ENV !== 'production';
}

/** What each legend entry shows after its label. */
export type ProportionLegendContent = 'value' | 'percent' | 'both' | 'none';

export interface StackedProportionBarProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children'
> {
  /**
   * The parts, in display order. Negative values are treated as 0. Either
   * every segment uses the data palette (`color`, or none), or every segment
   * has a status `tone` (status mode); the two never mix.
   */
  segments: readonly ProportionSegment[] | readonly ProportionStatusSegment[];
  /** Names the legend list (the accessible version of the bar), e.g. "Lead pipeline" or "Storage". */
  label: string;
  /**
   * The size of the whole. Default: the sum of the segments. Set it larger
   * for a capacity (storage, quota); the rest is drawn as empty track.
   */
  total?: number | undefined;
  /** Legend name for the empty remainder when `total` exceeds the sum, e.g. "Free". Omit to leave it out of the legend. */
  remainderLabel?: string | undefined;
  /** What the legend shows after each label. Default `both` ("1,204 · 34%"). */
  legendContent?: ProportionLegendContent | undefined;
  /**
   * `true` (default) shows the legend; `false` keeps it for assistive
   * technology only, visually hidden.
   */
  legend?: boolean | undefined;
  /** Leave segments with a value of 0 out of the bar and legend. Default `true`. */
  hideZero?: boolean | undefined;
  /** Narrowest a non-zero segment is drawn, so tiny shares stay visible. Default `4px`. */
  minSegmentWidth?: string | undefined;
  /** Bar thickness: `sm` 4px, `md` (default) 8px, `lg` 12px. */
  size?: 'sm' | 'md' | 'lg' | undefined;
  /** BCP 47 locale for values and percentages. */
  locale?: string | undefined;
  /** `Intl.NumberFormat` options for values. */
  valueFormat?: Intl.NumberFormatOptions | undefined;
  /** Formats values. Wins over `valueFormat`. */
  formatValue?: ValueFormatter | undefined;
  /** Decimal places in percentages. Default 0 (shares under 1% read "<1%"). */
  percentDigits?: number | undefined;
  /** Shown instead of the legend when every value is 0. Default "No data yet". */
  emptyLabel?: string | undefined;
}

/**
 * One horizontal bar split into proportions (pipeline stages, storage by
 * type, monitors by state) with a legend of values and shares. The bar is
 * decorative; the legend is a real list, so the data never depends on colour.
 */
export const StackedProportionBar = forwardRef<HTMLDivElement, StackedProportionBarProps>(
  function StackedProportionBar(
    {
      segments,
      label,
      total: totalProp,
      remainderLabel,
      legendContent = 'both',
      legend = true,
      hideZero = true,
      minSegmentWidth,
      size = 'md',
      locale,
      valueFormat,
      formatValue,
      percentDigits = 0,
      emptyLabel = 'No data yet',
      className,
      style,
      ...rest
    },
    ref,
  ) {
    const fmt = createValueFormatter(locale, valueFormat, formatValue);
    const list = segments as readonly (ProportionSegmentBase & {
      color?: ChartColor | undefined;
      tone?: ProportionTone | undefined;
    })[];
    const toned = list.filter((s) => s.tone !== undefined).length;
    const statusMode = toned > 0 && toned === list.length;
    if (toned > 0 && !statusMode && isDev()) {
      console.warn(
        `StackedProportionBar "${label}": mix of status tones and palette colours. Give every segment a tone (status mode) or none; tones are ignored.`,
      );
    }
    const parts = list.map((s, i) => ({
      ...s,
      value: Number.isFinite(s.value) && s.value > 0 ? s.value : 0,
      fill: statusMode && s.tone ? TONE_COLOR[s.tone] : chartColor(s.color ?? defaultChartColor(i)),
      color: s.color ?? defaultChartColor(i),
    }));
    const sum = parts.reduce((t, s) => t + s.value, 0);
    const total = Math.max(sum, totalProp ?? sum);
    const remainder = total - sum;
    const shown = hideZero ? parts.filter((s) => s.value > 0) : parts;
    const share = (v: number) =>
      formatShare(v, total, {
        ...(locale !== undefined && { locale }),
        maximumFractionDigits: percentDigits,
      });
    const describe = (name: string, v: number) => {
      if (legendContent === 'none') return name;
      if (legendContent === 'value') return `${name} ${fmt(v)}`;
      if (legendContent === 'percent') return `${name} ${share(v)}`;
      return `${name} ${fmt(v)} · ${share(v)}`;
    };

    const items: { key: string; label: string; color: ChartColor; fill: string }[] = shown.map(
      (s) => ({ key: s.key, label: describe(s.label, s.value), color: s.color, fill: s.fill }),
    );
    if (remainder > 0 && remainderLabel) {
      items.push({
        key: '__remainder',
        label: describe(remainderLabel, remainder),
        color: 'neutral',
        fill: 'var(--mn-color-bg-muted)',
      });
    }

    const vars: Record<string, string> = {};
    if (minSegmentWidth !== undefined) vars['--mn-proportion-min'] = minSegmentWidth;

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-stacked-proportion-bar', className)}
        data-size={size}
        data-mode={statusMode ? 'status' : undefined}
        data-empty={total > 0 ? undefined : ''}
        data-remainder={remainder > 0 && remainderLabel ? '' : undefined}
        style={{ ...(vars as CSSProperties), ...style }}
      >
        <div className="mn-stacked-proportion-bar__track" aria-hidden="true">
          {shown
            .filter((s) => s.value > 0)
            .map((s) => (
              <span
                key={s.key}
                className="mn-stacked-proportion-bar__segment"
                data-tone={statusMode ? s.tone : undefined}
                style={{ ['--_c' as string]: s.fill, flexGrow: s.value } as CSSProperties}
                title={`${s.label}: ${fmt(s.value)} (${share(s.value)})`}
              />
            ))}
          {remainder > 0 ? (
            <span
              className="mn-stacked-proportion-bar__remainder"
              style={{ flexGrow: remainder }}
              title={remainderLabel ? `${remainderLabel}: ${fmt(remainder)}` : undefined}
            />
          ) : null}
        </div>
        {items.length > 0 && statusMode ? (
          <ul
            className={cx(
              'mn-chart__legend mn-stacked-proportion-bar__legend',
              legend ? undefined : 'mn-visually-hidden',
            )}
            aria-label={label}
          >
            {items.map((item) => (
              <li key={item.key} className="mn-chart__legend-item">
                <span
                  className="mn-chart__swatch"
                  data-marker="square"
                  aria-hidden="true"
                  style={{ ['--_c' as string]: item.fill } as CSSProperties}
                />
                <span className="mn-chart__legend-label">{item.label}</span>
              </li>
            ))}
          </ul>
        ) : items.length > 0 ? (
          <ChartLegend
            items={items}
            marker="square"
            label={label}
            className={cx(
              'mn-stacked-proportion-bar__legend',
              legend ? undefined : 'mn-visually-hidden',
            )}
          />
        ) : (
          <p className="mn-stacked-proportion-bar__empty">
            <span className="mn-visually-hidden">{label}: </span>
            {emptyLabel}
          </p>
        )}
      </div>
    );
  },
);
