import { forwardRef, type CSSProperties, type ReactNode } from 'react';
import {
  ChartLegend,
  colorStyle,
  createValueFormatter,
  defaultChartColor,
  type ChartColor,
  type ChartTableData,
  type ValueFormatter,
} from '../../../internal/charts/index.js';
import { formatShare } from '../../../internal/format-number.js';
import { ChartFrame, type ChartFrameLabels, type ChartFrameProps } from '../chart-frame/index.js';

/** One slice of the donut. */
export interface DonutChartSegment {
  /** Stable id (React key, legend key). */
  key: string;
  /** Name in the legend, table and summary. */
  label: string;
  /** Amount; negative and non-finite values count as 0. */
  value: number;
  /** Palette slot. Default: the position after sorting (1, 2, …). Pin it so colours follow the entity. */
  color?: ChartColor | undefined;
}

/** Words the donut adds to ChartFrame's; pass translations here. */
export interface DonutChartLabels extends ChartFrameLabels {
  /** Name of the folded segment. Default "Other". */
  other?: string | undefined;
  /** Table column for segment names. Default "Segment". */
  segment?: string | undefined;
  /** Table column for values. Default "Value". */
  value?: string | undefined;
  /** Table column for shares. Default "Share". */
  share?: string | undefined;
  /** Summary prefix for the total. Default "Total". */
  total?: string | undefined;
}

export interface DonutChartProps extends Omit<
  ChartFrameProps,
  'children' | 'legend' | 'legendMarker' | 'onLegendToggle' | 'table' | 'labels' | 'height'
> {
  segments: readonly DonutChartSegment[];
  /** `desc` (default) orders slices largest first; `none` keeps the given order. */
  sort?: 'desc' | 'none' | undefined;
  /**
   * Most slices drawn. Beyond it the smallest fold into one neutral "Other"
   * slice (the table still lists every segment). Default 6: the five palette
   * colours plus Other.
   */
  maxSegments?: number | undefined;
  /** Outer diameter in px. Default 160. */
  size?: number | undefined;
  /** Ring thickness in px. Default 22% of `size`. */
  thickness?: number | undefined;
  /** Big text in the middle. Default: the formatted total. Pass `null` for none. */
  centerValue?: ReactNode | undefined;
  /** Small text under `centerValue`, e.g. "assets". */
  centerLabel?: ReactNode | undefined;
  /** What each legend entry shows after its label. Default `both` ("1,204 · 34%"). */
  legendContent?: 'value' | 'percent' | 'both' | 'none' | undefined;
  /** Show the legend beside the donut. Default `true`. */
  showLegend?: boolean | undefined;
  /** BCP 47 locale for numbers. Pass it when rendering on the server. */
  locale?: string | undefined;
  /** `Intl.NumberFormat` options for values. */
  valueFormat?: Intl.NumberFormatOptions | undefined;
  /** Formats values. Wins over `valueFormat`. */
  formatValue?: ValueFormatter | undefined;
  /** Decimal places in shares. Default 0 (shares under 1% read "<1%"). */
  percentDigits?: number | undefined;
  /** Replaces the generated text summary (the donut's accessible name). */
  summary?: string | undefined;
  labels?: DonutChartLabels | undefined;
}

const GAP = 2;

/**
 * A donut of parts of one whole, inside ChartFrame: palette slices, a total
 * in the middle, a legend of values and shares, folding into "Other", and
 * the data table ("View as table") with every segment.
 */
