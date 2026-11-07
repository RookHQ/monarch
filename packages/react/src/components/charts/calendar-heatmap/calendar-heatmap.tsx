'use client';

import {
  forwardRef,
  useId,
  useMemo,
  useState,
  type FocusEvent,
  type KeyboardEvent,
  type PointerEvent,
} from 'react';
import {
  ChartLiveRegion,
  ChartTooltip,
  DEFAULT_INTERACTION_LABELS,
  colorStyle,
  createValueFormatter,
  useControllable,
  useElementWidth,
  type ChartColor,
  type ChartInteractionLabels,
  type ChartTableData,
  type ValueFormatter,
} from '../../../internal/charts/index.js';
import { ChartFrame, type ChartFrameLabels, type ChartFrameProps } from '../chart-frame/index.js';
import { HeatmapLegend, heatmapScale, type HeatmapLegendLabels } from '../heatmap/index.js';

/** One day's value. */
export interface CalendarHeatmapDay {
  /** ISO date, `YYYY-MM-DD`. Read as a calendar date (no time zone shifts). */
  date: string;
  /** The value; `null` for no data. */
  value: number | null;
  /** Replaces the formatted value in the tooltip, table and announcements, e.g. "7h 30m". */
  valueLabel?: string | undefined;
  /** A short note after the value, e.g. "Holiday" or "Leave". */
  note?: string | undefined;
  /** A non-working or out-of-scope day (weekend, holiday): drawn hollow unless it has a value. */
  inactive?: boolean | undefined;
  /** Draws a small dot in the cell to flag the day (explain why in `note`). */
  marker?: boolean | undefined;
}

/** `year`: weeks as columns and weekdays as rows. `month`: a calendar, weeks as rows. */
export type CalendarHeatmapLayout = 'year' | 'month';

/** Words the calendar heatmap adds to ChartFrame's; pass translations here. */
export interface CalendarHeatmapLabels
  extends ChartFrameLabels, Partial<ChartInteractionLabels>, HeatmapLegendLabels {
  /** Table column for dates. Default "Date". */
  date?: string | undefined;
  /** Table column and tooltip row for values. Default "Value". */
  value?: string | undefined;
  /** Table column for notes. Default "Note". */
  note?: string | undefined;
  /** Generated summary. */
  summary?:
    | ((days: number, first: string, last: string, total: string, best: string | null) => string)
    | undefined;
}

export interface CalendarHeatmapProps extends Omit<
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
  /** One entry per day that has something to show. Days in the range without an entry are drawn as "no data". */
  days: readonly CalendarHeatmapDay[];
  /** First day drawn (`YYYY-MM-DD`). Default: the earliest day in `days`. */
  from?: string | undefined;
  /** Last day drawn (`YYYY-MM-DD`). Default: the latest day in `days`. */
  to?: string | undefined;
  /** `year` (default) contribution-style grid, or `month` calendar with day numbers. Exposed as `data-layout`. */
  layout?: CalendarHeatmapLayout | undefined;
  /** First day of the week, 0 (Sunday) to 6. Default 1 (Monday). */
  weekStartsOn?: number | undefined;
  /** Today's date (`YYYY-MM-DD`), outlined. Pass it from the app so server and client agree. */
  today?: string | undefined;
  /** Palette slot of the sequential ramp. Default 1. */
  color?: ChartColor | undefined;
  /** Three ascending cut points for levels 2–4. Default: quarters of the largest value. */
  thresholds?: readonly number[] | undefined;
  /** Largest cell side in px. Default 14 (`year`) or 56 (`month`). */
  maxCellSize?: number | undefined;
  /** Month layout: print each day's value under its number. Default `true`. */
  showValues?: boolean | undefined;
  /** Show the "Less … More" legend. Default `true`. */
  showLegend?: boolean | undefined;
  /** BCP 47 locale for numbers, dates, weekday and month names. Pass it when rendering on the server. */
  locale?: string | undefined;
  /** `Intl.NumberFormat` options for values. */
  valueFormat?: Intl.NumberFormatOptions | undefined;
  /** Formats values. Wins over `valueFormat`. */
  formatValue?: ValueFormatter | undefined;
  /** Controlled inspected date (`null` for none). */
  activeDate?: string | null | undefined;
  onActiveDateChange?: ((date: string | null) => void) | undefined;
  /** Click, Enter or Space on a day, for drill-in (open the day). Days without an entry come with value `null`. */
  onDaySelect?: ((day: CalendarHeatmapDay) => void) | undefined;
  /** Replaces the generated text summary (the plot's accessible name). */
  summary?: string | undefined;
  labels?: CalendarHeatmapLabels | undefined;
}

