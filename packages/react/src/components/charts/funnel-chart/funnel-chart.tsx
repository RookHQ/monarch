'use client';

import { forwardRef, useCallback, useId, useRef } from 'react';
import {
  ChartLiveRegion,
  DEFAULT_INTERACTION_LABELS,
  colorStyle,
  createValueFormatter,
  useChartPointer,
  type ChartColor,
  type ChartInteractionLabels,
  type ChartTableData,
  type ValueFormatter,
} from '../../../internal/charts/index.js';
import { ChartFrame, type ChartFrameLabels, type ChartFrameProps } from '../chart-frame/index.js';
import { useClientLocale } from '../../../internal/dates/client-time.js';
import { maxOf } from '../../../internal/math.js';

/** One step of the funnel. */
export interface FunnelStage {
  /** Stable id, passed back from `onStageSelect`. */
  key: string;
  /** Name of the step, such as "Visited pricing". */
  label: string;
  /** How many reached this step. */
  value: number;
  /** Palette slot. Default 1 for every stage: a funnel is one measure. */
  color?: ChartColor | undefined;
}

/** Words the funnel renders; pass translations through `labels`. */
export interface FunnelChartTextLabels {
  /** Table header of the stage column. Default "Stage". */
  stage: string;
  /** Table header of the value column. Default "Count". */
  value: string;
  /** Table header and text of the step conversion. Default "From previous". */
  fromPrevious: string;
  /** Table header of the share of the first stage. Default "Of first". */
  ofFirst: string;
  /** Visible step line between two stages: "62.5% continued · 30 dropped". */
  step: (rate: string, dropped: string) => string;
  /** Summary sentence: "4 stages from Visited to Paid". */
  stages: (count: number, first: string, last: string) => string;
  /** Summary sentence: "Overall conversion 3.2%". */
  overall: (rate: string) => string;
  /** Summary sentence: "Largest drop: Visited to Signed up, 60% lost". */
  largestDrop: (from: string, to: string, lost: string) => string;
  /** Announced while inspecting a stage: "Signed up: 1,200, 40% of previous, 12% of first". */
  describe: (
    label: string,
    value: string,
    fromPrevious: string | undefined,
    ofFirst: string,
  ) => string;
}

const DEFAULT_TEXT: FunnelChartTextLabels = {
  stage: 'Stage',
  value: 'Count',
  fromPrevious: 'From previous',
  ofFirst: 'Of first',
  step: (rate, dropped) => `${rate} continued · ${dropped} dropped`,
  stages: (count, first, last) =>
    count === 1 ? `1 stage, ${first}` : `${count} stages from ${first} to ${last}`,
  overall: (rate) => `Overall conversion ${rate}`,
  largestDrop: (from, to, lost) => `Largest drop: ${from} to ${to}, ${lost} lost`,
  describe: (label, value, fromPrevious, ofFirst) =>
    fromPrevious === undefined
      ? `${label}: ${value}`
      : `${label}: ${value}, ${fromPrevious} of previous, ${ofFirst} of first`,
};

export type FunnelChartLabels = ChartFrameLabels &
  Partial<ChartInteractionLabels> &
  Partial<FunnelChartTextLabels>;

export interface FunnelChartProps extends Omit<
  ChartFrameProps,
  'children' | 'legend' | 'legendMarker' | 'onLegendToggle' | 'table' | 'labels' | 'height'
> {
  /** The steps, first (widest) to last. */
  stages: readonly FunnelStage[];
  /**
   * `center` (default) centres each bar so the stack reads as a funnel;
   * `start` aligns bars to the start edge for easier length comparison.
   * Exposed as `data-align`.
   */
  align?: 'center' | 'start' | undefined;
  /** Show the conversion line between stages. Default `true`. */
  showSteps?: boolean | undefined;
  /** BCP 47 locale for numbers. Pass it when rendering on the server. */
  locale?: string | undefined;
  /** `Intl.NumberFormat` options for stage values. */
  valueFormat?: Intl.NumberFormatOptions | undefined;
  /** Formats stage values. Wins over `valueFormat`. */
  formatValue?: ValueFormatter | undefined;
  /** Formats conversion rates (0–1). Default: percent with one decimal. */
  formatRate?: ValueFormatter | undefined;
  /** Controlled inspected stage (`null` for none). */
  activeIndex?: number | null | undefined;
  onActiveIndexChange?: ((index: number | null) => void) | undefined;
  /** Click, Enter or Space on the inspected stage, for drill-in (the people who dropped). */
  onStageSelect?: ((stage: FunnelStage, index: number) => void) | undefined;
  /** Replaces the generated text summary (the plot's accessible name). */
  summary?: string | undefined;
  labels?: FunnelChartLabels | undefined;
}

/**
 * Conversion funnel: how many reach each step, the share that continues
 * from one step to the next and what drops off, with the overall conversion
 * and the largest drop in the summary. Inspect stages by hover, touch or
 * keyboard; every value is also in the data table.
 */
