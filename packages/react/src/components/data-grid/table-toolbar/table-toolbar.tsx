'use client';

import { forwardRef, type HTMLAttributes, type ReactNode, type RefObject } from 'react';
import { cx } from '../../../internal/cx.js';
import { SearchInput, type SearchInputProps } from '../../search/search-input/index.js';
import { BulkActionBar, type BulkAction } from '../bulk-action-bar/index.js';
import { ColumnMenu, type ColumnMenuProps } from '../column-menu/index.js';
import { ViewSwitcher, type ViewSwitcherProps } from '../view-switcher/index.js';

export interface TableToolbarMessages {
  /** Accessible name of the toolbar group. */
  label: string;
  /** Accessible name of the search field. */
  search: string;
  /** Placeholder of the search field. */
  searchPlaceholder: string;
  /** Result count next to the controls: "1,204 results". */
  results: (count: number) => string;
}

export const defaultTableToolbarMessages: TableToolbarMessages = {
  label: 'Table controls',
  search: 'Search',
  searchPlaceholder: 'Search…',
  results: (n) => `${n.toLocaleString()} ${n === 1 ? 'result' : 'results'}`,
};

/** The search field: SearchInput props (value, onValueChange, onSearch, debounce, pending…). */
export type TableToolbarSearch = Omit<SearchInputProps, 'className'>;

export interface TableToolbarProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Adds a search field. Pass SearchInput props such as `value`, `onValueChange` or `onSearch`. */
  search?: TableToolbarSearch | undefined;
  /** Filter controls after the search, e.g. a FilterBar or filter buttons. */
  filters?: ReactNode;
  /** Adds a ViewSwitcher (table, board, calendar, gallery…) with these props. */
  view?: ViewSwitcherProps<string> | undefined;
  /**
   * Adds a "Columns" ColumnMenu with these props; spread
   * `useDataTableColumns(columns).chooserProps` into it.
   */
  columns?: ColumnMenuProps | undefined;
  /** Trailing actions, e.g. an Export button or the primary "New" button. */
  actions?: ReactNode;
  /** Rows matching the search and filters: shown as "N results" and announced politely. */
  resultCount?: number | undefined;
  /**
   * Selected rows. While it is above zero, the search and filters give way to
   * a BulkActionBar with the count, `bulkActions` and "Clear selection".
   */
  selectedCount?: number | undefined;
  /** All rows that could be selected, for "3 of 120 selected". */
  totalCount?: number | undefined;
  /** Clears the selection. Required for the bulk selection area. */
  onClearSelection?: (() => void) | undefined;
  /** Actions on the selected rows (BulkActionBar actions). */
  bulkActions?: readonly BulkAction[] | undefined;
  /** Extra controls in the bulk selection area. */
  bulkChildren?: ReactNode;
  /** Where focus goes after "Clear selection", e.g. the grid. Default: the bulk area. */
  returnFocusRef?: RefObject<HTMLElement | null> | undefined;
  /** Smaller controls for dense screens. Default `sm`. */
  size?: 'sm' | 'md' | undefined;
  messages?: Partial<TableToolbarMessages> | undefined;
}

/**
 * The bar above a table or grid: search, a filters slot, a result count, a
 * view switcher, a column menu and trailing actions. While rows are selected
 * the search and filters give way to the bulk selection count and actions.
 * Composes SearchInput, ViewSwitcher, ColumnMenu and BulkActionBar.
 */
export const TableToolbar = forwardRef<HTMLDivElement, TableToolbarProps>(function TableToolbar(
  {
    search,
    filters,
    view,
    columns,
    actions,
    resultCount,
    selectedCount = 0,
    totalCount,
    onClearSelection,
    bulkActions,
    bulkChildren,
    returnFocusRef,
    size = 'sm',
    messages,
    className,
    'aria-label': ariaLabel,
    ...rest
  },
  ref,
) {
  const m = { ...defaultTableToolbarMessages, ...messages };
  const bulk = onClearSelection !== undefined;
  const selecting = bulk && selectedCount > 0;
  const hasStart = search !== undefined || filters !== undefined;
  const hasEnd =
    resultCount !== undefined || view !== undefined || columns !== undefined || actions;

  return (
    <div
      ref={ref}
      role="group"
      aria-label={rest['aria-labelledby'] ? undefined : (ariaLabel ?? m.label)}
      {...rest}
      className={cx('mn-table-toolbar', className)}
      data-selecting={selecting || undefined}
      data-size={size}
    >
      {hasStart ? (
        // Kept mounted while selecting so an uncontrolled search keeps its text.
        <div className="mn-table-toolbar__start" hidden={selecting}>
          {search ? (
            <SearchInput
              aria-label={m.search}
              placeholder={m.searchPlaceholder}
              size={size}
              {...search}
              className="mn-table-toolbar__search"
            />
          ) : null}
          {filters !== undefined ? (
            <div className="mn-table-toolbar__filters">{filters}</div>
          ) : null}
        </div>
      ) : null}
      {bulk ? (
        <BulkActionBar
          className="mn-table-toolbar__bulk"
          count={selectedCount}
          {...(totalCount !== undefined ? { totalCount } : {})}
          {...(bulkActions ? { actions: bulkActions } : {})}
          {...(returnFocusRef ? { returnFocusRef } : {})}
          onClear={onClearSelection}
        >
          {bulkChildren}
        </BulkActionBar>
      ) : null}
      {hasEnd ? (
        <div className="mn-table-toolbar__end">
          {resultCount !== undefined ? (
            <span className="mn-table-toolbar__count" role="status">
              {m.results(resultCount)}
            </span>
          ) : null}
          {view ? <ViewSwitcher size={size} {...view} /> : null}
          {columns ? <ColumnMenu size={size} {...columns} /> : null}
          {actions ? <div className="mn-table-toolbar__actions">{actions}</div> : null}
        </div>
      ) : null}
    </div>
  );
});