const DAY = 86_400_000;
const FALLBACK_WIDTH = 640;
const GAP = 2;

/** Days since 1970-01-01 for a `YYYY-MM-DD` string, or NaN. */
function dayNumber(iso: string): number {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso);
  if (!m) return NaN;
  return Date.UTC(Number(m[1]), Number(m[2]) - 1, Number(m[3])) / DAY;
}
function isoOf(n: number): string {
  return new Date(n * DAY).toISOString().slice(0, 10);
}

const fmtCache = new Map<string, Intl.DateTimeFormat>();
function dateFormat(locale: string | undefined, opts: Intl.DateTimeFormatOptions) {
  const key = `${locale ?? ''}|${JSON.stringify(opts)}`;
  let f = fmtCache.get(key);
  if (!f) {
    f = new Intl.DateTimeFormat(locale, { ...opts, timeZone: 'UTC' });
    fmtCache.set(key, f);
  }
  return f;
}

/**
 * Values by day on a calendar grid: a contribution-style year (weeks as
 * columns) or a month calendar with day numbers. Days are binned into a
 * four-step ramp of one palette colour, with today outlined, notes and
 * markers, tooltips on hover, touch and keyboard, drill-in and a data table.
 */
export const CalendarHeatmap = forwardRef<HTMLElement, CalendarHeatmapProps>(
  function CalendarHeatmap(
    {
      days,
      from,
      to,
      layout = 'year',
      weekStartsOn = 1,
      today,
      color = 1,
      thresholds,
      maxCellSize,
      showValues = true,
      showLegend = true,
      locale,
      valueFormat,
      formatValue,
      activeDate,
      onActiveDateChange,
      onDaySelect,
      summary,
      labels,
      status = 'ready',
      footer,
      ...frame
    },
    ref,
  ) {
    const hintId = useId();
    const il = { ...DEFAULT_INTERACTION_LABELS, ...labels };
    const words = {
      date: labels?.date ?? 'Date',
      value: labels?.value ?? 'Value',
      note: labels?.note ?? 'Note',
    };
    const fmt = createValueFormatter(locale, valueFormat, formatValue);
    const fullDate = dateFormat(locale, {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
    const monthName = dateFormat(locale, { month: 'short' });
    const weekdayName = dateFormat(locale, { weekday: 'short' });
    const [measureRef, measured] = useElementWidth<HTMLDivElement>();
    const width = measured ?? FALLBACK_WIDTH;
    const year = layout === 'year';
    const ws = ((Math.floor(weekStartsOn) % 7) + 7) % 7;

    const byDay = useMemo(() => {
      const m = new Map<number, CalendarHeatmapDay>();
      for (const d of days) {
        const n = dayNumber(d.date);
        if (Number.isFinite(n)) m.set(n, d);
      }
      return m;
    }, [days]);
    const known = [...byDay.keys()];
    const start = from ? dayNumber(from) : known.length ? Math.min(...known) : NaN;
    const end = to ? dayNumber(to) : known.length ? Math.max(...known) : NaN;
    const valid = Number.isFinite(start) && Number.isFinite(end) && end >= start;
    const count = valid ? end - start + 1 : 0;
    // 1970-01-01 was a Thursday (4).
    const dow = (n: number) => (((n + 4) % 7) + 7) % 7;
    const lead = valid ? (dow(start) - ws + 7) % 7 : 0;
    const gridStart = start - lead;
    const weeks = valid ? Math.ceil((lead + count) / 7) : 0;

    const dayAt = (n: number): CalendarHeatmapDay =>
      byDay.get(n) ?? { date: isoOf(n), value: null };
    const scale = heatmapScale(
      days.map((d) => d.value),
      thresholds,
    );
    const valueText = (d: CalendarHeatmapDay) =>
      d.valueLabel ??
      (typeof d.value === 'number' && Number.isFinite(d.value) ? fmt(d.value) : il.noValue);
    const describe = (n: number) => {
      const d = dayAt(n);
      return `${fullDate.format(n * DAY)}: ${valueText(d)}${d.note ? `, ${d.note}` : ''}`;
    };

    // Layout.
    const left = year ? 32 : 0;
    const top = year ? 18 : 20;
    const cols = year ? weeks : 7;
    const rowsN = year ? 7 : weeks;
    const maxCell = maxCellSize ?? (year ? 14 : 56);
    const step =
      cols > 0 ? Math.max(4, Math.min(maxCell + GAP, (width - left) / Math.max(1, cols))) : 0;
    const cell = Math.max(2, step - GAP);
    const height = top + rowsN * step;
    const pos = (n: number) => {
      const i = n - gridStart;
      const col = year ? Math.floor(i / 7) : i % 7;
      const row = year ? i % 7 : Math.floor(i / 7);
      return { x: left + col * step, y: top + row * step };
    };

    // Month labels (year) at the first column of each month, skipped when crowded.
    const monthLabels: { key: number; x: number; text: string }[] = [];
    if (year && valid) {
      let lastX = -Infinity;
      for (let n = start; n <= end; n++) {
        const iso = isoOf(n);
        if (n !== start && !iso.endsWith('-01')) continue;
        const x = left + Math.floor((n - gridStart) / 7) * step;
        const text = monthName.format(n * DAY);
        if (x - lastX < text.length * 6.6 + 4) continue;
        monthLabels.push({ key: n, x, text });
        lastX = x;
      }
    }
    // Weekday labels: every other row in the year grid, every column in the month grid.
    const weekdayLabels = Array.from({ length: 7 }, (_, i) => {
      // 1970-01-04 (day 3) was a Sunday.
      const n = 3 + ((ws + i) % 7);
      return weekdayName.format(n * DAY);
    });

    // Active day.
    const [active, setActive] = useControllable<string | null>(
      activeDate,
      null,
      onActiveDateChange,
    );
    const [source, setSource] = useState<'pointer' | 'keyboard'>('pointer');
    const activeN = active ? dayNumber(active) : NaN;
    const hasActive = valid && activeN >= start && activeN <= end;
    const interactive = status === 'ready' && valid;
    const go = (n: number, from: 'pointer' | 'keyboard') => {
      const c = Math.max(start, Math.min(end, n));
      setSource(from);
      const iso = isoOf(c);
      if (iso !== active) setActive(iso);
    };
    const shiftMonth = (n: number, by: number) => {
      const d = new Date(n * DAY);
      const y = d.getUTCFullYear();
      const m = d.getUTCMonth() + by;
      const last = new Date(Date.UTC(y, m + 1, 0)).getUTCDate();
      return Date.UTC(y, m, Math.min(d.getUTCDate(), last)) / DAY;
    };

    const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
      if (!interactive) return;
      const cur = hasActive ? activeN : null;
      const across = year ? 7 : 1;
      const down = year ? 1 : 7;
      // Arrow keys stop at the edges of the range instead of jumping to its ends.
      const step = (by: number) =>
        cur !== null && (cur + by < start || cur + by > end) ? cur : (cur ?? 0) + by;
      switch (e.key) {
        case 'ArrowRight':
          go(cur === null ? start : step(across), 'keyboard');
          break;
        case 'ArrowLeft':
          go(cur === null ? end : step(-across), 'keyboard');
          break;
        case 'ArrowDown':
          go(cur === null ? start : step(down), 'keyboard');
          break;
        case 'ArrowUp':
          go(cur === null ? end : step(-down), 'keyboard');
          break;
        case 'Home':
          go(start, 'keyboard');
          break;
        case 'End':
          go(end, 'keyboard');
          break;
        case 'PageDown':
          go(shiftMonth(cur ?? start, 1), 'keyboard');
          break;
        case 'PageUp':
          go(shiftMonth(cur ?? start, -1), 'keyboard');
          break;
        case 'Escape':
          if (!hasActive) return;
          setActive(null);
          break;
        case 'Enter':
        case ' ':
          if (hasActive && onDaySelect) {
            e.preventDefault();
            onDaySelect(dayAt(activeN));
          }
          return;
        default:
          return;
      }
      e.preventDefault();
    };

    const fromPointer = (e: PointerEvent<HTMLDivElement>) => {
      const n = Number((e.target as Element).closest?.('[data-day]')?.getAttribute('data-day'));
      if (Number.isFinite(n) && n >= start && n <= end) go(n, 'pointer');
    };

    // Summary and table.
    const entries = [...byDay.entries()]
      .filter(([n]) => !valid || (n >= start && n <= end))
      .sort((a, b) => a[0] - b[0]);
    let total = 0;
    let best: [number, number] | null = null;
    for (const [n, d] of entries) {
      if (typeof d.value !== 'number' || !Number.isFinite(d.value)) continue;
      total += d.value;
      if (!best || d.value > best[1]) best = [n, d.value];
    }
    const top1 = best as [number, number] | null;
    const firstText = valid ? fullDate.format(start * DAY) : '';
    const lastText = valid ? fullDate.format(end * DAY) : '';
    const bestText = top1 ? `${fullDate.format(top1[0] * DAY)} ${valueText(dayAt(top1[0]))}` : null;
    const generated = labels?.summary
      ? labels.summary(count, firstText, lastText, fmt(total), bestText)
      : `${count} ${count === 1 ? 'day' : 'days'} from ${firstText} to ${lastText}. Total ${fmt(
          total,
        )}.${bestText ? ` Highest ${bestText}` : ''}`;
    const hasNotes = entries.some(([, d]) => d.note);
    const table: ChartTableData = {
      columns: [
        { key: 'date', label: words.date },
        { key: 'value', label: words.value },
        ...(hasNotes ? [{ key: 'note', label: words.note, align: 'start' as const }] : []),
      ],
      rows: entries.map(([n, d]) => ({
        date: fullDate.format(n * DAY),
        value: valueText(d),
        ...(hasNotes ? { note: d.note ?? '' } : {}),
      })),
    };
    const hasData = entries.length > 0;
    const resolvedStatus = status === 'ready' && (!valid || !hasData) ? 'empty' : status;
    const todayN = today ? dayNumber(today) : NaN;

    const cellsOut = [];
    if (valid) {
      for (let n = start; n <= end; n++) {
        const d = byDay.get(n);
        const level = scale.level(d?.value);
        const { x, y } = pos(n);
        const isActive = hasActive && n === activeN;
        cellsOut.push(
          <g key={n}>
            <rect
              className="mn-calendar-heatmap__cell"
              data-day={n}
              data-level={level ?? undefined}
              data-empty={level === null ? '' : undefined}
              data-inactive={d?.inactive ? '' : undefined}
              data-today={n === todayN ? '' : undefined}
              data-active={isActive ? '' : undefined}
              x={x}
              y={y}
              width={cell}
              height={cell}
              rx={Math.min(year ? 2 : 6, cell / 4)}
            />
            {!year && cell >= 28 ? (
              <text
                className="mn-calendar-heatmap__day"
                data-level={level ?? undefined}
                x={x + 5}
                y={y + 5}
                dy="0.8em"
              >
                {Number(isoOf(n).slice(8))}
              </text>
            ) : null}
            {!year && showValues && d && cell >= 44 && (d.value || d.valueLabel || d.note) ? (
              <text
                className="mn-calendar-heatmap__value"
                data-level={level ?? undefined}
                x={x + cell / 2}
                y={y + cell - 6}
                textAnchor="middle"
              >
                {d.valueLabel ?? (d.value ? fmt(d.value) : d.note)}
              </text>
            ) : null}
            {d?.marker && cell >= 8 ? (
              <circle
                className="mn-calendar-heatmap__marker"
                cx={x + cell - Math.max(2.5, cell / 7)}
                cy={y + Math.max(2.5, cell / 7)}
                r={Math.max(1.5, Math.min(3, cell / 9))}
              />
            ) : null}
          </g>,
        );
      }
    }

    const activeDay = hasActive ? dayAt(activeN) : null;
    const anchor = hasActive ? pos(activeN) : null;

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
        if (e.currentTarget.matches(':focus-visible')) go(start, 'keyboard');
      },
      onBlur: (e: FocusEvent<HTMLDivElement>) => {
        if (e.currentTarget.contains(e.relatedTarget as Node | null)) return;
        setActive(null);
      },
      onClick: () => {
        if (hasActive && onDaySelect) onDaySelect(dayAt(activeN));
      },
    };

    return (
      <ChartFrame
        ref={ref}
        {...frame}
        status={resolvedStatus}
        height={Math.max(year ? 120 : 200, height)}
        table={table}
        labels={labels}
        footer={
          showLegend && resolvedStatus === 'ready' ? (
            <>
              <HeatmapLegend
                scale={scale}
                color={color}
                format={fmt}
                labels={{ ...labels, noValue: il.noValue }}
              />
              {footer}
            </>
          ) : (
            footer
          )
        }
        data-chart="calendar-heatmap"
      >
        <div
          ref={measureRef}
          className="mn-chart__plot mn-calendar-heatmap"
          data-layout={layout}
          data-selectable={onDaySelect ? '' : undefined}
          role="group"
          aria-roledescription={il.chart}
          aria-label={summary ?? generated}
          aria-describedby={hintId}
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
            <g className="mn-chart__axis" data-axis="month">
              {monthLabels.map((m) => (
                <text key={m.key} className="mn-chart__tick" x={m.x} y={top - 6}>
                  {m.text}
                </text>
              ))}
            </g>
            <g className="mn-chart__axis" data-axis="weekday">
              {weekdayLabels.map((w, i) =>
                year ? (
                  i % 2 === 0 ? null : (
                    <text
                      key={i}
                      className="mn-chart__tick"
                      x={left - 6}
                      y={top + i * step + cell / 2}
                      dy="0.32em"
                      textAnchor="end"
                    >
                      {w}
                    </text>
                  )
                ) : (
                  <text
                    key={i}
                    className="mn-chart__tick"
                    x={left + i * step + cell / 2}
                    y={top - 6}
                    textAnchor="middle"
                  >
                    {w}
                  </text>
                ),
              )}
            </g>
            {cellsOut}
          </svg>
          {activeDay && anchor && (
            <ChartTooltip
              x={anchor.x + cell}
              y={anchor.y}
              width={width}
              title={fullDate.format(activeN * DAY)}
              rows={[{ key: 'v', label: words.value, value: valueText(activeDay), color }]}
              footer={activeDay.note}
            />
          )}
          <span id={hintId} className="mn-visually-hidden">
            {il.keyboardHint}
          </span>
          <ChartLiveRegion>
            {hasActive && source === 'keyboard' ? describe(activeN) : ''}
          </ChartLiveRegion>
        </div>
      </ChartFrame>
    );
  },
);
