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
  colorStyle,
  createTickFormatter,
  createValueFormatter,
  createXFormatter,
  defaultChartColor,
  describeTrend,
  extent,
  labelIndices,
  linePath,
  linearScale,
  nearestIndex,
  numberAt,
  tickCountFor,
  useChartPointer,
  useControllable,
  useElementWidth,
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
import { ChartLegend } from '../chart-legend/index.js';
import { useClientLocale, useClientZone } from '../../../internal/dates/client-time.js';
import { maxOf, minOf } from '../../../internal/math.js';

/**
 * How the areas combine: `overlap` draws each series from the baseline (they
 * overlap, translucent); `stacked` piles them so the top edge is the total;
 * `expand` stacks shares of 100% at every x.
 */
export type AreaChartStacking = 'overlap' | 'stacked' | 'expand';
export type AreaChartCurve = Curve;

export interface AreaChartTextLabels {
  /** Tooltip and announcement suffix for a share in `expand` mode: "{value} ({share})". */
  share: (value: string, share: string) => string;
}

export type AreaChartLabels = ChartFrameLabels &
  Partial<ChartInteractionLabels> &
  Partial<ChartSummaryLabels> &
  Partial<AreaChartTextLabels>;

export interface AreaChartProps extends Omit<
  ChartFrameProps,
  'children' | 'legend' | 'legendMarker' | 'onLegendToggle' | 'table' | 'labels'
> {
  /** Rows in x order. Each has the `xKey` field and one number per series; a missing or `null` value is a gap. */
  data: readonly ChartDatum[];
  /** Field holding the x value: a `Date`, a number or a category label. */
  xKey: string;
  /** Column header for the x values in the data table. Default: `xKey`. */
  xLabel?: string | undefined;
  /** The measures to draw, bottom of the stack first. Colours follow the order unless pinned. */
  series: readonly ChartSeries[];
  /** `overlap` (default), `stacked` or `expand` (100%). Exposed as `data-stacking`. */
  stacking?: AreaChartStacking | undefined;
  /** `linear` (default) or `monotone`, which smooths without overshooting. */
  curve?: AreaChartCurve | undefined;
  /** `time` spaces points by date or number; `category` evenly. Default: `time` when every x is a Date or number. */
  xScale?: 'category' | 'time' | undefined;
  /** BCP 47 locale for numbers and dates. Pass it when rendering on the server. */
  locale?: string | undefined;
  /** `Intl.NumberFormat` options for values in the tooltip, table and summary. */
  valueFormat?: Intl.NumberFormatOptions | undefined;
  /** Formats values in the tooltip, table and summary. Wins over `valueFormat`. */
  formatValue?: ValueFormatter | undefined;
  /** Formats value-axis ticks. Default: compact notation (percent in `expand`). */
  formatTick?: ValueFormatter | undefined;
  /** `Intl.DateTimeFormat` options for date x values. Default `{ month: 'short', day: 'numeric' }`. */
  xFormat?: Intl.DateTimeFormatOptions | undefined;
  /** Formats x values everywhere. Wins over `xFormat`. */
  formatX?: ((value: XValue, index: number) => string) | undefined;
  /** Keep zero on the value axis (overlap only; stacks always start at zero). Default `true`. */
  includeZero?: boolean | undefined;
  /** Fix either end of the value axis (ignored in `expand`, which is always 0–100%). */
  yDomain?: readonly [ChartDomainBound, ChartDomainBound] | undefined;
  /** Dashed horizontal lines for targets or baselines (not drawn in `expand`). */
  referenceLines?: readonly ChartReferenceLine[] | undefined;
  /** Controlled list of hidden series keys. */
  hiddenSeries?: readonly string[] | undefined;
  /** Uncontrolled initial hidden series. */
  defaultHiddenSeries?: readonly string[] | undefined;
  onHiddenSeriesChange?: ((hidden: string[]) => void) | undefined;
  /** Legend entries toggle their series. Default `true` (the legend shows for two or more series). */
  legendToggle?: boolean | undefined;
  /** Controlled inspected index (`null` for none), to share a crosshair between charts. */
  activeIndex?: number | null | undefined;
  onActiveIndexChange?: ((index: number | null) => void) | undefined;
  /** Click, Enter or Space on the inspected point, for drill-in. */
  onPointSelect?: ((datum: ChartDatum, index: number) => void) | undefined;
  /** Replaces the generated text summary (the plot's accessible name). */
  summary?: string | undefined;
  labels?: AreaChartLabels | undefined;
}