export const FunnelChart = forwardRef<HTMLElement, FunnelChartProps>(function FunnelChart(
  {
    stages,
    align = 'center',
    showSteps = true,
    locale: localeProp,
    valueFormat,
    formatValue,
    formatRate,
    activeIndex,
    onActiveIndexChange,
    onStageSelect,
    summary,
    labels,
    status = 'ready',
    title,
    ...frame
  },
  ref,
) {
  const locale = useClientLocale(localeProp);
  const hintId = useId();
  const il = { ...DEFAULT_INTERACTION_LABELS, ...labels };
  const t = { ...DEFAULT_TEXT, ...labels };
  const fmtValue = createValueFormatter(locale, valueFormat, formatValue);
  const fmtRate = createValueFormatter(
    locale,
    { style: 'percent', maximumFractionDigits: 1 },
    formatRate,
  );
  const rows = useRef<(HTMLLIElement | null)[]>([]);

  const n = stages.length;
  const values = stages.map((s) => (Number.isFinite(s.value) ? Math.max(0, s.value) : 0));
  const first = values[0] ?? 0;
  const widest = Math.max(0, maxOf(values));
  const fromPrev = values.map((v, i) => {
    const prev = values[i - 1];
    return i === 0 || prev === undefined ? undefined : prev > 0 ? v / prev : 0;
  });
  const ofFirst = values.map((v) => (first > 0 ? v / first : 0));

  let drop: { from: number; lost: number } | undefined;
  fromPrev.forEach((rate, i) => {
    if (rate === undefined || !(values[i - 1] ?? 0)) return;
    const lost = 1 - rate;
    if (lost > 0 && (!drop || lost > drop.lost)) drop = { from: i - 1, lost };
  });

  const generated =
    n === 0
      ? il.noValue
      : [
          t.stages(n, stages[0]?.label ?? '', stages[n - 1]?.label ?? ''),
          ...(n > 1 ? [t.overall(fmtRate(ofFirst[n - 1] ?? 0))] : []),
          ...(drop
            ? [
                t.largestDrop(
                  stages[drop.from]?.label ?? '',
                  stages[drop.from + 1]?.label ?? '',
                  fmtRate(drop.lost),
                ),
              ]
            : []),
        ].join('. ');

  const indexAt = useCallback((_x: number, y: number) => {
    let best: number | null = null;
    let bestDist = Infinity;
    rows.current.forEach((row, i) => {
      if (!row) return;
      const mid = row.offsetTop + row.offsetHeight / 2;
      const d = Math.abs(mid - y);
      if (d < bestDist) {
        bestDist = d;
        best = i;
      }
    });
    return best;
  }, []);
  const { active, source, plotProps } = useChartPointer({
    count: status === 'ready' ? n : 0,
    activeIndex,
    onActiveIndexChange,
    indexAt,
    onSelect: onStageSelect
      ? (i) => {
          const s = stages[i];
          if (s) onStageSelect(s, i);
        }
      : undefined,
  });

  const describe = (i: number) => {
    const rate = fromPrev[i];
    return t.describe(
      stages[i]?.label ?? '',
      fmtValue(values[i] ?? 0),
      rate === undefined ? undefined : fmtRate(rate),
      fmtRate(ofFirst[i] ?? 0),
    );
  };

  const table: ChartTableData = {
    columns: [
      { key: 'stage', label: t.stage },
      { key: 'value', label: t.value },
      { key: 'prev', label: t.fromPrevious },
      { key: 'first', label: t.ofFirst },
    ],
    rows: stages.map((s, i) => {
      const rate = fromPrev[i];
      return {
        stage: s.label,
        value: fmtValue(values[i] ?? 0),
        prev: rate === undefined ? '' : fmtRate(rate),
        first: fmtRate(ofFirst[i] ?? 0),
      };
    }),
  };

  return (
    <ChartFrame
      ref={ref}
      {...frame}
      title={title}
      status={n === 0 && status === 'ready' ? 'empty' : status}
      height={Math.max(120, n * (showSteps ? 76 : 56))}
      table={table}
      labels={labels}
      data-chart="funnel"
    >
      <div
        className="mn-chart__plot mn-funnel-chart"
        data-align={align}
        data-selectable={onStageSelect ? '' : undefined}
        role="group"
        aria-roledescription={il.chart}
        aria-label={summary ?? generated}
        aria-describedby={hintId}
        {...plotProps}
      >
        <ol className="mn-funnel-chart__stages" aria-hidden="true">
          {stages.map((s, i) => {
            const rate = fromPrev[i];
            const share = widest > 0 ? (values[i] ?? 0) / widest : 0;
            const dropped = (values[i - 1] ?? 0) - (values[i] ?? 0);
            return (
              <li
                key={s.key}
                ref={(el) => {
                  rows.current[i] = el;
                }}
                className="mn-funnel-chart__stage"
                data-active={active === i ? '' : undefined}
                data-dimmed={active !== null && active !== i ? '' : undefined}
              >
                {showSteps && rate !== undefined ? (
                  <div className="mn-funnel-chart__step">
                    {t.step(fmtRate(rate), fmtValue(Math.max(0, dropped)))}
                  </div>
                ) : null}
                <div className="mn-funnel-chart__head">
                  <span className="mn-funnel-chart__label">{s.label}</span>
                  <span className="mn-funnel-chart__value">
                    {fmtValue(values[i] ?? 0)}
                    {i > 0 ? (
                      <span className="mn-funnel-chart__share">{fmtRate(ofFirst[i] ?? 0)}</span>
                    ) : null}
                  </span>
                </div>
                <div className="mn-funnel-chart__track">
                  <div
                    className="mn-funnel-chart__bar"
                    style={{
                      ...colorStyle(s.color ?? 1),
                      ['--_w' as string]: `${Math.max(share * 100, values[i] ? 0.5 : 0)}%`,
                    }}
                  />
                </div>
              </li>
            );
          })}
        </ol>
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
