'use client';

import { forwardRef, useCallback, useId, useMemo } from 'react';
import {
  CategoryAxis,
  ChartLiveRegion,
  ChartTooltip,
  DEFAULT_INTERACTION_LABELS,
  ValueAxis,
  colorStyle,
  createTickFormatter,
  createValueFormatter,
  createXFormatter,
  extent,
  isolatedPoints,
  labelIndices,
  linePath,
  linearScale,
  nearestIndex,
  tickCountFor,
  useChartPointer,
  useControllable,
  useElementWidth,
  valueAxisLayout,
  type ChartColor,
  type ChartInteractionLabels,
  type ChartTableData,
  type Point,
  type ValueFormatter,
  type XValue,
} from '../../../internal/charts/index.js';
import { ChartFrame, type ChartFrameLabels, type ChartFrameProps } from '../chart-frame/index.js';
import { ChartLegend } from '../chart-legend/index.js';
import { useClientLocale, useClientZone } from '../../../internal/dates/client-time.js';

/** One day (or other step) of the sprint. */
export interface BurndownPoint {
  /** The day: a `Date`, a number or a label. Formatted with `xFormat` / `formatX`. */
  date: XValue;
  /** Work left at the end of the day. Leave out (or `null`) for days not reached yet. */
  remaining?: number | null | undefined;
  /** Total committed work that day. A change from the day before is drawn as a scope change. */
  scope?: number | null | undefined;
}

/** Series of a burndown, used for `hiddenSeries`. */
export type BurndownSeries = 'remaining' | 'ideal' | 'scope';

/** Words the chart renders; pass translations through `labels`. */
export interface BurndownChartTextLabels {
  /** Legend, tooltip and table name of the actual line. Default "Remaining". */
  remaining: string;
  /** Name of the ideal line. Default "Ideal". */
  ideal: string;
  /** Name of the scope line. Default "Scope". */
  scope: string;
  /** Table header of the day column. Default "Day". */
  day: string;
  /** Label of the today marker. Default "Today". */
  today: string;
  /** Table header of the scope change column. Default "Scope change". */
  scopeChange: string;
  /** A scope change, signed: "+5" or "−2". */
  change: (delta: string, added: boolean) => string;
  /** Summary sentence: where the sprint stands on the latest day with data. */
  status: (day: string, remaining: string, ideal: string) => string;
  /** Appended to the status: ahead of, behind or on the ideal line. */
  ahead: (by: string) => string;
  behind: (by: string) => string;
  onTrack: string;
  /** Summary sentence about scope: "Scope 40, 5 added and 2 removed since the start". */
  scopeSummary: (scope: string, added: string, removed: string) => string;
  /** Summary when there is no remaining value yet. Default "Not started". */
  notStarted: string;
}

const DEFAULT_TEXT: BurndownChartTextLabels = {
  remaining: 'Remaining',
  ideal: 'Ideal',
  scope: 'Scope',
  day: 'Day',
  today: 'Today',
  scopeChange: 'Scope change',
  change: (delta, added) => `${added ? '+' : '−'}${delta}`,
  status: (day, remaining, ideal) => `${day}: ${remaining} remaining, ideal ${ideal}`,
  ahead: (by) => `${by} ahead of ideal`,
  behind: (by) => `${by} behind ideal`,
  onTrack: 'on the ideal line',
  scopeSummary: (scope, added, removed) =>
    `Scope ${scope}, ${added} added and ${removed} removed since the start`,
  notStarted: 'Not started',
};

export type BurndownChartLabels = ChartFrameLabels &
  Partial<ChartInteractionLabels> &
  Partial<BurndownChartTextLabels>;

export interface BurndownChartProps extends Omit<
  ChartFrameProps,
  'children' | 'legend' | 'legendMarker' | 'onLegendToggle' | 'table' | 'labels'
