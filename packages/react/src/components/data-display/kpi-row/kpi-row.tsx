import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import { StatCard, StatCardGroup, type StatCardProps } from '../stat-card/index.js';

/** One figure in a KPIRow: every StatCard prop, plus a stable key. */
export interface KPIRowItem extends Omit<StatCardProps, 'ref' | 'size' | 'variant'> {
  /** Stable key. */
  id: string;
}

export interface KPIRowProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /**
   * The figures, in order. Each is a StatCard: label, value, `change` (a
   * DeltaIndicator, polarity-aware), caption, hint, help, sparkline, link.
   */
  items: readonly KPIRowItem[];
  /** Names the row (`role="group"`), e.g. "This month". */
  label?: string | undefined;
  /** Columns on wide screens. Default: as many as fit at `minItemWidth`. */
  columns?: number | undefined;
  /** Narrowest a card gets in the automatic layout. Default `12rem`. */
  minItemWidth?: string | undefined;
  /** Card size for every figure. `sm` for dense rows, `lg` for a hero row. Default `md`. */
  size?: StatCardProps['size'];
  /** `card` (default) or `plain` (no border, for a row inside a Widget). */
  variant?: StatCardProps['variant'];
  /**
   * `wrap` (default) wraps cards onto new rows. `scroll` keeps one row and
   * scrolls it sideways with snap points on narrow screens, as many home pages
   * do for type tiles. The row is then a tab stop so keyboard users can
   * scroll it; give it a `label`.
   */
  overflow?: 'wrap' | 'scroll' | undefined;
  /** Shows every card's value placeholder. With no items yet, shows `loadingCount` placeholder cards. */
  loading?: boolean | undefined;
  /** Placeholder cards while loading with no items. Default 4. */
  loadingCount?: number | undefined;
  /** Replaces the row with this failure. */
  error?: ReactNode;
  /** With `error`, shows Try again calling this. */
  onRetry?: (() => unknown) | undefined;
  /** Text read for each placeholder card's label while loading with no items. Default "Loading". */
  loadingLabel?: string | undefined;
}

/**
 * A row of key figures at the top of a dashboard: StatCards with their
 * changes against the previous period, laid out responsively, with one
 * loading and error state for the whole row.
 */
export const KPIRow = forwardRef<HTMLDivElement, KPIRowProps>(function KPIRow(
  {
    items,
    label,
    columns,
    minItemWidth,
    size,
    variant,
    overflow = 'wrap',
    loading = false,
    loadingCount = 4,
    error,
    onRetry,
    loadingLabel = 'Loading',
    className,
    ...rest
  },
  ref,
) {
  const hasError = !loading && error !== undefined && error !== null && error !== false;
  if (hasError) {
    return (
      <div
        ref={ref}
        role={label ? 'group' : undefined}
        aria-label={label}
        {...rest}
        className={cx('mn-kpi-row', className)}
        data-state="error"
      >
        {onRetry ? (
          <InlineErrorRetry variant="banner" onRetry={onRetry}>
            {error}
          </InlineErrorRetry>
        ) : (
          <p className="mn-kpi-row__error" role="alert">
            {error}
          </p>
        )}
      </div>
    );
  }

  const cardProps = {
    ...(size !== undefined && { size }),
    ...(variant !== undefined && { variant }),
  };
  const placeholders = loading && items.length === 0;

  return (
    <StatCardGroup
      ref={ref}
      {...(label !== undefined && { label })}
      {...(columns !== undefined && { columns })}
      {...(minItemWidth !== undefined && { minItemWidth })}
      aria-busy={loading || undefined}
      // A sideways-scrolling row must be reachable by keyboard to scroll it.
      tabIndex={overflow === 'scroll' ? 0 : undefined}
      {...rest}
      className={cx('mn-kpi-row', className)}
      data-overflow={overflow}
      data-state={loading ? 'loading' : undefined}
    >
      {placeholders
        ? Array.from({ length: Math.max(1, loadingCount) }, (_, i) => (
            <StatCard
              key={i}
              {...cardProps}
              className="mn-kpi-row__item"
              label={<span className="mn-kpi-row__placeholder-label">{loadingLabel}</span>}
              value={null}
              loading
            />
          ))
        : items.map(({ id, className: itemClass, loading: itemLoading, ...item }) => (
            <StatCard
              key={id}
              {...cardProps}
              {...item}
              className={cx('mn-kpi-row__item', itemClass)}
              loading={loading || itemLoading === true}
            />
          ))}
    </StatCardGroup>
  );
});
