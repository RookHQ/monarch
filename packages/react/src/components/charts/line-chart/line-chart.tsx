'use client';

import { forwardRef, useCallback, useId, useMemo } from 'react';
import {
  CategoryAxis,
  ChartLiveRegion,
  ChartTooltip,
  DEFAULT_INTERACTION_LABELS,
  DEFAULT_SUMMARY_LABELS,
  ReferenceLine,
  ValueAxis,
  areaPath,
  colorStyle,
  createTickFormatter,
  createValueFormatter,
  createXFormatter,
  defaultChartColor,
  describeTrend,
  extent,
  isolatedPoints,
  labelIndices,
  linePath,
  linearScale,
  nearestIndex,
  numberAt,
  tickCountFor,
  useChartPointer,
  useElementWidth,
  useHiddenSeries,
  valueAxisLayout,
  type ChartColor,
  type ChartDatum,
  type ChartDomainBound,
  type ChartInteractionLabels,
  type ChartReferenceLine,
  type ChartSeries,
  type ChartSummaryLabels,
  type ChartTableData,
  type Curve,
  type Point,
  type ValueFormatter,
  type XValue,
} from '../../../internal/charts/index.js';
import { ChartFrame, type ChartFrameLabels, type ChartFrameProps } from '../chart-frame/index.js';

export type {
  ChartReferenceLine,
  ChartDomainBound,
  ChartSummaryLabels,
  ChartInteractionLabels,
} from '../../../internal/charts/index.js';

export type LineChartVariant = 'line' | 'area';
export type LineChartCurve = Curve;
export type ChartXScale = 'category' | 'time';

export type LineChartLabels = ChartFrameLabels &
  Partial<ChartInteractionLabels> &
  Partial<ChartSummaryLabels>;

export interface LineChartProps extends Omit<
  ChartFrameProps,
  'children' | 'legend' | 'legendMarker' | 'onLegendToggle' | 'table' | 'labels'
> {
  /** Rows in x order. Each has the `xKey` field and one number per series; a missing or `null` value is a gap. */
  data: readonly ChartDatum[];
  /** Field holding the x value: a `Date`, a number or a category label. */
  xKey: string;
  /** Column header for the x values in the data table. Default: `xKey`. */
  xLabel?: string | undefined;
  /** The measures to draw. Two to five read best; colours follow the order unless pinned. */
  series: readonly ChartSeries[];
  /** `line` (default) or `area`: the line plus a soft fill to the baseline. Exposed as `data-variant`. */
  variant?: LineChartVariant | undefined;
  /** `linear` (default) joins points with straight lines; `monotone` smooths without overshooting. */
  curve?: LineChartCurve | undefined;
  /**
   * `time` spaces points by their date or number (gaps in time show); `category`
   * spaces them evenly. Default: `time` when every x is a Date or number.
   */
  xScale?: ChartXScale | undefined;
  /** BCP 47 locale for numbers and dates. Pass it when rendering on the server. */
  locale?: string | undefined;
  /** `Intl.NumberFormat` options for values in the tooltip, table and summary. */
  valueFormat?: Intl.NumberFormatOptions | undefined;
  /** Formats values in the tooltip, table and summary. Wins over `valueFormat`. */
  formatValue?: ValueFormatter | undefined;
  /** Formats value-axis ticks. Default: compact notation of `valueFormat` (1.2K). */
  formatTick?: ValueFormatter | undefined;
  /** `Intl.DateTimeFormat` options for date x values. Default `{ month: 'short', day: 'numeric' }`. */
  xFormat?: Intl.DateTimeFormatOptions | undefined;
  /** Formats x values everywhere (axis, tooltip, table, summary). Wins over `xFormat`. */
  formatX?: ((value: XValue, index: number) => string) | undefined;
  /** Keep zero on the value axis. Default `true`; turn off for values far from zero. */
  includeZero?: boolean | undefined;
  /** Fix either end of the value axis, e.g. `[0, 100]` for percentages. */
  yDomain?: readonly [ChartDomainBound, ChartDomainBound] | undefined;
  /** Dashed horizontal lines for targets, baselines or SLAs. */
  referenceLines?: readonly ChartReferenceLine[] | undefined;
  /** Draw a dot on every point. Default `false` (lone points between gaps always get one). */
  dots?: boolean | undefined;
  /** Controlled list of hidden series keys. */
  hiddenSeries?: readonly string[] | undefined;
  /** Uncontrolled initial hidden series. */
  defaultHiddenSeries?: readonly string[] | undefined;
  onHiddenSeriesChange?: ((hidden: string[]) => void) | undefined;
  /** Legend entries toggle their series. Default `true` (legend shows for two or more series). */
  legendToggle?: boolean | undefined;
  /**
   * Controlled inspected index (`null` for none), for a crosshair shared by
   * charts on the same x values. Leave undefined for uncontrolled.
   */
  activeIndex?: number | null | undefined;
  onActiveIndexChange?: ((index: number | null) => void) | undefined;
  /** Click, Enter or Space on the inspected point, for drill-in. */
  onPointSelect?: ((datum: ChartDatum, index: number) => void) | undefined;
  /** Replaces the generated text summary (the plot's accessible name). */
  summary?: string | undefined;
  labels?: LineChartLabels | undefined;
}

