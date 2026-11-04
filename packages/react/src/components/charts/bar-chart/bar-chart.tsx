'use client';

import { forwardRef, useCallback, useId, useMemo, type ReactNode } from 'react';
import {
  CategoryAxis,
  ChartLiveRegion,
  ChartTooltip,
  DEFAULT_INTERACTION_LABELS,
  DEFAULT_SUMMARY_LABELS,
  ReferenceLine,
  ValueAxis,
  barPath,
  colorStyle,
  createTickFormatter,
  createValueFormatter,
  createXFormatter,
  defaultChartColor,
  estimateTextWidth,
  extent,
  labelIndices,
  linearScale,
  numberAt,
  seriesStats,
  tickCountFor,
  truncateLabel,
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
  type ValueFormatter,
  type XValue,
} from '../../../internal/charts/index.js';
import { ChartFrame, type ChartFrameLabels, type ChartFrameProps } from '../chart-frame/index.js';

export type BarChartOrientation = 'vertical' | 'horizontal';
export type BarChartLayout = 'grouped' | 'stacked';

export type BarChartLabels = ChartFrameLabels &
  Partial<ChartInteractionLabels> &
  Partial<ChartSummaryLabels>;

export interface BarChartProps extends Omit<
  ChartFrameProps,
  'children' | 'legend' | 'legendMarker' | 'onLegendToggle' | 'table' | 'labels' | 'height'
> {
  /** One row per category, in display order, with one number per series. Missing values draw no bar. */
  data: readonly ChartDatum[];
  /** Field holding the category: a label, a number or a `Date` (formatted with `categoryFormat`). */
  categoryKey: string;
  /** Column header for the categories in the data table. Default: `categoryKey`. */
  categoryLabel?: string | undefined;
  /** The measures to draw. Colours follow the order unless pinned. */
  series: readonly ChartSeries[];
  /** `vertical` (default) columns, or `horizontal` bars for long category names and rankings. Exposed as `data-orientation`. */
  orientation?: BarChartOrientation | undefined;
  /** With several series: side by side (`grouped`, default) or one stacked bar per category. Exposed as `data-layout`. */
  layout?: BarChartLayout | undefined;
  /** Print each bar's value (the stack total when stacked) at its end. */
  valueLabels?: boolean | undefined;
  /** Emphasise one category (the current month, say); the others are dimmed. */
  highlightIndex?: number | undefined;
  /** Height of the chart in px. Default 240 vertical; horizontal grows with the number of categories (32px each). */
  height?: number | undefined;
  /** BCP 47 locale for numbers and dates. Pass it when rendering on the server. */
  locale?: string | undefined;
  /** `Intl.NumberFormat` options for values in the tooltip, table, labels and summary. */
  valueFormat?: Intl.NumberFormatOptions | undefined;
  /** Formats values in the tooltip, table, labels and summary. Wins over `valueFormat`. */
  formatValue?: ValueFormatter | undefined;
  /** Formats value-axis ticks. Default: compact notation of `valueFormat`. */
  formatTick?: ValueFormatter | undefined;
  /** `Intl.DateTimeFormat` options when categories are dates. Default `{ month: 'short', day: 'numeric' }`. */
  categoryFormat?: Intl.DateTimeFormatOptions | undefined;
  /** Formats categories everywhere. Wins over `categoryFormat`. */
  formatCategory?: ((value: XValue, index: number) => string) | undefined;
  /** Fix the far end of the value axis (bars always start at zero). */
  valueDomain?: readonly [ChartDomainBound, ChartDomainBound] | undefined;
  /** Dashed lines across the bars for targets or averages. */
  referenceLines?: readonly ChartReferenceLine[] | undefined;
  /** Controlled list of hidden series keys. */
  hiddenSeries?: readonly string[] | undefined;
  /** Uncontrolled initial hidden series. */
  defaultHiddenSeries?: readonly string[] | undefined;
  onHiddenSeriesChange?: ((hidden: string[]) => void) | undefined;
  /** Legend entries toggle their series. Default `true` (legend shows for two or more series). */
  legendToggle?: boolean | undefined;
  /** Controlled inspected category (`null` for none). */
  activeIndex?: number | null | undefined;
  onActiveIndexChange?: ((index: number | null) => void) | undefined;
  /** Click, Enter or Space on the inspected category, for drill-in (open the filtered list). */
  onBarSelect?: ((datum: ChartDatum, index: number) => void) | undefined;
  /** Replaces the generated text summary (the plot's accessible name). */
  summary?: string | undefined;
  labels?: BarChartLabels | undefined;
}

