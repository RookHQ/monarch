import { forwardRef, type ElementType, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { formatNumber } from '../../../internal/format-number.js';
import { formatMoney } from '../../billing/money/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import { VisuallyHidden } from '../../layout/visually-hidden/index.js';

/** One row of a BarList. */
export interface BarListItem {
  /** Stable React key. Default: the row's position. */
  id?: string | number;
  /** What the row is, e.g. a page path or a referrer. */
  label: ReactNode;
  /** The measured value; `null` shows a dash read as `missingLabel` and no bar. */
  value: number | null;
  /** A second, muted line under the label, e.g. the campaign or host. */
  description?: ReactNode;
  /** A small decorative leading visual (favicon, flag, avatar). Hidden from assistive technology. */
  icon?: ReactNode;
  /** Makes the label a link; the whole row is the click target. */
  href?: string;
  /** Shown instead of the formatted value, e.g. "12% · 4 of 33". */
  displayValue?: ReactNode;
  /** Emphasises the row, e.g. "you" in a comparison. Say why in the label too: emphasis is not announced. */
  highlighted?: boolean;
}

export interface BarListProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  items: BarListItem[];
  /** Names the list for assistive technology, e.g. "Top pages". */
  label?: string;
  /** Numbers the rows and renders an ordered list. Default `false`. */
  ranked?: boolean;
  /** Row order. `desc` (default) puts the largest first; `none` keeps the given order. Missing values go last. */
  sort?: 'desc' | 'asc' | 'none';
  /** The value of a full bar. Default: the largest value. Pass `1` for ratios of a whole. */
  max?: number;
  /** Shows only the first `limit` rows, with a "Top 5 of 12" footer. */
  limit?: number;
  /** Replaces the default "Top {shown} of {total}" footer when rows are cut by `limit`. */
  overflowLabel?: ReactNode;
  /** How values are written: `number` (default), `percent` (a ratio: 0.12 is 12%) or `currency`. */
  format?: 'number' | 'percent' | 'currency';
  /** ISO 4217 code for `format="currency"`. */
  currency?: string;
  /** Unit after a `number` value, e.g. `visits`. */
  unit?: string;
  /** BCP 47 locale. Default: the runtime locale. */
  locale?: string;
  /** `compact` writes 1.2K. Default `standard`. */
  notation?: 'standard' | 'compact';
  /** Extra `Intl.NumberFormat` options. */
  formatOptions?: Intl.NumberFormatOptions;
  /** Writes a value yourself; replaces `format`. */
  formatValue?: (value: number, item: BarListItem) => ReactNode;
  /** `stacked` (default): bar under the label. `inline`: label, bar and value in one row on wide screens. */
  layout?: 'stacked' | 'inline';
  /** Component used for `href` rows, e.g. a router link. Default `a`. */
  linkComponent?: ElementType;
  /** Spoken for a missing value. Default "No value". */
  missingLabel?: string;
  /** Shown when there are no items. Default "No data yet". */
  emptyState?: ReactNode;
  /** Shows placeholder rows and sets `aria-busy`. */
  loading?: boolean;
  /** Placeholder rows while loading. Default 5. */
  loadingRows?: number;
}

function sortItems(items: BarListItem[], sort: BarListProps['sort']) {
  const indexed = items.map((item, index) => ({ item, index }));
  if (sort === 'none') return indexed;
  const dir = sort === 'asc' ? 1 : -1;
  return indexed.sort((a, b) => {
    if (a.item.value === null || b.item.value === null) {
      if (a.item.value === b.item.value) return a.index - b.index;
      return a.item.value === null ? 1 : -1;
    }
    return (a.item.value - b.item.value) * dir || a.index - b.index;
  });
}

/**
 * A ranked list with an inline bar per row (top pages, top referrers,
 * rivals by score). Labels and values are real text; the bars are decorative.
 */