export const DonutChart = forwardRef<HTMLElement, DonutChartProps>(function DonutChart(
  {
    segments,
    sort = 'desc',
    maxSegments = 6,
    size = 160,
    thickness: thicknessProp,
    centerValue,
    centerLabel,
    legendContent = 'both',
    showLegend = true,
    locale,
    valueFormat,
    formatValue,
    percentDigits = 0,
    summary,
    labels,
    status = 'ready',
    ...frame
  },
  ref,
) {
  const words = {
    other: labels?.other ?? 'Other',
    segment: labels?.segment ?? 'Segment',
    value: labels?.value ?? 'Value',
    share: labels?.share ?? 'Share',
    total: labels?.total ?? 'Total',
  };
  const fmt = createValueFormatter(locale, valueFormat, formatValue);
  const clean = segments.map((s) => ({
    ...s,
    value: Number.isFinite(s.value) && s.value > 0 ? s.value : 0,
  }));
  const ordered =
    sort === 'desc'
      ? clean
          .map((s, i) => ({ s, i }))
          .sort((a, b) => b.s.value - a.s.value || a.i - b.i)
          .map(({ s }) => s)
      : clean;
  const total = ordered.reduce((t, s) => t + s.value, 0);
  const share = (v: number) =>
    formatShare(v, total, {
      ...(locale !== undefined && { locale }),
      maximumFractionDigits: percentDigits,
    });

  const colored = ordered.map((s, i) => ({ ...s, color: s.color ?? defaultChartColor(i) }));
  const limit = Math.max(2, Math.floor(maxSegments));
  const slices: { key: string; label: string; value: number; color: ChartColor }[] =
    colored.length > limit
      ? [
          ...colored.slice(0, limit - 1),
          {
            key: '__other',
            label: words.other,
            value: colored.slice(limit - 1).reduce((t, s) => t + s.value, 0),
            color: 'neutral',
          },
        ]
      : colored;
  const drawn = slices.filter((s) => s.value > 0);

  const thickness = Math.min(size / 2, Math.max(4, thicknessProp ?? Math.round(size * 0.22)));
  const r = (size - thickness) / 2;
  const c = 2 * Math.PI * r;
  let start = 0;
  const arcs = drawn.map((s) => {
    const len = (s.value / total) * c;
    const gap = drawn.length > 1 ? GAP : 0;
    // Tiny shares keep a visible sliver instead of vanishing into the gap.
    const dash = Math.max(len - gap, Math.min(len, 1.5));
    const arc = { ...s, dash, offset: start };
    start += len;
    return arc;
  });

  const describe = (name: string, v: number) => {
    if (legendContent === 'none') return name;
    if (legendContent === 'value') return `${name} ${fmt(v)}`;
    if (legendContent === 'percent') return `${name} ${share(v)}`;
    return `${name} ${fmt(v)} · ${share(v)}`;
  };

  const generated = `${words.total} ${fmt(total)}. ${ordered
    .filter((s) => s.value > 0)
    .map((s) => `${s.label} ${fmt(s.value)} (${share(s.value)})`)
    .join(', ')}`;

  const table: ChartTableData = {
    columns: [
      { key: 'label', label: words.segment },
      { key: 'value', label: words.value },
      { key: 'share', label: words.share },
    ],
    rows: [
      ...ordered.map((s) => ({ label: s.label, value: fmt(s.value), share: share(s.value) })),
      { label: words.total, value: fmt(total), share: share(total) },
    ],
  };

  const resolvedStatus = status === 'ready' && !(total > 0) ? 'empty' : status;
  const center = centerValue === undefined ? fmt(total) : centerValue;

  return (
    <ChartFrame
      ref={ref}
      {...frame}
      status={resolvedStatus}
      height={size}
      table={table}
      labels={labels}
      data-chart="donut"
    >
      <div className="mn-donut-chart" data-legend={showLegend ? '' : undefined}>
        <div
          className="mn-chart__plot mn-donut-chart__plot"
          role="img"
          aria-label={summary ?? generated}
          style={{ width: size, height: size } as CSSProperties}
        >
          <svg
            className="mn-donut-chart__svg"
            viewBox={`0 0 ${size} ${size}`}
            width={size}
            height={size}
            aria-hidden="true"
            focusable="false"
          >
            <circle
              className="mn-donut-chart__track"
              cx={size / 2}
              cy={size / 2}
              r={r}
              strokeWidth={thickness}
            />
            <g transform={`rotate(-90 ${size / 2} ${size / 2})`}>
              {arcs.map((a) => (
                <circle
                  key={a.key}
                  className="mn-donut-chart__segment"
                  cx={size / 2}
                  cy={size / 2}
                  r={r}
                  strokeWidth={thickness}
                  strokeDasharray={`${a.dash} ${c - a.dash}`}
                  strokeDashoffset={-a.offset}
                  style={colorStyle(a.color)}
                >
                  <title>{`${a.label}: ${fmt(a.value)} (${share(a.value)})`}</title>
                </circle>
              ))}
            </g>
          </svg>
          {center !== null || centerLabel ? (
            <div className="mn-donut-chart__center" aria-hidden="true">
              {center !== null ? <span className="mn-donut-chart__value">{center}</span> : null}
              {centerLabel ? <span className="mn-donut-chart__label">{centerLabel}</span> : null}
            </div>
          ) : null}
        </div>
        {showLegend ? (
          <ChartLegend
            items={slices
              .filter((s) => s.value > 0)
              .map((s) => ({ key: s.key, label: describe(s.label, s.value), color: s.color }))}
            marker="square"
            label={labels?.legend ?? 'Legend'}
            className="mn-donut-chart__legend"
          />
        ) : null}
      </div>
    </ChartFrame>
  );
});
