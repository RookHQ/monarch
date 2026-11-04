import type { CSSProperties, ReactNode } from 'react';
import { cx } from '../cx.js';
import { chartColor, type ChartColor } from './palette.js';
import type { ChartLegendItem, ChartLegendMarker, ChartTableData } from './types.js';
import type { LinearScale } from './scale.js';

/** Inline style that hands a series colour to CSS as `--_c` (always a token). */
export function colorStyle(color: ChartColor): CSSProperties {
  return { ['--_c' as string]: chartColor(color) } as CSSProperties;
}

/* ------------------------------------------------------------------ */
/* Legend                                                              */
/* ------------------------------------------------------------------ */

export interface ChartLegendProps {
  items: readonly ChartLegendItem[];
  marker?: ChartLegendMarker | undefined;
  /** When set, each entry is a toggle button (`aria-pressed`) that shows or hides its series. */
  onToggle?: ((key: string) => void) | undefined;
  /** Accessible name of the legend list. */
  label?: string | undefined;
  className?: string | undefined;
}

/**
 * Series legend. Identity is never colour alone: every entry has its text
 * label, dashed series show a dashed marker, and hidden series are struck through.
 */
export function ChartLegend({
  items,
  marker = 'line',
  onToggle,
  label = 'Legend',
  className,
}: ChartLegendProps) {
  return (
    <ul className={cx('mn-chart__legend', className)} aria-label={label}>
      {items.map((item) => {
        const content = (
          <>
            <span
              className="mn-chart__swatch"
              data-marker={marker}
              data-dashed={item.dashed || undefined}
              style={colorStyle(item.color)}
              aria-hidden="true"
            />
            <span className="mn-chart__legend-label">{item.label}</span>
          </>
        );
        return (
          <li
            key={item.key}
            className="mn-chart__legend-item"
            data-hidden={item.hidden || undefined}
          >
            {onToggle ? (
              <button
                type="button"
                className="mn-chart__legend-toggle"
                aria-pressed={!item.hidden}
                onClick={() => onToggle(item.key)}
              >
                {content}
              </button>
            ) : (
              content
            )}
          </li>
        );
      })}
    </ul>
  );
}

/* ------------------------------------------------------------------ */
/* Tooltip                                                             */
/* ------------------------------------------------------------------ */

export interface ChartTooltipRow {
  key: string;
  label: string;
  value: string;
  color: ChartColor;
  dashed?: boolean | undefined;
  marker?: ChartLegendMarker | undefined;
}

export interface ChartTooltipProps {
  /** Anchor in px from the plot's left and top. */
  x: number;
  y: number;
  /** Plot width, to flip the tooltip before it overflows. */
  width: number;
  title: ReactNode;
  rows: readonly ChartTooltipRow[];
  footer?: ReactNode | undefined;
}

/**
 * The visual tooltip. It is `aria-hidden`: keyboard and screen-reader users
 * get the same text from the chart's live region and the data table.
 */
export function ChartTooltip({ x, y, width, title, rows, footer }: ChartTooltipProps) {
  const side = x > width / 2 ? 'left' : 'right';
  return (
    <div
      className="mn-chart__tooltip"
      data-side={side}
      style={{ left: x, top: y }}
      aria-hidden="true"
    >
      <div className="mn-chart__tooltip-title">{title}</div>
      {rows.length > 0 && (
        <ul className="mn-chart__tooltip-rows">
          {rows.map((row) => (
            <li key={row.key} className="mn-chart__tooltip-row">
              <span
                className="mn-chart__swatch"
                data-marker={row.marker ?? 'square'}
                data-dashed={row.dashed || undefined}
                style={colorStyle(row.color)}
              />
              <span className="mn-chart__tooltip-label">{row.label}</span>
              <span className="mn-chart__tooltip-value">{row.value}</span>
            </li>
          ))}
        </ul>
      )}
      {footer && <div className="mn-chart__tooltip-footer">{footer}</div>}
    </div>
  );
}