export const BarList = forwardRef<HTMLDivElement, BarListProps>(function BarList(
  {
    items,
    label,
    ranked = false,
    sort = 'desc',
    max,
    limit,
    overflowLabel,
    format = 'number',
    currency,
    unit,
    locale,
    notation = 'standard',
    formatOptions,
    formatValue,
    layout = 'stacked',
    linkComponent: LinkComponent = 'a',
    missingLabel = 'No value',
    emptyState = 'No data yet',
    loading = false,
    loadingRows = 5,
    className,
    ...rest
  },
  ref,
) {
  const List = ranked ? 'ol' : 'ul';
  const root = {
    ref,
    ...rest,
    className: cx('mn-bar-list', className),
    'data-layout': layout,
    'data-ranked': ranked || undefined,
  };

  if (loading) {
    return (
      <div {...root} aria-busy="true" data-loading="">
        <ul className="mn-bar-list__list" aria-hidden>
          {Array.from({ length: loadingRows }, (_, i) => (
            <li key={i} className="mn-bar-list__item">
              <span className="mn-bar-list__main">
                <Skeleton shape="text" width={`${60 - i * 6}%`} />
              </span>
              <span className="mn-bar-list__track">
                <Skeleton shape="text" width={`${90 - i * 14}%`} height="100%" />
              </span>
            </li>
          ))}
        </ul>
        <VisuallyHidden>{label ? `Loading ${label}` : 'Loading'}</VisuallyHidden>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div {...root} data-empty="">
        <p className="mn-bar-list__empty">{emptyState}</p>
      </div>
    );
  }

  const sorted = sortItems(items, sort);
  const shown = limit !== undefined && limit >= 0 ? sorted.slice(0, limit) : sorted;
  const values = items.map((i) => i.value).filter((v): v is number => v !== null);
  const scale = max ?? Math.max(0, ...values);

  const write = (value: number, item: BarListItem): ReactNode => {
    if (formatValue) return formatValue(value, item);
    if (format === 'currency') {
      return formatMoney(value, currency ?? '', {
        ...(locale !== undefined && { locale }),
        notation,
        ...(formatOptions?.maximumFractionDigits !== undefined && {
          maximumFractionDigits: formatOptions.maximumFractionDigits,
        }),
        ...(formatOptions?.minimumFractionDigits !== undefined && {
          minimumFractionDigits: formatOptions.minimumFractionDigits,
        }),
      });
    }
    const text = formatNumber(value, {
      kind: format,
      notation,
      ...(locale !== undefined && { locale }),
      ...(formatOptions !== undefined && { options: formatOptions }),
    });
    return unit && format === 'number' ? `${text}\u00a0${unit}` : text;
  };

  return (
    <div {...root}>
      {/* role="list": Safari drops list semantics when list-style is none. */}
      <List className="mn-bar-list__list" role="list" aria-label={label}>
        {shown.map(({ item, index }, position) => {
          const pct =
            item.value === null || !(scale > 0)
              ? 0
              : Math.min(100, Math.max(0, (item.value / scale) * 100));
          const labelNode = item.href ? (
            <LinkComponent className="mn-bar-list__link" href={item.href}>
              {item.label}
            </LinkComponent>
          ) : (
            item.label
          );
          return (
            <li
              key={item.id ?? index}
              className="mn-bar-list__item"
              data-highlighted={item.highlighted || undefined}
              data-linked={item.href ? '' : undefined}
            >
              {ranked ? (
                <span className="mn-bar-list__rank" aria-hidden>
                  {position + 1}
                </span>
              ) : null}
              <span className="mn-bar-list__main">
                <span className="mn-bar-list__label">
                  {item.icon ? (
                    <span className="mn-bar-list__icon" aria-hidden>
                      {item.icon}
                    </span>
                  ) : null}
                  <span className="mn-bar-list__text">{labelNode}</span>
                </span>
                {item.description ? (
                  <span className="mn-bar-list__description">{item.description}</span>
                ) : null}
              </span>
              <span className="mn-bar-list__value" data-missing={item.value === null || undefined}>
                {item.displayValue ??
                  (item.value === null ? (
                    <>
                      <span aria-hidden>–</span>
                      <VisuallyHidden>{missingLabel}</VisuallyHidden>
                    </>
                  ) : (
                    write(item.value, item)
                  ))}
              </span>
              <span className="mn-bar-list__track" aria-hidden>
                <span className="mn-bar-list__bar" style={{ width: `${pct}%` }} />
              </span>
            </li>
          );
        })}
      </List>
      {shown.length < sorted.length ? (
        <p className="mn-bar-list__footer">
          {overflowLabel ?? `Top ${shown.length} of ${sorted.length}`}
        </p>
      ) : null}
    </div>
  );
});
