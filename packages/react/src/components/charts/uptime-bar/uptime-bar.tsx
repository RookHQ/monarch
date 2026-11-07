'use client';

import {
  forwardRef,
  useId,
  useState,
  type FocusEvent,
  type HTMLAttributes,
  type KeyboardEvent,
  type PointerEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  ChartLiveRegion,
  ChartTable,
  DEFAULT_INTERACTION_LABELS,
  createValueFormatter,
  useControllable,
  type ChartInteractionLabels,
  type ChartTableData,
} from '../../../internal/charts/index.js';

/**
 * State of one period. Status colours are allowed here because they carry
 * status, not a series: every state also has a text label.
 */
export type UptimeStatus =
  'operational' | 'degraded' | 'partial' | 'down' | 'maintenance' | 'nodata';

/** An extra line in the tooltip and table, e.g. checks or p95 response time. */
export interface UptimeSegmentDetail {
  label: string;
  value: string | number;
}

/** One period of the bar, usually a day. */
export interface UptimeSegment {
  /** Stable id (React key), e.g. the ISO date. */
  key: string;
  /** The period as text, e.g. "Mar 4". Shown in the tooltip, table and announcements. */
  label: string;
  /** The state. Default: derived from `uptime` with `thresholds` (`nodata` when `uptime` is missing). */
  status?: UptimeStatus | undefined;
  /** Share of checks that passed, in percent (0–100); `null` when nothing was checked. */
  uptime?: number | null | undefined;
  /** Extra lines (checks, average and p95 response time…). */
  details?: readonly UptimeSegmentDetail[] | undefined;
  /** A short note, e.g. the incident title. */
  note?: string | undefined;
}

/** Cut points used when a segment has `uptime` but no `status`. */
export interface UptimeThresholds {
  /** At or above: operational. Default 99.9. */
  operational: number;
  /** At or above (and below `operational`): degraded. Below: down. Default 99. */
  degraded: number;
}

/** Text the uptime bar renders; pass translations here. */
export interface UptimeBarLabels extends Partial<ChartInteractionLabels> {
  /** Names of the states, in the tooltip, legend, table and summary. */
  status?: Partial<Record<UptimeStatus, string>> | undefined;
  /** Tooltip and table label of the uptime value. Default "Uptime". */
  uptime?: string | undefined;
  /** Table column for the period. Default "Period". */
  period?: string | undefined;
  /** Table column for the state. Default "Status". */
  state?: string | undefined;
  /** Table column for notes and details. Default "Details". */
  details?: string | undefined;
  /** After the overall percentage. Default "uptime". */
  uptimeSuffix?: string | undefined;
  /** Name of the legend list. Default "Legend". */
  legend?: string | undefined;
  /** Generated summary; receives the segment count and the formatted overall uptime (or null). */
  summary?:
    | ((
        count: number,
        counts: Partial<Record<UptimeStatus, number>>,
        uptime: string | null,
      ) => string)
    | undefined;
}

export const DEFAULT_UPTIME_STATUS_LABELS: Record<UptimeStatus, string> = {
  operational: 'Operational',
  degraded: 'Degraded',
  partial: 'Partial outage',
  down: 'Down',
  maintenance: 'Maintenance',
  nodata: 'No data',
};

const STATUS_ORDER: readonly UptimeStatus[] = [
  'operational',
  'degraded',
  'partial',
  'down',
  'maintenance',
  'nodata',
];

/** The state of a segment: its own `status`, or one derived from `uptime`. */
export function uptimeStatus(
  segment: Pick<UptimeSegment, 'status' | 'uptime'>,
  thresholds: UptimeThresholds = { operational: 99.9, degraded: 99 },
): UptimeStatus {
  if (segment.status) return segment.status;
  const u = segment.uptime;
  if (typeof u !== 'number' || !Number.isFinite(u)) return 'nodata';
  if (u >= thresholds.operational) return 'operational';
  if (u >= thresholds.degraded) return 'degraded';
  return 'down';
}

export interface UptimeLegendProps extends HTMLAttributes<HTMLUListElement> {
  /** States to list, in order. Default: operational, degraded, partial, down, maintenance, no data. */
  statuses?: readonly UptimeStatus[] | undefined;
  labels?: Pick<UptimeBarLabels, 'status' | 'legend'> | undefined;
}

/**
 * Names the uptime colours once for a page of uptime bars. Every entry has
 * its text label, so the colours are never the only encoding.
 */
