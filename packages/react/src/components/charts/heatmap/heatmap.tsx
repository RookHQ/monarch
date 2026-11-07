'use client';

import {
  forwardRef,
  useId,
  useState,
  type FocusEvent,
  type KeyboardEvent,
  type PointerEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  ChartLiveRegion,
  ChartTooltip,
  DEFAULT_INTERACTION_LABELS,
  colorStyle,
  createValueFormatter,
  estimateTextWidth,
  labelIndices,
  truncateLabel,
  useControllable,
  useElementWidth,
  type ChartColor,
  type ChartInteractionLabels,
  type ChartTableData,
  type ValueFormatter,
} from '../../../internal/charts/index.js';
import { ChartFrame, type ChartFrameLabels, type ChartFrameProps } from '../chart-frame/index.js';

/* ------------------------------------------------------------------ */
/* Sequential scale (shared with CalendarHeatmap and WorldMap)          */
/* ------------------------------------------------------------------ */

/**
 * Colour level of a value: `null` for no data, `0` for zero (or less), and
 * `1`–`4` for positive values, from lightest to darkest.
 */
export type HeatmapLevel = 0 | 1 | 2 | 3 | 4;

/** A sequential scale: three cut points and the level of any value. */
export interface HeatmapScale {
  /** Lower bounds of levels 2, 3 and 4. Positive values below the first are level 1. */
  thresholds: readonly [number, number, number];
  /** The level of a value; `null` for a missing value. */
  level: (value: number | null | undefined) => HeatmapLevel | null;
}

/**
 * Builds the four-step sequential scale used by Heatmap, CalendarHeatmap and
 * WorldMap. By default the cut points split the largest value into quarters;
 * pass `thresholds` (three ascending numbers) to fix them, for example
 * `[2, 5, 10]` so that "10 or more" is always the darkest step.
 */
export function heatmapScale(
  values: Iterable<number | null | undefined>,
  thresholds?: readonly number[] | undefined,
): HeatmapScale {
  let cuts: [number, number, number];
  if (thresholds && thresholds.length >= 3) {
    cuts = [thresholds[0] ?? 0, thresholds[1] ?? 0, thresholds[2] ?? 0];
  } else {
    let max = 0;
    for (const v of values) if (typeof v === 'number' && Number.isFinite(v) && v > max) max = v;
    cuts = [max * 0.25, max * 0.5, max * 0.75];
  }
  const [a, b, c] = cuts;
  return {
    thresholds: cuts,
    level: (v) => {
      if (typeof v !== 'number' || !Number.isFinite(v)) return null;
      if (v <= 0) return 0;
      if (v >= c && c > 0) return 4;
      if (v >= b && b > 0) return 3;
      if (v >= a && a > 0) return 2;
      return 1;
    },
  };
}

/** Words of the scale legend; pass translations through a chart's `labels`. */
export interface HeatmapLegendLabels {
  /** Name of the legend list. Default "Legend". */
  legend?: string | undefined;
  /** Before the lightest step. Default "Less". */
  less?: string | undefined;
  /** After the darkest step. Default "More". */
  more?: string | undefined;
  /** Step for missing values, shown when `showEmpty` is set. Default "No data". */
  noValue?: string | undefined;
}

export interface HeatmapLegendProps {
  scale: HeatmapScale;
  /** Palette slot of the ramp. Default 1. */
  color?: ChartColor | undefined;
  /** Formats the cut points in each step's range. */
  format: ValueFormatter;
  /** Add a step for missing values. */
  showEmpty?: boolean | undefined;
  labels?: HeatmapLegendLabels | undefined;
  className?: string | undefined;
}

/**
 * The "Less … More" legend of a sequential scale. Each step is a list item
 * whose range ("0", "1–4", "5 or more") is real text for screen readers and
 * a native tooltip for pointer users.
 */