> {
  /** One entry per day of the sprint, in order, including days not reached yet (without `remaining`). */
  data: readonly BurndownPoint[];
  /**
   * Work at the start of the ideal line. Default: the first day's `scope`,
   * or its `remaining` when there is no scope. The ideal falls linearly to
   * zero on the last day.
   */
  idealStart?: number | undefined;
  /**
   * Index of today, marked with a dashed vertical line. Default: the last day
   * with a `remaining` value, when later days exist. Pass `null` for no marker.
   */
  todayIndex?: number | null | undefined;
  /** Draw the scope line and its changes. Default `true` when any day has a `scope`. */
  showScope?: boolean | undefined;
  /** BCP 47 locale for numbers and dates. Pass it when rendering on the server. */
  locale?: string | undefined;
  /** `Intl.NumberFormat` options for values (tooltip, table, summary). */
  valueFormat?: Intl.NumberFormatOptions | undefined;
  /** Formats values, for example `(v) => \`${v} pts\``. Wins over `valueFormat`. */
  formatValue?: ValueFormatter | undefined;
  /** Formats value-axis ticks. Default: compact notation. */
  formatTick?: ValueFormatter | undefined;
  /** `Intl.DateTimeFormat` options for Date days. Default `{ month: 'short', day: 'numeric' }`. */
  xFormat?: Intl.DateTimeFormatOptions | undefined;
  /** Formats days everywhere. Wins over `xFormat`. */
  formatX?: ((value: XValue, index: number) => string) | undefined;
  /** Controlled list of hidden lines. The legend always keeps one line visible. */
  hiddenSeries?: readonly BurndownSeries[] | undefined;
  /** Uncontrolled initial hidden lines. */
  defaultHiddenSeries?: readonly BurndownSeries[] | undefined;
  onHiddenSeriesChange?: ((hidden: BurndownSeries[]) => void) | undefined;
  /** Controlled inspected day (`null` for none). */
  activeIndex?: number | null | undefined;
  onActiveIndexChange?: ((index: number | null) => void) | undefined;
  /** Click, Enter or Space on the inspected day, for drill-in (the issues closed that day). */
  onDaySelect?: ((point: BurndownPoint, index: number) => void) | undefined;
  /** Replaces the generated text summary (the plot's accessible name). */
  summary?: string | undefined;
  labels?: BurndownChartLabels | undefined;
}

const MARGIN = { top: 16, right: 16, bottom: 24 };
const FALLBACK_WIDTH = 640;
const COLORS: Record<BurndownSeries, ChartColor> = { remaining: 1, ideal: 'neutral', scope: 2 };

function finite(v: number | null | undefined): number | null {
  return typeof v === 'number' && Number.isFinite(v) ? v : null;
}

/**
 * Sprint burndown: work remaining per day against the ideal line, with the
 * scope line and its changes marked, a today marker, and a summary of
 * whether the sprint is ahead or behind. Inspect days by hover, touch or
 * keyboard; every value is also in the data table.
 */