/** Announces the inspected position to screen readers (keyboard inspection only). */
export function ChartLiveRegion({ children }: { children: ReactNode }) {
  return (
    <div className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
      {children}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Axes and grid (SVG)                                                 */
/* ------------------------------------------------------------------ */

export interface ValueAxisProps {
  ticks: readonly number[];
  scale: LinearScale;
  format: (v: number) => string;
  orientation: 'vertical' | 'horizontal';
  /** Cross-axis extent of the grid lines (px). */
  from: number;
  to: number;
  /** Where tick labels sit: x for a vertical axis, y for a horizontal one. */
  labelAt: number;
}

/** Recessive grid lines plus value tick labels; the zero line is solid. */
export function ValueAxis({
  ticks,
  scale,
  format,
  orientation,
  from,
  to,
  labelAt,
}: ValueAxisProps) {
  return (
    <g className="mn-chart__axis" data-axis="value">
      {ticks.map((t) => {
        const p = scale(t);
        const zero = t === 0;
        return orientation === 'vertical' ? (
          <g key={t}>
            <line
              className="mn-chart__grid"
              data-zero={zero || undefined}
              x1={from}
              x2={to}
              y1={p}
              y2={p}
            />
            <text className="mn-chart__tick" x={labelAt} y={p} dy="0.32em" textAnchor="end">
              {format(t)}
            </text>
          </g>
        ) : (
          <g key={t}>
            <line
              className="mn-chart__grid"
              data-zero={zero || undefined}
              x1={p}
              x2={p}
              y1={from}
              y2={to}
            />
            <text className="mn-chart__tick" x={p} y={labelAt} dy="0.71em" textAnchor="middle">
              {format(t)}
            </text>
          </g>
        );
      })}
    </g>
  );
}

export interface CategoryLabel {
  key: string | number;
  at: number;
  label: string;
  anchor?: 'start' | 'middle' | 'end' | undefined;
}

/** Category or time labels along the x axis, or down the y axis for horizontal bars. */
export function CategoryAxis({
  labels,
  orientation,
  position,
}: {
  labels: readonly CategoryLabel[];
  orientation: 'horizontal' | 'vertical';
  /** y of the labels for a horizontal axis, x for a vertical one. */
  position: number;
}) {
  return (
    <g className="mn-chart__axis" data-axis="category">
      {labels.map((l) =>
        orientation === 'horizontal' ? (
          <text
            key={l.key}
            className="mn-chart__tick"
            x={l.at}
            y={position}
            dy="0.71em"
            textAnchor={l.anchor ?? 'middle'}
          >
            {l.label}
          </text>
        ) : (
          <text
            key={l.key}
            className="mn-chart__tick"
            x={position}
            y={l.at}
            dy="0.32em"
            textAnchor="end"
          >
            {l.label}
          </text>
        ),
      )}
    </g>
  );
}

export interface ChartReferenceLine {
  /** Value on the value axis. */
  value: number;
  /** Short label drawn at the end of the line and listed in the summary, for example "Target". */
  label?: string | undefined;
}

/** A dashed reference line (target, baseline, SLA) with its label. */
export function ReferenceLine({
  line,
  scale,
  orientation,
  from,
  to,
  format,
}: {
  line: ChartReferenceLine;
  scale: LinearScale;
  orientation: 'vertical' | 'horizontal';
  from: number;
  to: number;
  format: (v: number) => string;
}) {
  const p = scale(line.value);
  const text = line.label ? `${line.label} ${format(line.value)}` : format(line.value);
  return orientation === 'vertical' ? (
    <g className="mn-chart__reference">
      <line x1={from} x2={to} y1={p} y2={p} />
      <text x={to} y={p - 4} textAnchor="end">
        {text}
      </text>
    </g>
  ) : (
    <g className="mn-chart__reference">
      <line x1={p} x2={p} y1={from} y2={to} />
      <text x={p + 4} y={from + 10} textAnchor="start">
        {text}
      </text>
    </g>
  );
}

/* ------------------------------------------------------------------ */
/* Data table                                                          */
/* ------------------------------------------------------------------ */

/** The accessible data table: a real `<table>` with a caption and header scopes. */
export function ChartTable({ data, caption }: { data: ChartTableData; caption: ReactNode }) {
  const [head, ...rest] = data.columns;
  return (
    <table className="mn-chart__table">
      <caption className="mn-chart__table-caption">{data.caption ?? caption}</caption>
      <thead>
        <tr>
          {data.columns.map((c, i) => (
            <th key={c.key} scope="col" data-align={c.align ?? (i === 0 ? 'start' : 'end')}>
              {c.label}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {data.rows.map((row, r) => (
          <tr key={r}>
            {head && (
              <th scope="row" data-align={head.align ?? 'start'}>
                {row[head.key]}
              </th>
            )}
            {rest.map((c) => (
              <td key={c.key} data-align={c.align ?? 'end'}>
                {row[c.key]}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