export function HeatmapLegend({
  scale,
  color = 1,
  format,
  showEmpty = false,
  labels,
  className,
}: HeatmapLegendProps) {
  const [a, b, c] = scale.thresholds;
  const steps: { level: HeatmapLevel | 'empty'; text: string }[] = [
    ...(showEmpty ? [{ level: 'empty' as const, text: labels?.noValue ?? 'No data' }] : []),
    { level: 0, text: format(0) },
    { level: 1, text: a > 0 ? `< ${format(a)}` : `> ${format(0)}` },
    { level: 2, text: `${format(a)}–${format(b)}` },
    { level: 3, text: `${format(b)}–${format(c)}` },
    { level: 4, text: `≥ ${format(c)}` },
  ];
  return (
    <div className={cx('mn-heatmap-legend', className)} style={colorStyle(color)}>
      <span className="mn-heatmap-legend__end" aria-hidden="true">
        {labels?.less ?? 'Less'}
      </span>
      <ul className="mn-heatmap-legend__steps" aria-label={labels?.legend ?? 'Legend'}>
        {steps.map((s) => (
          <li key={s.level} className="mn-heatmap-legend__step" title={s.text}>
            <span
              className="mn-heatmap__swatch"
              data-level={s.level === 'empty' ? undefined : s.level}
              data-empty={s.level === 'empty' ? '' : undefined}
              aria-hidden="true"
            />
            <span className="mn-visually-hidden">{s.text}</span>
          </li>
        ))}
      </ul>
      <span className="mn-heatmap-legend__end" aria-hidden="true">
        {labels?.more ?? 'More'}
      </span>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Heatmap                                                             */
/* ------------------------------------------------------------------ */

/** A row or column of the matrix. */
export interface HeatmapAxisItem {
  /** Stable id, matched by `HeatmapCell.row` / `.column`. */
  key: string;
  /** Text on the axis, in the tooltip, table and announcements. */
  label: string;
}

/** One value of the matrix. Cells missing from `data` are drawn as "no data". */
export interface HeatmapCell {
  /** Row key. */
  row: string;
  /** Column key. */
  column: string;
  /** The value; `null` for no data. */
  value: number | null;
  /** Replaces the formatted value in the tooltip, table and announcements, e.g. "7h 30m". */
  valueLabel?: string | undefined;
  /** A short note after the value, e.g. "Holiday". */
  note?: string | undefined;
}

/** A cell position. */
export interface HeatmapPosition {
  row: string;
  column: string;
}

/** Words the heatmap adds to ChartFrame's; pass translations here. */
export interface HeatmapLabels
  extends ChartFrameLabels, Partial<ChartInteractionLabels>, HeatmapLegendLabels {
  /** Tooltip row name. Default "Value". */
  value?: string | undefined;
  /** Generated summary. Default "{rows} rows by {columns} columns. Highest {cell} at {value}". */
  summary?: ((rows: number, columns: number, max: string | null) => string) | undefined;
}

export interface HeatmapProps extends Omit<
  ChartFrameProps,
  | 'children'
  | 'legend'
  | 'legendMarker'
  | 'onLegendToggle'
  | 'table'
  | 'labels'
  | 'height'
  | 'color'
> {
  /** Rows, top to bottom (for example weekdays or teams). */
  rows: readonly HeatmapAxisItem[];
  /** Columns, left to right (for example hours or weeks). */
  columns: readonly HeatmapAxisItem[];
  /** The values. Order does not matter; a row/column pair missing here is "no data". */
  data: readonly HeatmapCell[];
  /** Palette slot of the sequential ramp. Default 1. */
  color?: ChartColor | undefined;
  /** Three ascending cut points for levels 2–4. Default: quarters of the largest value. */
  thresholds?: readonly number[] | undefined;
  /** Print each value inside its cell when the cell is wide enough. */
  showValues?: boolean | undefined;
  /** Largest cell side in px. Cells shrink to fit the width. Default 36. */
  maxCellSize?: number | undefined;
  /** Gap between cells in px. Default 2. */
  gap?: number | undefined;
  /** Show the "Less … More" legend under the grid. Default `true`. */
  showLegend?: boolean | undefined;
  /** Column header for row names in the data table. Default "Row". */
  rowHeader?: string | undefined;
  /** BCP 47 locale for numbers. Pass it when rendering on the server. */
  locale?: string | undefined;
  /** `Intl.NumberFormat` options for values. */
  valueFormat?: Intl.NumberFormatOptions | undefined;
  /** Formats values. Wins over `valueFormat`. */
  formatValue?: ValueFormatter | undefined;
  /** Controlled inspected cell (`null` for none). */
  activeCell?: HeatmapPosition | null | undefined;
  onActiveCellChange?: ((cell: HeatmapPosition | null) => void) | undefined;
  /** Click, Enter or Space on a cell, for drill-in. Receives the cell (value `null` when missing). */
  onCellSelect?: ((cell: HeatmapCell) => void) | undefined;
  /** Replaces the generated text summary (the plot's accessible name). */
  summary?: string | undefined;
  labels?: HeatmapLabels | undefined;
}

const FALLBACK_WIDTH = 640;
const LABEL_GAP = 8;
const TOP = 18;

/**
 * A matrix of values as coloured cells, for example activity by weekday and
 * hour or attendance by team and week. Cells are binned into a four-step
 * sequential ramp of one palette colour, with a "Less … More" legend,
 * tooltips on hover, touch and keyboard, drill-in and a data table.
 */
export const Heatmap = forwardRef<HTMLElement, HeatmapProps>(function Heatmap(
  {
    rows,
    columns,
    data,
    color = 1,
    thresholds,
    showValues = false,
    maxCellSize = 36,
    gap = 2,
    showLegend = true,
    rowHeader,
    locale,
    valueFormat,
    formatValue,
    activeCell,
    onActiveCellChange,
    onCellSelect,
    summary,
    labels,
    status = 'ready',
    title,
    footer,
    ...frame
  },
  ref,
) {
  const hintId = useId();
  const il = { ...DEFAULT_INTERACTION_LABELS, ...labels };
  const fmt = createValueFormatter(locale, valueFormat, formatValue);
  const [measureRef, measured] = useElementWidth<HTMLDivElement>();
  const width = measured ?? FALLBACK_WIDTH;
  const nr = rows.length;
  const nc = columns.length;

  const cellMap = new Map<string, HeatmapCell>();
  for (const d of data) cellMap.set(`${d.row}\u0000${d.column}`, d);
  const cellAt = (r: number, c: number): HeatmapCell => {
    const row = rows[r]?.key ?? '';
    const column = columns[c]?.key ?? '';
    return cellMap.get(`${row}\u0000${column}`) ?? { row, column, value: null };
  };
  const scale = heatmapScale(
    data.map((d) => d.value),
    thresholds,
  );
  const valueText = (cell: HeatmapCell) => {
    const v =
      cell.valueLabel ??
      (typeof cell.value === 'number' && Number.isFinite(cell.value)
        ? fmt(cell.value)
        : il.noValue);
    return cell.note ? `${v}, ${cell.note}` : v;
  };

  // Layout: row labels on the left, column labels on top, square cells.
  const rowLabelWidth = Math.min(
    Math.max(0, ...rows.map((r) => estimateTextWidth(r.label))),
    Math.round(width * 0.35),
  );
  const left = rowLabelWidth + LABEL_GAP;
  const available = Math.max(0, width - left);
  const step = nc > 0 ? Math.max(4, Math.min(maxCellSize + gap, available / nc)) : 0;
  const cell = Math.max(2, step - gap);
  const height = TOP + nr * step;
  const colLabelWidth = Math.max(0, ...columns.map((c) => estimateTextWidth(c.label)));
  const shownColumns = new Set(labelIndices(nc, nc * step, Math.max(step, colLabelWidth + 6)));

  // Active cell: controlled by key, kept internally as indices.
  const [active, setActive] = useControllable<HeatmapPosition | null>(
    activeCell,
    null,
    onActiveCellChange,
  );
  const [source, setSource] = useState<'pointer' | 'keyboard'>('pointer');
  const ar = active ? rows.findIndex((r) => r.key === active.row) : -1;
  const ac = active ? columns.findIndex((c) => c.key === active.column) : -1;
  const hasActive = ar >= 0 && ac >= 0;
  const move = (r: number, c: number, from: 'pointer' | 'keyboard') => {
    const row = rows[Math.max(0, Math.min(nr - 1, r))];
    const column = columns[Math.max(0, Math.min(nc - 1, c))];
    if (!row || !column) return;
    setSource(from);
    if (active?.row !== row.key || active?.column !== column.key) {
      setActive({ row: row.key, column: column.key });
    }
  };
  const interactive = status === 'ready' && nr > 0 && nc > 0;

  const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    if (!interactive) return;
    const r = hasActive ? ar : 0;
    const c = hasActive ? ac : 0;
    const page = Math.max(1, Math.ceil(nc / 4));
    switch (e.key) {
      case 'ArrowRight':
        move(r, hasActive ? c + 1 : 0, 'keyboard');
        break;
      case 'ArrowLeft':
        move(r, hasActive ? c - 1 : nc - 1, 'keyboard');
        break;
      case 'ArrowDown':
        move(hasActive ? r + 1 : 0, c, 'keyboard');
        break;
      case 'ArrowUp':
        move(hasActive ? r - 1 : nr - 1, c, 'keyboard');
        break;
      case 'Home':
        if (e.ctrlKey || e.metaKey) move(0, 0, 'keyboard');
        else move(r, 0, 'keyboard');
        break;
      case 'End':
        if (e.ctrlKey || e.metaKey) move(nr - 1, nc - 1, 'keyboard');
        else move(r, nc - 1, 'keyboard');
        break;
      case 'PageDown':
        move(r, c + page, 'keyboard');
        break;
      case 'PageUp':
        move(r, c - page, 'keyboard');
        break;
      case 'Escape':
        if (!hasActive) return;
        setActive(null);
        break;
      case 'Enter':
      case ' ':
        if (hasActive && onCellSelect) {
          e.preventDefault();
          onCellSelect(cellAt(ar, ac));
        }
        return;
      default:
        return;
    }
    e.preventDefault();
  };

  const fromPointer = (e: PointerEvent<HTMLDivElement>) => {
    const target = (e.target as Element).closest?.('[data-cell]');
    const pos = target?.getAttribute('data-cell')?.split(':');
    if (!pos) return;
    move(Number(pos[0]), Number(pos[1]), 'pointer');
  };

  const describe = (r: number, c: number) =>
    `${rows[r]?.label ?? ''}, ${columns[c]?.label ?? ''}: ${valueText(cellAt(r, c))}`;

  // Summary: size of the matrix and its highest cell.
  let best: { r: number; c: number; v: number } | null = null;
  rows.forEach((_, r) =>
    columns.forEach((__, c) => {
      const v = cellAt(r, c).value;
      if (typeof v === 'number' && Number.isFinite(v) && (!best || v > best.v)) best = { r, c, v };
    }),
  );
  const top = best as { r: number; c: number; v: number } | null;
  const maxText = top
    ? `${rows[top.r]?.label}, ${columns[top.c]?.label} ${valueText(cellAt(top.r, top.c))}`
    : null;
  const generated = labels?.summary
    ? labels.summary(nr, nc, maxText)
    : `${nr} ${nr === 1 ? 'row' : 'rows'} by ${nc} ${nc === 1 ? 'column' : 'columns'}. ${
        maxText ? `Highest ${maxText}` : il.noValue
      }`;

  const table: ChartTableData = {
    columns: [
      { key: '__row', label: rowHeader ?? 'Row' },
      ...columns.map((c) => ({ key: `c:${c.key}`, label: c.label })),
    ],
    rows: rows.map((row, r) => {
      const out: Record<string, string> = { __row: row.label };
      columns.forEach((c, ci) => {
        out[`c:${c.key}`] = valueText(cellAt(r, ci));
      });
      return out;
    }),
  };

  const hasData = data.some((d) => typeof d.value === 'number' && Number.isFinite(d.value));
  const resolvedStatus =
    status === 'ready' && (!hasData || nr === 0 || nc === 0) ? 'empty' : status;

  const plotProps = {
    tabIndex: interactive ? 0 : -1,
    onKeyDown: onKeyDown,
    onPointerMove: fromPointer,
    onPointerDown: fromPointer,
    onPointerLeave: (e: PointerEvent<HTMLDivElement>) => {
      if (e.pointerType === 'mouse' && source === 'pointer') setActive(null);
    },
    onFocus: (e: FocusEvent<HTMLDivElement>) => {
      if (e.target !== e.currentTarget || hasActive || !interactive) return;
      if (e.currentTarget.matches(':focus-visible')) move(0, 0, 'keyboard');
    },
    onBlur: (e: FocusEvent<HTMLDivElement>) => {
      if (e.currentTarget.contains(e.relatedTarget as Node | null)) return;
      setActive(null);
    },
    onClick: () => {
      if (hasActive && onCellSelect) onCellSelect(cellAt(ar, ac));
    },
  };

  return (
    <ChartFrame
      ref={ref}
      {...frame}
      title={title}
      status={resolvedStatus}
      height={Math.max(120, height)}
      table={table}
      labels={labels}
      footer={
        showLegend && resolvedStatus === 'ready' ? (
          <>
            <HeatmapLegend
              scale={scale}
              color={color}
              format={fmt}
              showEmpty={data.length < nr * nc}
              labels={{ ...labels, noValue: il.noValue }}
            />
            {footer}
          </>
        ) : (
          footer
        )
      }
      data-chart="heatmap"
    >
      <div
        ref={measureRef}
        className="mn-chart__plot mn-heatmap"
        role="group"
        aria-roledescription={il.chart}
        aria-label={summary ?? generated}
        aria-describedby={hintId}
        data-selectable={onCellSelect ? '' : undefined}
        style={colorStyle(color)}
        {...plotProps}
      >
        <svg
          className="mn-chart__svg"
          viewBox={`0 0 ${width} ${height}`}
          height={height}
          aria-hidden="true"
          focusable="false"
        >
          <g className="mn-chart__axis" data-axis="column">
            {columns.map((c, i) =>
              shownColumns.has(i) ? (
                <text
                  key={c.key}
                  className="mn-chart__tick"
                  x={left + i * step + cell / 2}
                  y={TOP - 6}
                  textAnchor="middle"
                >
                  {c.label}
                </text>
              ) : null,
            )}
          </g>
          <g className="mn-chart__axis" data-axis="row">
            {rows.map((r, i) => (
              <text
                key={r.key}
                className="mn-chart__tick"
                x={left - LABEL_GAP}
                y={TOP + i * step + cell / 2}
                dy="0.32em"
                textAnchor="end"
              >
                {truncateLabel(r.label, rowLabelWidth)}
              </text>
            ))}
          </g>
          {rows.map((row, r) =>
            columns.map((column, c) => {
              const d = cellAt(r, c);
              const level = scale.level(d.value);
              const isActive = r === ar && c === ac;
              return (
                <g key={`${row.key}:${column.key}`}>
                  <rect
                    className="mn-heatmap__cell"
                    data-cell={`${r}:${c}`}
                    data-level={level ?? undefined}
                    data-empty={level === null ? '' : undefined}
                    data-active={isActive ? '' : undefined}
                    x={left + c * step}
                    y={TOP + r * step}
                    width={cell}
                    height={cell}
                    rx={Math.min(4, cell / 4)}
                  />
                  {showValues && level !== null && cell >= 24 ? (
                    <text
                      className="mn-heatmap__value"
                      data-level={level}
                      x={left + c * step + cell / 2}
                      y={TOP + r * step + cell / 2}
                      dy="0.32em"
                      textAnchor="middle"
                    >
                      {d.valueLabel ?? fmt(d.value ?? 0)}
                    </text>
                  ) : null}
                </g>
              );
            }),
          )}
        </svg>
        {hasActive && (
          <ChartTooltip
            x={left + ac * step + cell}
            y={TOP + ar * step}
            width={width}
            title={`${rows[ar]?.label ?? ''} · ${columns[ac]?.label ?? ''}`}
            rows={[
              {
                key: 'v',
                label: labels?.value ?? 'Value',
                value: valueText({ ...cellAt(ar, ac), note: undefined }),
                color,
              },
            ]}
            footer={cellAt(ar, ac).note}
          />
        )}
        <span id={hintId} className="mn-visually-hidden">
          {il.keyboardHint}
        </span>
        <ChartLiveRegion>
          {hasActive && source === 'keyboard' ? describe(ar, ac) : ''}
        </ChartLiveRegion>
      </div>
    </ChartFrame>
  );
});
