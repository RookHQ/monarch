'use client';

import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { WarningGlyph } from '../../../internal/icons.js';
import {
  ChartLegend,
  ChartTable,
  useControllable,
  type ChartLegendItem,
  type ChartLegendMarker,
  type ChartTableData,
} from '../../../internal/charts/index.js';
import { Button } from '../../actions/button/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';

export {
  chartColor,
  type ChartColor,
  type ChartDatum,
  type ChartLegendItem,
  type ChartLegendMarker,
  type ChartSeries,
  type ChartTableColumn,
  type ChartTableData,
} from '../../../internal/charts/index.js';

export type ChartFrameStatus = 'ready' | 'loading' | 'empty' | 'error';
export type ChartFrameHeadingLevel = 2 | 3 | 4 | 5 | 6;

/** Text the frame renders itself; pass translations here. */
export interface ChartFrameLabels {
  /** Toggle text while the chart shows. Default "View as table". */
  viewTable?: string | undefined;
  /** Toggle text while the table shows. Default "View as chart". */
  viewChart?: string | undefined;
  /** Announced while loading. Default "Loading chart". */
  loading?: string | undefined;
  /** Empty-state title. Default "No data yet". */
  emptyTitle?: string | undefined;
  /** Error message. Default "This chart didn't load." */
  error?: string | undefined;
  /** Name of the legend list. Default "Legend". */
  legend?: string | undefined;
}

const DEFAULT_LABELS: Required<ChartFrameLabels> = {
  viewTable: 'View as table',
  viewChart: 'View as chart',
  loading: 'Loading chart',
  emptyTitle: 'No data yet',
  error: "This chart didn't load.",
  legend: 'Legend',
};

export interface ChartFrameProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The chart's name. Labels the figure. Required. */
  title: ReactNode;
  /** One line under the title: the period, unit or what the chart answers. Describes the figure. */
  description?: ReactNode | undefined;
  /** Render the title as a heading (`h2`–`h6`) to match the page outline. Default: not a heading. */
  headingLevel?: ChartFrameHeadingLevel | undefined;
  /** Period or metric controls in the header, such as a pill TabsFilter or a Select. */
  actions?: ReactNode | undefined;
  /** Legend entries, shown under the header while the chart is visible. */
  legend?: readonly ChartLegendItem[] | undefined;
  /** Legend marker shape. Default `line`. */
  legendMarker?: ChartLegendMarker | undefined;
  /** Makes legend entries toggle buttons; called with the series key. */
  onLegendToggle?: ((key: string) => void) | undefined;
  /**
   * `ready` (default) shows the chart; `loading` a skeleton with
   * `aria-busy`; `empty` an empty state; `error` an error with Try again
   * when `onRetry` is set. Exposed as `data-status`.
   */
  status?: ChartFrameStatus | undefined;
  /** Empty-state description, such as what will appear and when. */
  emptyDescription?: ReactNode | undefined;
  /** Actions in the empty state, such as a link to set up tracking. */
  emptyActions?: ReactNode | undefined;
  /** Error-state retry. Return the refetch promise to keep the button busy until it settles. */
  onRetry?: (() => unknown) | undefined;
  /**
   * The accessible data table: every value the chart shows, as text. It is
   * always in the page for screen readers, and visible through the
   * "View as table" toggle when `tableToggle` is on.
   */
  table?: ChartTableData | undefined;
  /** Show the "View as table" toggle. Default `true` when `table` is set. */
  tableToggle?: boolean | undefined;
  /** Controlled table view. */
  showTable?: boolean | undefined;
  /** Uncontrolled initial table view. Default `false`. */
  defaultShowTable?: boolean | undefined;
  onShowTableChange?: ((showTable: boolean) => void) | undefined;
  /** Height reserved for the chart body (loading, empty and error keep it, so nothing jumps). Default 240. */
  height?: number | undefined;
  /** Notes under the chart: source, last updated, a footnote. */
  footer?: ReactNode | undefined;
  labels?: ChartFrameLabels | undefined;
  /** The chart. */
  children?: ReactNode | undefined;
}

/**
 * The container every Monarch chart sits in: a `<figure>` with a title,
 * description, controls slot, legend, loading / empty / error states and an
 * accessible data table ("View as table", always present for screen readers).
 */
