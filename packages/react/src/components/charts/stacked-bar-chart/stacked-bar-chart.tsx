'use client';

import { forwardRef, useMemo } from 'react';
import { cx } from '../../../internal/cx.js';
import { createValueFormatter, numberAt, type ChartDatum } from '../../../internal/charts/index.js';
import { BarChart, type BarChartLayout, type BarChartProps } from '../bar-chart/index.js';
import { useClientLocale } from '../../../internal/dates/client-time.js';

/** `normal` stacks values; `expand` stacks each category's shares of 100%. */
export type StackedBarChartOffset = 'normal' | 'expand';

export interface StackedBarChartProps extends Omit<BarChartProps, 'layout'> {
  /**
   * `stacked` (default) draws one bar per category made of its series;
   * `grouped` puts the series side by side. Exposed as `data-layout`.
   */
  layout?: BarChartLayout | undefined;
  /**
   * `normal` (default) stacks the values. `expand` stacks each category's
   * shares so every bar is 100% long; the tooltip, table and summary then
   * show shares. Only applies when stacked. Exposed as `data-offset`.
   */
  offset?: StackedBarChartOffset | undefined;
}

/**
 * Stacked and grouped bar chart: how a total splits into parts, per
 * category, vertical or horizontal, as values or as shares of 100%. It is
 * BarChart with stacking on by default, plus the 100% offset, so tooltips,
 * keyboard inspection, legend toggles and the data table behave the same.
 */
export const StackedBarChart = forwardRef<HTMLElement, StackedBarChartProps>(
  function StackedBarChart(
    {
      layout = 'stacked',
      offset = 'normal',
      data,
      series,
      hiddenSeries,
      defaultHiddenSeries,
      locale: localeProp,
      formatValue,
      formatTick,
      valueDomain,
      referenceLines,
      onBarSelect,
      className,
      ...rest
    },
    ref,
  ) {
    const locale = useClientLocale(localeProp);
    const expand = layout === 'stacked' && offset === 'expand';
    // Shares are computed over every series, so hiding one in the legend does
    // not silently rescale the others (the hidden part leaves a gap at the end).
    const shares = useMemo<readonly ChartDatum[]>(() => {
      if (!expand) return data;
      return data.map((d) => {
        const total = series.reduce((sum, s) => sum + Math.max(0, numberAt(d, s.key) ?? 0), 0);
        const row: ChartDatum = { ...d };
        for (const s of series) {
          const v = numberAt(d, s.key);
          row[s.key] = v === null ? null : total ? Math.max(0, v) / total : 0;
        }
        return row;
      });
    }, [data, series, expand]);
    const percent = createValueFormatter(locale, { style: 'percent', maximumFractionDigits: 1 });
    const percentTick = createValueFormatter(locale, {
      style: 'percent',
      maximumFractionDigits: 0,
    });

    return (
      <BarChart
        ref={ref}
        {...rest}
        className={cx('mn-stacked-bar-chart', className)}
        data={shares}
        series={series}
        layout={layout}
        hiddenSeries={hiddenSeries}
        defaultHiddenSeries={defaultHiddenSeries}
        locale={locale}
        formatValue={expand ? percent : formatValue}
        formatTick={expand ? percentTick : formatTick}
        valueDomain={expand ? [0, 1] : valueDomain}
        referenceLines={expand ? undefined : referenceLines}
        onBarSelect={
          onBarSelect ? (_datum, index) => onBarSelect(data[index] ?? {}, index) : undefined
        }
        data-offset={layout === 'stacked' ? offset : undefined}
      />
    );
  },
);