const FALLBACK_WIDTH = 640;
const RADIUS = 4;

interface Segment {
  key: string;
  color: ChartColor;
  dashed: boolean;
  value: number;
  /** Value-axis start and end of the bar. */
  from: number;
  to: number;
  /** Cross-axis offset and thickness inside the category band (px). */
  offset: number;
  size: number;
  rounded: boolean;
}

/**
 * Bar and column chart: one or more series per category, grouped or stacked,
 * vertical or horizontal, with value labels, a highlighted category,
 * reference lines, tooltips on hover, touch and keyboard, legend toggles,
 * drill-in and an accessible data table.
 */
export const BarChart = forwardRef<HTMLElement, BarChartProps>(function BarChart(
  {
    data,
    categoryKey,
    categoryLabel,
    series,
    orientation = 'vertical',
    layout = 'grouped',
    valueLabels = false,
    highlightIndex,
    height: heightProp,
    locale,
    valueFormat,
    formatValue,
    formatTick,
    categoryFormat,
    formatCategory,
    valueDomain,
    referenceLines = [],
    hiddenSeries,
    defaultHiddenSeries,
    onHiddenSeriesChange,
    legendToggle = true,
    activeIndex,
    onActiveIndexChange,
    onBarSelect,
    summary,
    labels,
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
  const horizontal = orientation === 'horizontal';
  const stacked = layout === 'stacked';
  const n = data.length;
  const height = heightProp ?? (horizontal ? Math.max(120, n * 32 + 32) : 240);

  const fmtValue = createValueFormatter(locale, valueFormat, formatValue);
  const fmtTick = createTickFormatter(locale, valueFormat, formatTick);
  const fmtCat = createXFormatter(locale, categoryFormat, formatCategory);

  const colored = useMemo(
    () => series.map((s, i) => ({ ...s, color: (s.color ?? defaultChartColor(i)) as ChartColor })),
    [series],
  );
  const visible = colored.filter((s) => !hidden.includes(s.key));
  const cats = data.map((d, i) => fmtCat(d[categoryKey] as XValue, i));

  // Value extent: stacks sum positives and negatives separately.
  const valueExtent = useMemo(() => {
    const vals: number[] = referenceLines.map((r) => r.value);
    for (const d of data) {
      if (stacked) {
        let pos = 0;
        let neg = 0;
        for (const s of visible) {
          const v = numberAt(d, s.key) ?? 0;
          if (v >= 0) pos += v;
          else neg += v;
        }
        vals.push(pos, neg);
      } else for (const s of visible) vals.push(numberAt(d, s.key) ?? 0);
    }
    return extent(vals);
  }, [data, visible, stacked, referenceLines]);

  // Margins depend on orientation: horizontal bars put category labels on the left.
  const catLabelWidth = horizontal
    ? Math.min(width * 0.4, Math.max(0, ...cats.map((c) => estimateTextWidth(c))) + 8)
    : 0;
  const valueLabelRoom = valueLabels ? (horizontal ? 44 : 16) : 0;
  const top = 8 + (horizontal ? 0 : valueLabelRoom);
  const bottom = 24;
  const plotLength = horizontal
    ? width - catLabelWidth - 12 - valueLabelRoom
    : height - top - bottom;
  const axis = valueAxisLayout(valueExtent, {
    includeZero: true,
    domain: valueDomain,
    tickCount: horizontal
      ? Math.max(2, Math.min(6, Math.floor(plotLength / 80)))
      : tickCountFor(plotLength),
    format: fmtTick,
  });
  const x0 = horizontal ? catLabelWidth : axis.labelWidth + 8;
  const x1 = Math.max(x0 + 1, width - 12 - (horizontal ? valueLabelRoom : 0));
  const y0 = top;
  const y1 = Math.max(y0 + 1, height - bottom);
  const value = horizontal
    ? linearScale(axis.domain, [x0, x1])
    : linearScale(axis.domain, [y1, y0]);
  const bandStart = horizontal ? y0 : x0;
  const bandLength = horizontal ? y1 - y0 : x1 - x0;
  const band = n > 0 ? bandLength / n : bandLength;

  const bars = useMemo(() => {
    const groupSize = Math.min(
      band * 0.72,
      stacked || visible.length <= 1 ? 56 : 56 * visible.length,
    );
    const per = stacked ? groupSize : groupSize / Math.max(1, visible.length);
    return data.map((d) => {
      const segs: Segment[] = [];
      let pos = 0;
      let neg = 0;
      visible.forEach((s, si) => {
        const v = numberAt(d, s.key);
        if (v === null) return;
        const base = stacked ? (v >= 0 ? pos : neg) : 0;
        const end = base + v;
        if (stacked) {
          if (v >= 0) pos = end;
          else neg = end;
        }
        segs.push({
          key: s.key,
          color: s.color,
          dashed: s.dashed ?? false,
          value: v,
          from: base,
          to: end,
          offset: (band - groupSize) / 2 + (stacked ? 0 : si * per),
          size: per,
          rounded: !stacked,
        });
      });
      if (stacked) {
        const lastPos = [...segs].reverse().find((s) => s.value > 0);
        const lastNeg = [...segs].reverse().find((s) => s.value < 0);
        if (lastPos) lastPos.rounded = true;
        if (lastNeg) lastNeg.rounded = true;
      }
      return { segs, total: pos + neg, pos, neg };
    });
  }, [data, visible, stacked, band]);

  const indexAt = useCallback(
    (px: number, py: number) => {
      if (n === 0) return null;
      const p = horizontal ? py : px;
      const i = Math.floor((p - bandStart) / band);
      return i < 0 || i >= n ? null : i;
    },
    [n, horizontal, bandStart, band],
  );
  const { active, source, plotProps } = useChartPointer({
    count: status === 'ready' ? n : 0,
    activeIndex,
    onActiveIndexChange,
    indexAt,
    onSelect: onBarSelect ? (i) => onBarSelect(data[i] ?? {}, i) : undefined,
  });

  const zero = value(0);
  const rect = (seg: Segment, i: number) => {
    const a = value(Math.min(seg.from, seg.to));
    const b = value(Math.max(seg.from, seg.to));
    const cross = bandStart + i * band + seg.offset + (stacked || visible.length <= 1 ? 0 : 1);
    const thick = Math.max(1, seg.size - (stacked || visible.length <= 1 ? 0 : 2));
    const r = seg.rounded ? RADIUS : 0;
    if (horizontal) {
      return barPath(a, cross, Math.max(0, b - a), thick, r, seg.value >= 0 ? 'right' : 'left');
    }
    return barPath(cross, b, thick, Math.max(0, a - b), r, seg.value >= 0 ? 'top' : 'bottom');
  };

  const catTicks = horizontal
    ? cats.map((c, i) => ({
        key: i,
        at: bandStart + (i + 0.5) * band,
        label: truncateLabel(c, catLabelWidth - 8),
      }))
    : labelIndices(
        n,
        bandLength,
        Math.max(32, Math.max(0, ...cats.map((c) => estimateTextWidth(c))) + 12),
      ).map((i) => ({ key: i, at: bandStart + (i + 0.5) * band, label: cats[i] ?? '' }));

  const stackTotal = stacked && visible.length > 1;
  const valueText = (v: number | null) => (v === null ? il.noValue : fmtValue(v));

  const describe = (i: number) => {
    const parts = visible.map((s) => `${s.label} ${valueText(numberAt(data[i], s.key))}`);
    if (stackTotal) parts.push(`${il.total} ${fmtValue(bars[i]?.total ?? 0)}`);
    return `${cats[i]}: ${parts.join(', ')}`;
  };

  const generated =
    n === 0
      ? sl.noData
      : [
          sl.points(n, cats[0] ?? '', cats[n - 1] ?? ''),
          ...visible.map((s) => {
            const st = seriesStats(data.map((d) => numberAt(d, s.key)));
            return st
              ? sl.categories(
                  s.label,
                  fmtValue(st.total),
                  cats[st.maxIndex] ?? '',
                  fmtValue(st.max),
                )
              : `${s.label}: ${sl.noData}`;
          }),
          ...referenceLines.map((r) => `${r.label ?? ''} ${fmtValue(r.value)}`.trim()),
        ].join('. ');

  const table: ChartTableData = {
    columns: [
      { key: '__c', label: categoryLabel ?? categoryKey },
      ...colored.map((s) => ({ key: s.key, label: s.label })),
      ...(stacked && colored.length > 1 ? [{ key: '__total', label: il.total }] : []),
    ],
    rows: data.map((d, i) => {
      const row: Record<string, string> = { __c: cats[i] ?? '' };
      let total = 0;
      for (const s of colored) {
        const v = numberAt(d, s.key);
        total += v ?? 0;
        row[s.key] = valueText(v);
      }
      if (stacked && colored.length > 1) row.__total = fmtValue(total);
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

  // Value labels at the bar ends (the stack total when stacked).
  const endLabels: ReactNode[] = [];
  if (valueLabels) {
    bars.forEach((b, i) => {
      const items = stacked
        ? b.segs.length
          ? [
              {
                key: 'total',
                v: b.total,
                end: b.total >= 0 ? b.pos : b.neg,
                offset: b.segs[0]?.offset ?? 0,
                size: b.segs[0]?.size ?? 0,
              },
            ]
          : []
        : b.segs.map((s) => ({
            key: s.key,
            v: s.value,
            end: s.to,
            offset: s.offset,
            size: s.size,
          }));
      for (const it of items) {
        const center = bandStart + i * band + it.offset + it.size / 2;
        const p = value(it.end);
        endLabels.push(
          horizontal ? (
            <text
              key={`${i}-${it.key}`}
              className="mn-chart__value"
              x={it.v >= 0 ? p + 4 : p - 4}
              y={center}
              dy="0.32em"
              textAnchor={it.v >= 0 ? 'start' : 'end'}
            >
              {fmtTick(it.v)}
            </text>
          ) : (
            <text
              key={`${i}-${it.key}`}
              className="mn-chart__value"
              x={center}
              y={it.v >= 0 ? p - 4 : p + 12}
              textAnchor="middle"
            >
              {fmtTick(it.v)}
            </text>
          ),
        );
      }
    });
  }

  const anchor =
    active !== null
      ? horizontal
        ? {
            x: Math.min(value(Math.max(0, bars[active]?.pos ?? 0)), x1),
            y: bandStart + active * band,
          }
        : { x: bandStart + (active + 0.5) * band, y: y0 }
      : undefined;

  return (
    <ChartFrame
      ref={ref}
      {...frame}
      title={title}
      status={n === 0 && status === 'ready' ? 'empty' : status}
      height={height}
      legend={legend}
      legendMarker="square"
      onLegendToggle={legendToggle && legend ? toggle : undefined}
      table={table}
      labels={labels}
      data-chart="bar"
    >
      <div
        ref={measureRef}
        className="mn-chart__plot mn-bar-chart"
        data-orientation={orientation}
        data-layout={layout}
        data-selectable={onBarSelect ? '' : undefined}
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
          {active !== null &&
            (horizontal ? (
              <rect
                className="mn-chart__band"
                x={x0}
                y={bandStart + active * band}
                width={x1 - x0}
                height={band}
                rx={RADIUS}
              />
            ) : (
              <rect
                className="mn-chart__band"
                x={bandStart + active * band}
                y={y0}
                width={band}
                height={y1 - y0}
                rx={RADIUS}
              />
            ))}
          <ValueAxis
            ticks={axis.ticks}
            scale={value}
            format={fmtTick}
            orientation={horizontal ? 'horizontal' : 'vertical'}
            from={horizontal ? y0 : x0}
            to={horizontal ? y1 : x1}
            labelAt={horizontal ? y1 + 8 : x0 - 8}
          />
          <CategoryAxis
            labels={catTicks}
            orientation={horizontal ? 'vertical' : 'horizontal'}
            position={horizontal ? x0 - 8 : y1 + 8}
          />
          {bars.map((b, i) => (
            <g key={i} data-index={i}>
              {b.segs.map((s) => (
                <path
                  key={s.key}
                  className="mn-chart__bar"
                  data-series={s.key}
                  data-dashed={s.dashed || undefined}
                  data-dimmed={
                    highlightIndex !== undefined && highlightIndex !== i ? '' : undefined
                  }
                  style={colorStyle(s.color)}
                  d={rect(s, i)}
                />
              ))}
            </g>
          ))}
          {horizontal ? (
            <line className="mn-chart__grid" data-zero="" x1={zero} x2={zero} y1={y0} y2={y1} />
          ) : (
            <line className="mn-chart__grid" data-zero="" x1={x0} x2={x1} y1={zero} y2={zero} />
          )}
          {referenceLines.map((r, i) => (
            <ReferenceLine
              key={`ref-${i}`}
              line={r}
              scale={value}
              orientation={horizontal ? 'horizontal' : 'vertical'}
              from={horizontal ? y0 : x0}
              to={horizontal ? y1 : x1}
              format={fmtTick}
            />
          ))}
          {endLabels}
        </svg>
        {active !== null && anchor && (
          <ChartTooltip
            x={anchor.x}
            y={anchor.y}
            width={width}
            title={cats[active]}
            rows={visible.map((s) => ({
              key: s.key,
              label: s.label,
              value: valueText(numberAt(data[active], s.key)),
              color: s.color,
              dashed: s.dashed ?? false,
              marker: 'square' as const,
            }))}
            footer={stackTotal ? `${il.total} ${fmtValue(bars[active]?.total ?? 0)}` : undefined}
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