const MARGIN = { top: 8, right: 12, bottom: 24 };
const FALLBACK_WIDTH = 640;

function isContinuous(v: unknown): v is Date | number {
  return v instanceof Date || (typeof v === 'number' && Number.isFinite(v));
}

/** Closed shape between an upper and a lower edge, one per run of points defined on both. */
function bandPath(
  upper: readonly (Point | null)[],
  lower: readonly (Point | null)[],
  curve: Curve,
): string {
  const runs: { top: Point[]; bottom: Point[] }[] = [];
  let run: { top: Point[]; bottom: Point[] } | null = null;
  upper.forEach((u, i) => {
    const l = lower[i];
    if (u && l) {
      run ??= { top: [], bottom: [] };
      run.top.push(u);
      run.bottom.push(l);
    } else if (run) {
      runs.push(run);
      run = null;
    }
  });
  if (run) runs.push(run);
  return runs
    .filter((r) => r.top.length > 1)
    .map(
      (r) =>
        `${linePath(r.top, curve)}${linePath([...r.bottom].reverse(), curve).replace(/^M/, 'L')}Z`,
    )
    .join('');
}

/**
 * Area chart for volumes over time: several series overlapping, stacked so
 * the top edge is the total, or stacked to 100% to show shares. Same
 * crosshair tooltip on hover, touch and keyboard, legend toggles and
 * accessible data table as LineChart.
 */