export const BurndownChart = forwardRef<HTMLElement, BurndownChartProps>(function BurndownChart(
  {
    data,
    idealStart,
    todayIndex,
    showScope,
    locale: localeProp,
    valueFormat,
    formatValue,
    formatTick,
    xFormat,
    formatX,
    hiddenSeries,
    defaultHiddenSeries,
    onHiddenSeriesChange,
    activeIndex,
    onActiveIndexChange,
    onDaySelect,
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
  const t = { ...DEFAULT_TEXT, ...labels };
  const [hidden, setHidden] = useControllable<readonly BurndownSeries[]>(
    hiddenSeries,
    defaultHiddenSeries ?? [],
    onHiddenSeriesChange as ((next: readonly BurndownSeries[]) => void) | undefined,
  );
  const [measureRef, measured] = useElementWidth<HTMLDivElement>();
  const width = measured ?? FALLBACK_WIDTH;

  const fmtValue = createValueFormatter(locale, valueFormat, formatValue);
  const fmtTick = createTickFormatter(locale, valueFormat, formatTick);
  const fmtX = createXFormatter(locale, xFormat, formatX, zone);

  const n = data.length;
  const days = data.map((d, i) => fmtX(d.date, i));
  const remaining = data.map((d) => finite(d.remaining));
  const scope = data.map((d) => finite(d.scope));
  const hasScope = scope.some((v) => v !== null);
  const scopeOn = (showScope ?? hasScope) && hasScope;
  const start = idealStart ?? scope[0] ?? remaining[0] ?? null;
  const ideal = data.map((_, i) =>
    start === null ? null : n <= 1 ? start : start * (1 - i / (n - 1)),
  );
  const lastActual = remaining.reduce<number>((last, v, i) => (v === null ? last : i), -1);
  const today = todayIndex === undefined ? (lastActual < n - 1 ? lastActual : -1) : todayIndex;

  const changes = useMemo(
    () =>
      scope.map((v, i) => {
        const prev = i > 0 ? scope[i - 1] : null;
        return v !== null && prev !== null && prev !== undefined && v !== prev ? v - prev : 0;
      }),
    // scope is derived from data
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [data],
  );
  const added = changes.reduce((s, c) => (c > 0 ? s + c : s), 0);
  const removed = changes.reduce((s, c) => (c < 0 ? s - c : s), 0);

  const show = {
    remaining: !hidden.includes('remaining'),
    ideal: !hidden.includes('ideal'),
    scope: scopeOn && !hidden.includes('scope'),
  };

  // Value axis: always from zero.
  const plotH = Math.max(40, height - MARGIN.top - MARGIN.bottom);
  const axis = valueAxisLayout(
    extent([
      ...(show.remaining ? remaining : []),
      ...(show.ideal ? ideal : []),
      ...(show.scope ? scope : []),
    ]),
    { includeZero: true, tickCount: tickCountFor(plotH), format: fmtTick },
  );
  const x0 = axis.labelWidth + 8;
  const x1 = Math.max(x0 + 1, width - MARGIN.right);
  const y0 = MARGIN.top;
  const y1 = MARGIN.top + plotH;
  const y = linearScale(axis.domain, [y1, y0]);
  const positions = useMemo(
    () =>
      n === 0 ? [] : n === 1 ? [(x0 + x1) / 2] : data.map((_, i) => x0 + (i / (n - 1)) * (x1 - x0)),
    [n, data, x0, x1],
  );

  const indexAt = useCallback((px: number) => nearestIndex(positions, px), [positions]);
  const { active, source, plotProps } = useChartPointer({
    count: status === 'ready' ? n : 0,
    activeIndex,
    onActiveIndexChange,
    indexAt,
    onSelect: onDaySelect ? (i) => onDaySelect(data[i] ?? { date: '' }, i) : undefined,
  });

  const pts = (values: readonly (number | null)[]): (Point | null)[] =>
    values.map((v, i) => (v === null ? null : { x: positions[i] ?? 0, y: y(v) }));
  const actualPts = pts(remaining);
  const scopePts = pts(scope);
  // Scope is a step line: it holds its value until the day it changes.
  const scopePath = scopePts
    .map((p, i) => {
      if (!p) return '';
      const prev = i > 0 ? scopePts[i - 1] : null;
      return prev ? `L${p.x},${prev.y}L${p.x},${p.y}` : `M${p.x},${p.y}`;
    })
    .join('');

  const ticks = labelIndices(n, x1 - x0).map((i) => ({
    key: i,
    at: positions[i] ?? 0,
    label: days[i] ?? '',
    anchor: (n > 1 && i === 0 ? 'start' : n > 1 && i === n - 1 ? 'end' : 'middle') as
      'start' | 'end' | 'middle',
  }));

  const text = (v: number | null | undefined) =>
    v === null || v === undefined ? il.noValue : fmtValue(v);
  const changeText = (c: number) => (c === 0 ? '' : t.change(fmtValue(Math.abs(c)), c > 0));

  const describe = (i: number) => {
    const parts: string[] = [];
    if (show.remaining) parts.push(`${t.remaining} ${text(remaining[i])}`);
    if (show.ideal) parts.push(`${t.ideal} ${text(ideal[i])}`);
    if (show.scope) parts.push(`${t.scope} ${text(scope[i])}`);
    const c = changes[i] ?? 0;
    if (scopeOn && c !== 0) parts.push(`${t.scopeChange} ${changeText(c)}`);
    return `${days[i]}: ${parts.join(', ')}`;
  };

  let trend: 'ahead' | 'behind' | 'on-track' | undefined;
  let statusSentence = t.notStarted;
  if (lastActual >= 0) {
    const r = remaining[lastActual] ?? 0;
    const id = ideal[lastActual] ?? r;
    const diff = Math.round((r - id) * 100) / 100;
    trend = diff > 0 ? 'behind' : diff < 0 ? 'ahead' : 'on-track';
    statusSentence = `${t.status(days[lastActual] ?? '', fmtValue(r), fmtValue(id))}, ${
      diff > 0 ? t.behind(fmtValue(diff)) : diff < 0 ? t.ahead(fmtValue(-diff)) : t.onTrack
    }`;
  }
  const lastScope = [...scope].reverse().find((v) => v !== null) ?? null;
  const generated =
    n === 0
      ? il.noValue
      : [
          statusSentence,
          ...(scopeOn && lastScope !== null
            ? [t.scopeSummary(fmtValue(lastScope), fmtValue(added), fmtValue(removed))]
            : []),
        ].join('. ');

  const table: ChartTableData = {
    columns: [
      { key: 'day', label: t.day },
      { key: 'remaining', label: t.remaining },
      { key: 'ideal', label: t.ideal },
      ...(scopeOn
        ? [
            { key: 'scope', label: t.scope },
            { key: 'change', label: t.scopeChange },
          ]
        : []),
    ],
    rows: data.map((_, i) => ({
      day: days[i] ?? '',
      remaining: text(remaining[i]),
      ideal: text(ideal[i]),
      ...(scopeOn ? { scope: text(scope[i]), change: changeText(changes[i] ?? 0) } : {}),
    })),
  };

  const legendItems = [
    { key: 'remaining', label: t.remaining, color: COLORS.remaining },
    { key: 'ideal', label: t.ideal, color: COLORS.ideal, dashed: true },
    ...(scopeOn ? [{ key: 'scope', label: t.scope, color: COLORS.scope }] : []),
  ];

  const activeX = active !== null ? positions[active] : undefined;
  const todayX = today !== null && today >= 0 && today < n ? positions[today] : undefined;

  return (
    <ChartFrame
      ref={ref}
      {...frame}
      title={title}
      status={n === 0 && status === 'ready' ? 'empty' : status}
      height={height}
      table={table}
      labels={labels}
      data-chart="burndown"
      data-trend={trend}
    >
      <ChartLegend
        className="mn-burndown-chart__legend"
        items={legendItems}
        marker="line"
        toggleable
        hiddenSeries={hidden}
        onHiddenSeriesChange={(next) => setHidden(next as BurndownSeries[])}
        messages={labels?.legend ? { label: labels.legend } : undefined}
      />
      <div
        ref={measureRef}
        className="mn-chart__plot mn-burndown-chart"
        data-trend={trend}
        data-selectable={onDaySelect ? '' : undefined}
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
          {todayX !== undefined && (
            <g className="mn-burndown-chart__today">
              <line x1={todayX} x2={todayX} y1={y0} y2={y1} />
              <text x={todayX} y={y0 - 4} textAnchor="middle">
                {t.today}
              </text>
            </g>
          )}
          {activeX !== undefined && (
            <line className="mn-chart__crosshair" x1={activeX} x2={activeX} y1={y0} y2={y1} />
          )}
          {show.ideal && (
            <g style={colorStyle(COLORS.ideal)} data-series="ideal">
              <path className="mn-chart__line" data-dashed="" d={linePath(pts(ideal))} />
            </g>
          )}
          {show.scope && (
            <g style={colorStyle(COLORS.scope)} data-series="scope">
              <path className="mn-chart__line mn-burndown-chart__scope" d={scopePath} />
              {changes.map((c, i) => {
                const p = scopePts[i];
                if (c === 0 || !p) return null;
                return (
                  <g
                    key={i}
                    className="mn-burndown-chart__change"
                    data-direction={c > 0 ? 'up' : 'down'}
                  >
                    <circle className="mn-chart__dot" cx={p.x} cy={p.y} r={3.5} />
                    <text x={p.x} y={p.y - 8} textAnchor="middle">
                      {changeText(c)}
                    </text>
                  </g>
                );
              })}
            </g>
          )}
          {show.remaining && (
            <g style={colorStyle(COLORS.remaining)} data-series="remaining">
              <path className="mn-chart__line mn-burndown-chart__actual" d={linePath(actualPts)} />
              {isolatedPoints(actualPts).map((p, i) => (
                <circle key={i} className="mn-chart__dot" cx={p.x} cy={p.y} r={3.5} />
              ))}
              {active !== null && actualPts[active] ? (
                <circle
                  className="mn-chart__dot"
                  data-active=""
                  cx={actualPts[active]?.x}
                  cy={actualPts[active]?.y}
                  r={5}
                />
              ) : null}
            </g>
          )}
        </svg>
        {active !== null && activeX !== undefined && (
          <ChartTooltip
            x={activeX}
            y={y0}
            width={width}
            title={days[active]}
            rows={[
              ...(show.remaining
                ? [
                    {
                      key: 'remaining',
                      label: t.remaining,
                      value: text(remaining[active]),
                      color: COLORS.remaining,
                      marker: 'line' as const,
                    },
                  ]
                : []),
              ...(show.ideal
                ? [
                    {
                      key: 'ideal',
                      label: t.ideal,
                      value: text(ideal[active]),
                      color: COLORS.ideal,
                      dashed: true,
                      marker: 'line' as const,
                    },
                  ]
                : []),
              ...(show.scope
                ? [
                    {
                      key: 'scope',
                      label: t.scope,
                      value: text(scope[active]),
                      color: COLORS.scope,
                      marker: 'line' as const,
                    },
                  ]
                : []),
            ]}
            footer={
              scopeOn && (changes[active] ?? 0) !== 0
                ? `${t.scopeChange} ${changeText(changes[active] ?? 0)}`
                : undefined
            }
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