export const ChartFrame = forwardRef<HTMLElement, ChartFrameProps>(function ChartFrame(
  {
    title,
    description,
    headingLevel,
    actions,
    legend,
    legendMarker = 'line',
    onLegendToggle,
    status = 'ready',
    emptyDescription,
    emptyActions,
    onRetry,
    table,
    tableToggle,
    showTable,
    defaultShowTable = false,
    onShowTableChange,
    height = 240,
    footer,
    labels,
    className,
    children,
    style,
    ...rest
  },
  ref,
) {
  const id = useId();
  const titleId = `${id}-title`;
  const descId = `${id}-desc`;
  const bodyId = `${id}-body`;
  const l = { ...DEFAULT_LABELS, ...labels };
  const [asTable, setAsTable] = useControllable(showTable, defaultShowTable, onShowTableChange);
  const canToggle = (tableToggle ?? true) && table !== undefined && status === 'ready';
  const tableVisible = canToggle && asTable;
  const Title = headingLevel ? (`h${headingLevel}` as const) : 'div';

  let body: ReactNode;
  if (status === 'loading') {
    body = (
      <div className="mn-chart-frame__state" aria-busy="true">
        <Skeleton className="mn-chart-frame__skeleton" />
        <span className="mn-visually-hidden" role="status">
          {l.loading}
        </span>
      </div>
    );
  } else if (status === 'empty') {
    body = (
      <div className="mn-chart-frame__state">
        <EmptyState
          size="compact"
          headingLevel="p"
          title={l.emptyTitle}
          description={emptyDescription}
          actions={emptyActions}
        />
      </div>
    );
  } else if (status === 'error') {
    body = (
      <div className="mn-chart-frame__state">
        {onRetry ? (
          <InlineErrorRetry onRetry={onRetry}>{l.error}</InlineErrorRetry>
        ) : (
          <p className="mn-chart-frame__error" role="alert">
            <WarningGlyph aria-hidden="true" />
            {l.error}
          </p>
        )}
      </div>
    );
  } else if (tableVisible && table) {
    body = (
      // A scrollable region must be focusable so keyboard users can scroll it (WCAG 2.1.1).
      // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
      <div className="mn-chart-frame__table" role="region" aria-labelledby={titleId} tabIndex={0}>
        <ChartTable data={table} caption={title} />
      </div>
    );
  } else {
    body = (
      <>
        <div className="mn-chart-frame__chart">{children}</div>
        {table && (
          <div className="mn-visually-hidden">
            <ChartTable data={table} caption={title} />
          </div>
        )}
      </>
    );
  }

  return (
    <figure
      ref={ref}
      {...rest}
      aria-labelledby={titleId}
      aria-describedby={description ? descId : undefined}
      data-status={status}
      data-view={tableVisible ? 'table' : 'chart'}
      className={cx('mn-chart-frame', className)}
      style={{ ['--_h' as string]: `${height}px`, ...style }}
    >
      <div className="mn-chart-frame__header">
        <div className="mn-chart-frame__heading">
          <Title id={titleId} className="mn-chart-frame__title">
            {title}
          </Title>
          {description && (
            <p id={descId} className="mn-chart-frame__description">
              {description}
            </p>
          )}
        </div>
        {(actions || canToggle) && (
          <div className="mn-chart-frame__actions">
            {actions}
            {canToggle && (
              <Button
                variant="ghost"
                size="sm"
                aria-controls={bodyId}
                className="mn-chart-frame__toggle"
                onClick={() => setAsTable(!asTable)}
              >
                {asTable ? l.viewChart : l.viewTable}
              </Button>
            )}
          </div>
        )}
      </div>
      {legend && legend.length > 0 && status === 'ready' && !tableVisible && (
        <ChartLegend
          items={legend}
          marker={legendMarker}
          onToggle={onLegendToggle}
          label={l.legend}
          className="mn-chart-frame__legend"
        />
      )}
      <div id={bodyId} className="mn-chart-frame__body">
        {body}
      </div>
      {footer && <div className="mn-chart-frame__footer">{footer}</div>}
    </figure>
  );
});