export const UptimeLegend = forwardRef<HTMLUListElement, UptimeLegendProps>(function UptimeLegend(
  { statuses = STATUS_ORDER, labels, className, ...rest },
  ref,
) {
  const names = { ...DEFAULT_UPTIME_STATUS_LABELS, ...labels?.status };
  return (
    <ul
      ref={ref}
      aria-label={labels?.legend ?? 'Legend'}
      {...rest}
      className={cx('mn-uptime-legend', className)}
    >
      {statuses.map((s) => (
        <li key={s} className="mn-uptime-legend__item">
          <span className="mn-uptime-bar__swatch" data-status={s} aria-hidden="true" />
          {names[s]}
        </li>
      ))}
    </ul>
  );
});

export interface UptimeBarProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** The periods, oldest first. */
  segments: readonly UptimeSegment[];
  /** What is monitored, shown above the bar and naming it, e.g. a service name. */
  label?: ReactNode | undefined;
  /** Short text under the label, e.g. a description. */
  description?: ReactNode | undefined;
  /** Content at the end of the header, e.g. a status Badge or the response time. */
  meta?: ReactNode | undefined;
  /** Overall uptime in percent shown under the bar. Default: the mean of the segments' `uptime`. Pass `null` to hide it. */
  uptime?: number | null | undefined;
  /** Text under the oldest segment, e.g. "30 days ago". */
  startLabel?: ReactNode | undefined;
  /** Text under the newest segment, e.g. "Today". */
  endLabel?: ReactNode | undefined;
  /** Cut points when segments give `uptime` without a `status`. Default 99.9 / 99. */
  thresholds?: UptimeThresholds | undefined;
  /** Bar height: `md` (32px, default) for status pages, `sm` (20px) for tiles and table rows. Exposed as `data-size`. */
  size?: 'sm' | 'md' | undefined;
  /** Show the legend under the bar. Default `false`: with several bars, render one `UptimeLegend` for the page. */
  showLegend?: boolean | undefined;
  /** Keep a data table for screen readers. Default `true`. */
  table?: boolean | undefined;
  /** BCP 47 locale for percentages. Pass it when rendering on the server. */
  locale?: string | undefined;
  /** Decimal places of uptime percentages. Default 2. */
  uptimeDigits?: number | undefined;
  /** Controlled inspected segment (`null` for none). */
  activeIndex?: number | null | undefined;
  onActiveIndexChange?: ((index: number | null) => void) | undefined;
  /** Click, Enter or Space on a segment, e.g. to open that day's incidents. */
  onSegmentSelect?: ((segment: UptimeSegment, index: number) => void) | undefined;
  /** Replaces the generated text summary of the bar. */
  summary?: string | undefined;
  labels?: UptimeBarLabels | undefined;
}

/**
 * A row of per-period status segments (usually one per day) for uptime and
 * status pages: status colours with text labels, a tooltip on hover, touch
 * and keyboard with the exact uptime and details, overall uptime, drill-in
 * and a data table for screen readers.
 */