export const AreaChart = forwardRef<HTMLElement, AreaChartProps>(function AreaChart(
  {
    data,
    xKey,
    xLabel,
    series,
    stacking = 'overlap',
    curve = 'linear',
    xScale,
    locale: localeProp,
    valueFormat,
    formatValue,
    formatTick,
    xFormat,
    formatX,
    includeZero = true,
    yDomain,
    referenceLines: referenceLinesProp = [],
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
  const locale = useClientLocale(localeProp);
  const zone = useClientZone();
  const hintId = useId();
  const il = { ...DEFAULT_INTERACTION_LABELS, ...labels };
  const sl = { ...DEFAULT_SUMMARY_LABELS, ...labels };
  const shareText = labels?.share ?? ((v: string, s: string) => `${v} (${s})`);
  const [hidden, setHidden] = useControllable<readonly string[]>(
    hiddenSeries,
    defaultHiddenSeries ?? [],
    onHiddenSeriesChange as ((next: readonly string[]) => void) | undefined,
  );
  const [measureRef, measured] = useElementWidth<HTMLDivElement>();
  const width = measured ?? FALLBACK_WIDTH;
  const stacked = stacking !== 'overlap';
  const expand = stacking === 'expand';
  const referenceLines = expand ? [] : referenceLinesProp;

  const fmtValue = createValueFormatter(locale, valueFormat, formatValue);
  const fmtPercent = createValueFormatter(locale, { style: 'percent', maximumFractionDigits: 1 });
  const fmtTick = expand
    ? (formatTick ?? createValueFormatter(locale, { style: 'percent', maximumFractionDigits: 0 }))
    : createTickFormatter(locale, valueFormat, formatTick);
  const fmtX = createXFormatter(locale, xFormat, formatX, zone);

  const colored = useMemo(
    () => series.map((s, i) => ({ ...s, color: (s.color ?? defaultChartColor(i)) as ChartColor })),
    [series],
  );
  const visible = colored.filter((s) => !hidden.includes(s.key));
  const n = data.length;
  const xs = data.map((d) => d[xKey]);
  const continuous = (xScale ?? (xs.every(isContinuous) ? 'time' : 'category')) === 'time';
  const xLabels = xs.map((v, i) => fmtX(v as XValue, i));

  // Stack: for each visible series, its lower and upper edge at every x (in data units, or shares).
  const stack = useMemo(() => {
    const totals = data.map((d) =>
      visible.reduce((sum, s) => sum + Math.max(0, numberAt(d, s.key) ?? 0), 0),
    );
    const base = data.map(() => 0);
    return {
      totals,
      layers: visible.map((s) =>
        data.map((d, i) => {
          const v = numberAt(d, s.key);
          if (v === null) return null;
          if (!stacked) return { lo: 0, hi: v };
          const part = Math.max(0, v);
          const scaled = expand ? (totals[i] ? part / (totals[i] ?? 1) : 0) : part;
          const lo = base[i] ?? 0;
          base[i] = lo + scaled;
          return { lo, hi: lo + scaled };
        }),
      ),
    };
  }, [data, visible, stacked, expand]);

  // Value axis.
  const plotH = Math.max(40, height - MARGIN.top - MARGIN.bottom);
  const ext = expand
    ? ([0, 1] as [number, number])
    : extent([
        ...stack.layers.flatMap((layer) => layer.flatMap((p) => (p ? [p.lo, p.hi] : []))),
        ...referenceLines.map((r) => r.value),
      ]);
  const axis = valueAxisLayout(ext, {
    includeZero: stacked || includeZero,
    domain: expand ? [0, 1] : yDomain,
    tickCount: expand ? 4 : tickCountFor(plotH),
    format: fmtTick,
  });
  const x0 = axis.labelWidth + 8;
  const x1 = Math.max(x0 + 1, width - MARGIN.right);
  const y0 = MARGIN.top;
  const y1 = MARGIN.top + plotH;
  const y = linearScale(axis.domain, [y1, y0]);

  const positions = useMemo(() => {
    if (n === 0) return [];
    if (n === 1) return [(x0 + x1) / 2];
    if (continuous) {
      const t = xs.map((v) => (v instanceof Date ? v.getTime() : Number(v)));
      const lo = minOf(t);
      const hi = maxOf(t);
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

  const baseline = y(Math.max(axis.domain[0], Math.min(0, axis.domain[1])));
  const edges = stack.layers.map((layer) => ({
    upper: layer.map((p, i) => (p ? { x: positions[i] ?? 0, y: y(p.hi) } : null)),
    lower: layer.map((p, i) =>
      p ? { x: positions[i] ?? 0, y: stacked ? y(p.lo) : baseline } : null,
    ),
  }));

  const ticks = labelIndices(n, x1 - x0).map((i) => ({
    key: i,
    at: positions[i] ?? 0,
    label: xLabels[i] ?? '',
    anchor: (n > 1 && i === 0 ? 'start' : n > 1 && i === n - 1 ? 'end' : 'middle') as
      'start' | 'end' | 'middle',
  }));

  const valueAt = (i: number, key: string) => {
    const v = numberAt(data[i], key);
    if (v === null) return il.noValue;
    if (!expand) return fmtValue(v);
    const total = stack.totals[i] ?? 0;
    return shareText(fmtValue(v), fmtPercent(total ? Math.max(0, v) / total : 0));
  };
  const totalAt = (i: number) => fmtValue(stack.totals[i] ?? 0);
  const showTotal = stacked && visible.length > 1;

  const describe = (i: number) => {
    const parts = visible.map((s) => `${s.label} ${valueAt(i, s.key)}`);
    if (showTotal) parts.push(`${il.total} ${totalAt(i)}`);
    return `${xLabels[i]}: ${parts.join(', ')}`;
  };

  const generated =
    n === 0
      ? sl.noData
      : [
          sl.points(n, xLabels[0] ?? '', xLabels[n - 1] ?? ''),
          ...visible.map((s) =>
            describeTrend(
              s.label,
              data.map((d) => numberAt(d, s.key)),
              fmtValue,
              sl,
            ),
          ),
          ...(showTotal ? [describeTrend(il.total, stack.totals, fmtValue, sl)] : []),
          ...referenceLines.map((r) => `${r.label ?? ''} ${fmtValue(r.value)}`.trim()),
        ].join('. ');

  const table: ChartTableData = {
    columns: [
      { key: '__x', label: xLabel ?? xKey },
      ...colored.map((s) => ({ key: s.key, label: s.label })),
      ...(stacked && colored.length > 1 ? [{ key: '__total', label: il.total }] : []),
    ],
    rows: data.map((d, i) => {
      const row: Record<string, string> = { __x: xLabels[i] ?? '' };
      let total = 0;
      for (const s of colored) {
        const v = numberAt(d, s.key);
        total += Math.max(0, v ?? 0);
        row[s.key] = v === null ? il.noValue : fmtValue(v);
      }
      if (stacked && colored.length > 1) row.__total = fmtValue(total);
      return row;
    }),
  };

  const activeX = active !== null ? positions[active] : undefined;

  return (
    <ChartFrame
      ref={ref}
      {...frame}
      title={title}
      status={n === 0 && status === 'ready' ? 'empty' : status}
      height={height}
      table={table}
      labels={labels}
      data-chart="area"
    >
      {colored.length > 1 ? (
        <ChartLegend
          className="mn-area-chart__legend"
          items={colored.map((s) => ({
            key: s.key,
            label: s.label,
            color: s.color,
            dashed: s.dashed ?? false,
          }))}
          marker="square"
          toggleable={legendToggle}
          hiddenSeries={hidden}
          onHiddenSeriesChange={setHidden}
          keepOneVisible={false}
          messages={labels?.legend ? { label: labels.legend } : undefined}
        />
      ) : null}
      <div
        ref={measureRef}
        className="mn-chart__plot mn-area-chart"
        data-stacking={stacking}
        data-selectable={onPointSelect ? '' : undefined}
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
          {visible.map((s, si) => {
            const e = edges[si];
            if (!e) return null;
            return (
              <path
                key={`area-${s.key}`}
                className="mn-chart__area mn-area-chart__area"
                data-series={s.key}
                data-dashed={s.dashed || undefined}
                style={colorStyle(s.color)}
                d={bandPath(e.upper, e.lower, curve)}
              />
            );
          })}
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
          {visible.map((s, si) => {
            const e = edges[si];
            if (!e) return null;
            const lone = e.upper.filter(
              (p, i): p is Point => p !== null && !e.upper[i - 1] && !e.upper[i + 1],
            );
            const hi = active !== null ? e.upper[active] : null;
            return (
              <g key={s.key} style={colorStyle(s.color)} data-series={s.key}>
                <path
                  className="mn-chart__line"
                  data-dashed={s.dashed || undefined}
                  d={linePath(e.upper, curve)}
                />
                {lone.map((p, i) => (
                  <circle key={i} className="mn-chart__dot" cx={p.x} cy={p.y} r={3.5} />
                ))}
                {hi ? (
                  <circle className="mn-chart__dot" data-active="" cx={hi.x} cy={hi.y} r={5} />
                ) : null}
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
            rows={[...visible].reverse().map((s) => ({
              key: s.key,
              label: s.label,
              value: valueAt(active, s.key),
              color: s.color,
              dashed: s.dashed ?? false,
              marker: 'square' as const,
            }))}
            footer={showTotal ? `${il.total} ${totalAt(active)}` : undefined}
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