const MARGIN = { top: 8, right: 12, bottom: 24 };
const FALLBACK_WIDTH = 640;

function isContinuous(v: unknown): v is Date | number {
  return v instanceof Date || (typeof v === 'number' && Number.isFinite(v));
}

/**
 * Line and area chart for trends over time or ordered categories, with
 * multiple series, gaps, reference lines, a shared crosshair tooltip on
 * hover, touch and keyboard, legend toggles and an accessible data table.
 */
export const LineChart = forwardRef<HTMLElement, LineChartProps>(function LineChart(
  {
    data,
    xKey,
    xLabel,
    series,
    variant = 'line',
    curve = 'linear',
    xScale,
    locale,
    valueFormat,
    formatValue,
    formatTick,
    xFormat,
    formatX,
    includeZero = true,
    yDomain,
    referenceLines = [],
    dots = false,
    hiddenSeries,
    defaultHiddenSeries,
    onHiddenSeriesChange,
    legendToggle = true,
    activeIndex,
    onActiveIndexChange,
    onPointSelect,
    summary,
    labels,
    height = 240,
    status = 'ready',
    title,
    ...frame
  },
  ref,
) {
  const hintId = useId();
  const il = { ...DEFAULT_INTERACTION_LABELS, ...labels };
  const sl = { ...DEFAULT_SUMMARY_LABELS, ...labels };
  const [hidden, toggle] = useHiddenSeries(hiddenSeries, defaultHiddenSeries, onHiddenSeriesChange);
  const [measureRef, measured] = useElementWidth<HTMLDivElement>();
  const width = measured ?? FALLBACK_WIDTH;

  const fmtValue = createValueFormatter(locale, valueFormat, formatValue);
  const fmtTick = createTickFormatter(locale, valueFormat, formatTick);
  const fmtX = createXFormatter(locale, xFormat, formatX);

  const colored = useMemo(
    () => series.map((s, i) => ({ ...s, color: (s.color ?? defaultChartColor(i)) as ChartColor })),
    [series],
  );
  const visible = colored.filter((s) => !hidden.includes(s.key));
  const n = data.length;
  const xs = data.map((d) => d[xKey]);
  const continuous = (xScale ?? (xs.every(isContinuous) ? 'time' : 'category')) === 'time';
  const xLabels = xs.map((v, i) => fmtX(v as XValue, i));
  const values = (key: string) => data.map((d) => numberAt(d, key));

  // Value axis.
  const plotH = Math.max(40, height - MARGIN.top - MARGIN.bottom);
  const ext = extent([
    ...visible.flatMap((s) => values(s.key)),
    ...referenceLines.map((r) => r.value),
  ]);
  const axis = valueAxisLayout(ext, {
    includeZero,
    domain: yDomain,
    tickCount: tickCountFor(plotH),
    format: fmtTick,
  });
  const left = axis.labelWidth + 8;
  const x0 = left;
  const x1 = Math.max(x0 + 1, width - MARGIN.right);
  const y0 = MARGIN.top;
  const y1 = MARGIN.top + plotH;
  const y = linearScale(axis.domain, [y1, y0]);

  // X positions.
  const positions = useMemo(() => {
    if (n === 0) return [];
    if (n === 1) return [(x0 + x1) / 2];
    if (continuous) {
      const t = xs.map((v) => (v instanceof Date ? v.getTime() : Number(v)));
      const lo = Math.min(...t);
      const hi = Math.max(...t);
      const sx = linearScale([lo, hi === lo ? lo + 1 : hi], [x0, x1]);
      return t.map(sx);
    }
    return xs.map((_, i) => x0 + (i / (n - 1)) * (x1 - x0));
  }, [n, continuous, xs, x0, x1]);

  const indexAt = useCallback((px: number) => nearestIndex(positions, px), [positions]);
  const { active, source, plotProps } = useChartPointer({
    count: status === 'ready' ? n : 0,
    activeIndex,
    onActiveIndexChange,
    indexAt,
    onSelect: onPointSelect ? (i) => onPointSelect(data[i] ?? {}, i) : undefined,
  });

  const pointsFor = (key: string): (Point | null)[] =>
    data.map((d, i) => {
      const v = numberAt(d, key);
      return v === null ? null : { x: positions[i] ?? 0, y: y(v) };
    });
  const baseline = y(Math.max(axis.domain[0], Math.min(0, axis.domain[1])));

  const ticks = labelIndices(n, x1 - x0).map((i) => ({
    key: i,
    at: positions[i] ?? 0,
    label: xLabels[i] ?? '',
    anchor: (n > 1 && i === 0 ? 'start' : n > 1 && i === n - 1 ? 'end' : 'middle') as
      'start' | 'end' | 'middle',
  }));

  const showDot = (v: number | null) => v !== null;
  const describe = (i: number) =>
    `${xLabels[i]}: ${visible
      .map((s) => {
        const v = numberAt(data[i], s.key);
        return `${s.label} ${v === null ? il.noValue : fmtValue(v)}`;
      })
      .join(', ')}`;

  const generated =
    n === 0
      ? sl.noData
      : [
          sl.points(n, xLabels[0] ?? '', xLabels[n - 1] ?? ''),
          ...visible.map((s) => describeTrend(s.label, values(s.key), fmtValue, sl)),
          ...referenceLines.map((r) => `${r.label ?? ''} ${fmtValue(r.value)}`.trim()),
        ].join('. ');

  const table: ChartTableData = {
    columns: [
      { key: '__x', label: xLabel ?? xKey },
      ...colored.map((s) => ({ key: s.key, label: s.label })),
    ],
    rows: data.map((d, i) => {
      const row: Record<string, string> = { __x: xLabels[i] ?? '' };
      for (const s of colored) {
        const v = numberAt(d, s.key);
        row[s.key] = v === null ? il.noValue : fmtValue(v);
      }
      return row;
    }),
  };

  const legend =
    colored.length > 1
      ? colored.map((s) => ({
          key: s.key,
          label: s.label,
          color: s.color,
          dashed: s.dashed ?? false,
          hidden: hidden.includes(s.key),
        }))
      : undefined;

  const activeX = active !== null ? positions[active] : undefined;

  return (
    <ChartFrame
      ref={ref}
      {...frame}
      title={title}
      status={n === 0 && status === 'ready' ? 'empty' : status}
      height={height}
      legend={legend}
      legendMarker="line"
      onLegendToggle={legendToggle && legend ? toggle : undefined}
      table={table}
      labels={labels}
      data-chart="line"
    >
      <div
        ref={measureRef}
        className="mn-chart__plot mn-line-chart"
        data-variant={variant}
        role="group"
        aria-roledescription={il.chart}
        aria-label={summary ?? generated}
        aria-describedby={hintId}
        {...plotProps}
      >
        <svg
          className="mn-chart__svg"
          viewBox={`0 0 ${width} ${height}`}
          height={height}
          aria-hidden="true"
          focusable="false"
        >
          <ValueAxis
            ticks={axis.ticks}
            scale={y}
            format={fmtTick}
            orientation="vertical"
            from={x0}
            to={x1}
            labelAt={x0 - 8}
          />
          <CategoryAxis labels={ticks} orientation="horizontal" position={y1 + 8} />
          {variant === 'area' &&
            visible.map((s) => (
              <path
                key={`area-${s.key}`}
                className="mn-chart__area"
                style={colorStyle(s.color)}
                d={areaPath(pointsFor(s.key), baseline, curve)}
              />
            ))}
          {referenceLines.map((r, i) => (
            <ReferenceLine
              key={`ref-${i}`}
              line={r}
              scale={y}
              orientation="vertical"
              from={x0}
              to={x1}
              format={fmtTick}
            />
          ))}
          {activeX !== undefined && (
            <line className="mn-chart__crosshair" x1={activeX} x2={activeX} y1={y0} y2={y1} />
          )}
          {visible.map((s) => {
            const pts = pointsFor(s.key);
            const lone = dots ? pts.filter((p): p is Point => p !== null) : isolatedPoints(pts);
            return (
              <g key={s.key} style={colorStyle(s.color)} data-series={s.key}>
                <path
                  className="mn-chart__line"
                  data-dashed={s.dashed || undefined}
                  d={linePath(pts, curve)}
                />
                {lone.map((p, i) => (
                  <circle key={i} className="mn-chart__dot" cx={p.x} cy={p.y} r={3.5} />
                ))}
                {active !== null && showDot(numberAt(data[active], s.key)) && (
                  <circle
                    className="mn-chart__dot"
                    data-active=""
                    cx={positions[active]}
                    cy={y(numberAt(data[active], s.key) ?? 0)}
                    r={5}
                  />
                )}
              </g>
            );
          })}
        </svg>
        {active !== null && activeX !== undefined && (
          <ChartTooltip
            x={activeX}
            y={y0}
            width={width}
            title={xLabels[active]}
            rows={visible.map((s) => {
              const v = numberAt(data[active], s.key);
              return {
                key: s.key,
                label: s.label,
                value: v === null ? il.noValue : fmtValue(v),
                color: s.color,
                dashed: s.dashed ?? false,
                marker: 'line' as const,
              };
            })}
          />
        )}
        <span id={hintId} className="mn-visually-hidden">
          {il.keyboardHint}
        </span>
        <ChartLiveRegion>
          {active !== null && source === 'keyboard' ? describe(active) : ''}
        </ChartLiveRegion>
      </div>
    </ChartFrame>
  );
});