export const UptimeBar = forwardRef<HTMLDivElement, UptimeBarProps>(function UptimeBar(
  {
    segments,
    label,
    description,
    meta,
    uptime,
    startLabel,
    endLabel,
    thresholds,
    size = 'md',
    showLegend = false,
    table: withTable = true,
    locale,
    uptimeDigits = 2,
    activeIndex,
    onActiveIndexChange,
    onSegmentSelect,
    summary,
    labels,
    className,
    ...rest
  },
  ref,
) {
  const id = useId();
  const labelId = `${id}-label`;
  const summaryId = `${id}-summary`;
  const hintId = `${id}-hint`;
  const il = { ...DEFAULT_INTERACTION_LABELS, ...labels };
  const names = { ...DEFAULT_UPTIME_STATUS_LABELS, ...labels?.status };
  const words = {
    uptime: labels?.uptime ?? 'Uptime',
    period: labels?.period ?? 'Period',
    state: labels?.state ?? 'Status',
    details: labels?.details ?? 'Details',
    suffix: labels?.uptimeSuffix ?? 'uptime',
  };
  const pct = createValueFormatter(locale, {
    minimumFractionDigits: uptimeDigits,
    maximumFractionDigits: uptimeDigits,
  });
  const fmtPct = (v: number) => `${pct(v)}%`;
  const n = segments.length;
  const statuses = segments.map((s) => uptimeStatus(s, thresholds));

  const measured = segments
    .map((s) => s.uptime)
    .filter((u): u is number => typeof u === 'number' && Number.isFinite(u));
  const overall =
    uptime === undefined
      ? measured.length
        ? measured.reduce((t, u) => t + u, 0) / measured.length
        : null
      : uptime;

  const counts: Partial<Record<UptimeStatus, number>> = {};
  for (const s of statuses) counts[s] = (counts[s] ?? 0) + 1;
  const overallText = overall === null ? null : fmtPct(overall);
  const generated = labels?.summary
    ? labels.summary(n, counts, overallText)
    : `${n} ${n === 1 ? 'period' : 'periods'}: ${STATUS_ORDER.filter((s) => counts[s])
        .map((s) => `${counts[s]} ${names[s].toLowerCase()}`)
        .join(', ')}.${overallText ? ` ${words.uptime} ${overallText}.` : ''}`;

  const [active, setActive] = useControllable<number | null>(
    activeIndex,
    null,
    onActiveIndexChange,
  );
  const [source, setSource] = useState<'pointer' | 'keyboard'>('pointer');
  const current = active !== null && active >= 0 && active < n ? active : null;
  const go = (i: number, from: 'pointer' | 'keyboard') => {
    setSource(from);
    setActive(Math.max(0, Math.min(n - 1, i)));
  };

  const segmentText = (i: number) => {
    const s = segments[i];
    if (!s) return '';
    const parts = [names[statuses[i] ?? 'nodata']];
    if (typeof s.uptime === 'number' && Number.isFinite(s.uptime)) {
      parts.push(`${words.uptime} ${fmtPct(s.uptime)}`);
    }
    for (const d of s.details ?? []) parts.push(`${d.label} ${d.value}`);
    if (s.note) parts.push(s.note);
    return `${s.label}: ${parts.join(', ')}`;
  };

  const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    if (n === 0) return;
    const page = Math.max(1, Math.ceil(n / 10));
    switch (e.key) {
      case 'ArrowRight':
      case 'ArrowDown':
        go(current === null ? 0 : current + 1, 'keyboard');
        break;
      case 'ArrowLeft':
      case 'ArrowUp':
        go(current === null ? n - 1 : current - 1, 'keyboard');
        break;
      case 'Home':
        go(0, 'keyboard');
        break;
      case 'End':
        go(n - 1, 'keyboard');
        break;
      case 'PageDown':
        go((current ?? 0) + page, 'keyboard');
        break;
      case 'PageUp':
        go((current ?? 0) - page, 'keyboard');
        break;
      case 'Escape':
        if (current === null) return;
        setActive(null);
        break;
      case 'Enter':
      case ' ':
        if (current !== null && onSegmentSelect && segments[current]) {
          e.preventDefault();
          onSegmentSelect(segments[current], current);
        }
        return;
      default:
        return;
    }
    e.preventDefault();
  };

  const fromPointer = (e: PointerEvent<HTMLDivElement>) => {
    const i = Number((e.target as Element).closest?.('[data-index]')?.getAttribute('data-index'));
    if (Number.isInteger(i)) go(i, 'pointer');
  };

  const hasDetails = segments.some((s) => s.note || s.details?.length);
  const tableData: ChartTableData = {
    columns: [
      { key: 'period', label: words.period },
      { key: 'state', label: words.state, align: 'start' },
      { key: 'uptime', label: words.uptime },
      ...(hasDetails ? [{ key: 'details', label: words.details, align: 'start' as const }] : []),
    ],
    rows: segments.map((s, i) => ({
      period: s.label,
      state: names[statuses[i] ?? 'nodata'],
      uptime:
        typeof s.uptime === 'number' && Number.isFinite(s.uptime) ? fmtPct(s.uptime) : il.noValue,
      ...(hasDetails
        ? {
            details: [...(s.details ?? []).map((d) => `${d.label} ${d.value}`), s.note ?? '']
              .filter(Boolean)
              .join(', '),
          }
        : {}),
    })),
  };

  const activeSeg = current === null ? null : segments[current];
  const hasHeader = label !== undefined || description !== undefined || meta !== undefined;
  const hasFooter = startLabel !== undefined || endLabel !== undefined || overallText !== null;

  const plotProps = {
    tabIndex: n > 0 ? 0 : -1,
    onKeyDown: onKeyDown,
    onPointerMove: fromPointer,
    onPointerDown: fromPointer,
    onPointerLeave: (e: PointerEvent<HTMLDivElement>) => {
      if (e.pointerType === 'mouse' && source === 'pointer') setActive(null);
    },
    onFocus: (e: FocusEvent<HTMLDivElement>) => {
      if (e.target !== e.currentTarget || current !== null || n === 0) return;
      if (e.currentTarget.matches(':focus-visible')) go(n - 1, 'keyboard');
    },
    onBlur: (e: FocusEvent<HTMLDivElement>) => {
      if (e.currentTarget.contains(e.relatedTarget as Node | null)) return;
      setActive(null);
    },
    onClick: () => {
      const seg = current === null ? undefined : segments[current];
      if (seg && current !== null && onSegmentSelect) onSegmentSelect(seg, current);
    },
  };

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-uptime-bar', className)}
      data-size={size}
      data-empty={n === 0 ? '' : undefined}
    >
      {hasHeader && (
        <div className="mn-uptime-bar__header">
          <div className="mn-uptime-bar__heading">
            {label !== undefined && (
              <div id={labelId} className="mn-uptime-bar__label">
                {label}
              </div>
            )}
            {description !== undefined && (
              <div className="mn-uptime-bar__description">{description}</div>
            )}
          </div>
          {meta !== undefined && <div className="mn-uptime-bar__meta">{meta}</div>}
        </div>
      )}
      <div
        className="mn-chart__plot mn-uptime-bar__plot"
        role="group"
        aria-roledescription={il.chart}
        aria-labelledby={label !== undefined ? `${labelId} ${summaryId}` : summaryId}
        aria-describedby={hintId}
        data-selectable={onSegmentSelect ? '' : undefined}
        {...plotProps}
      >
        <div className="mn-uptime-bar__segments" aria-hidden="true">
          {segments.map((s, i) => (
            <span
              key={s.key}
              className="mn-uptime-bar__segment"
              data-index={i}
              data-status={statuses[i]}
              data-active={i === current ? '' : undefined}
            />
          ))}
        </div>
        {activeSeg && current !== null && (
          <div
            className="mn-chart__tooltip mn-uptime-bar__tooltip"
            data-side={current > n / 2 ? 'left' : 'right'}
            style={{ left: `${((current + 0.5) / n) * 100}%` }}
            aria-hidden="true"
          >
            <div className="mn-chart__tooltip-title">{activeSeg.label}</div>
            <ul className="mn-chart__tooltip-rows">
              <li className="mn-chart__tooltip-row">
                <span className="mn-uptime-bar__swatch" data-status={statuses[current]} />
                <span className="mn-chart__tooltip-label">
                  {names[statuses[current] ?? 'nodata']}
                </span>
                {typeof activeSeg.uptime === 'number' && Number.isFinite(activeSeg.uptime) && (
                  <span className="mn-chart__tooltip-value">{fmtPct(activeSeg.uptime)}</span>
                )}
              </li>
              {(activeSeg.details ?? []).map((d) => (
                <li key={d.label} className="mn-chart__tooltip-row">
                  <span className="mn-chart__tooltip-label">{d.label}</span>
                  <span className="mn-chart__tooltip-value">{d.value}</span>
                </li>
              ))}
            </ul>
            {activeSeg.note && <div className="mn-chart__tooltip-footer">{activeSeg.note}</div>}
          </div>
        )}
        <span id={summaryId} className="mn-visually-hidden">
          {summary ?? generated}
        </span>
        <span id={hintId} className="mn-visually-hidden">
          {il.keyboardHint}
        </span>
        <ChartLiveRegion>
          {current !== null && source === 'keyboard' ? segmentText(current) : ''}
        </ChartLiveRegion>
      </div>
      {hasFooter && (
        <div className="mn-uptime-bar__footer" aria-hidden="true">
          <span>{startLabel}</span>
          {overallText !== null && (
            <span className="mn-uptime-bar__overall">
              {overallText} {words.suffix}
            </span>
          )}
          <span>{endLabel}</span>
        </div>
      )}
      {showLegend && <UptimeLegend labels={labels} className="mn-uptime-bar__legend" />}
      {withTable && n > 0 && (
        <div className="mn-visually-hidden">
          <ChartTable data={tableData} caption={label ?? summary ?? generated} />
        </div>
      )}
    </div>
  );
});
