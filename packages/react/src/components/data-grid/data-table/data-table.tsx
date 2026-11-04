'use client';

import {
  forwardRef,
  type HTMLAttributes,
  type TableHTMLAttributes,
  type TdHTMLAttributes,
  type ThHTMLAttributes,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import { Checkbox, type CheckboxProps } from '../../forms/checkbox/index.js';

export interface TableProps extends TableHTMLAttributes<HTMLTableElement> {
  /** Row height. `compact` for dense data, `comfortable` (default) for mixed content. */
  density?: 'compact' | 'comfortable';
  /** Keep the header visible while the body scrolls (the wrapper needs a height, see `maxHeight`). */
  stickyHeader?: boolean;
  /**
   * Keep the first column visible while the table scrolls sideways. Cells can
   * also opt in one by one with `data-pinned` (DataTable does this for its
   * selection and expand columns).
   */
  stickyFirstColumn?: boolean;
  /** Maximum height of the scrolling wrapper, e.g. `480` or `'60vh'`. Pair with `stickyHeader`. */
  maxHeight?: number | string;
}

/**
 * Data table primitives. Wrap in `<Card flush>` for a framed table. Pass a
 * `<TableCaption>` or `aria-label` so the table has an accessible name.
 */
export const Table = forwardRef<HTMLTableElement, TableProps>(function Table(
  {
    density = 'comfortable',
    stickyHeader = false,
    stickyFirstColumn = false,
    maxHeight,
    className,
    ...rest
  },
  ref,
) {
  return (
    <div className="mn-table-wrap" style={maxHeight === undefined ? undefined : { maxHeight }}>
      <table
        ref={ref}
        {...rest}
        data-density={density}
        data-sticky={stickyHeader || undefined}
        data-sticky-first={stickyFirstColumn || undefined}
        className={cx('mn-table', className)}
      />
    </div>
  );
});

export function TableHeader({ className, ...rest }: HTMLAttributes<HTMLTableSectionElement>) {
  return <thead {...rest} className={cx('mn-table__header', className)} />;
}

export function TableBody({ className, ...rest }: HTMLAttributes<HTMLTableSectionElement>) {
  return <tbody {...rest} className={cx('mn-table__body', className)} />;
}

export interface TableRowProps extends HTMLAttributes<HTMLTableRowElement> {
  /** Highlights the row (also sets aria-selected). */
  selected?: boolean;
  /** Pointer affordance for rows that open something on click. Keep a real link or button inside. */
  interactive?: boolean;
}

export function TableRow({ selected, interactive, className, ...rest }: TableRowProps) {
  return (
    <tr
      {...rest}
      aria-selected={selected ?? rest['aria-selected']}
      data-selected={selected || undefined}
      data-interactive={interactive || undefined}
      className={cx('mn-table__row', className)}
    />
  );
}

export type SortDirection = 'ascending' | 'descending' | 'none';

export interface TableHeadProps extends ThHTMLAttributes<HTMLTableCellElement> {
  /** Current sort of this column. Renders a SortButton and sets aria-sort. */
  sort?: SortDirection;
  /** Called when the sort button is pressed. Required when `sort` is set. */
  onSortChange?: (next: 'ascending' | 'descending') => void;
  /** Right-align numbers. */
  numeric?: boolean;
}

export function TableHead({
  sort,
  onSortChange,
  numeric,
  className,
  children,
  scope = 'col',
  ...rest
}: TableHeadProps) {
  const sortable = sort !== undefined;
  return (
    <th
      {...rest}
      scope={scope}
      aria-sort={sortable && sort !== 'none' ? sort : undefined}
      data-numeric={numeric || undefined}
      className={cx('mn-table__head', className)}
    >
      {sortable ? (
        <SortButton
          direction={sort}
          onClick={() => onSortChange?.(sort === 'ascending' ? 'descending' : 'ascending')}
        >
          {children}
        </SortButton>
      ) : (
        children
      )}
    </th>
  );
}

export interface SortButtonProps {
  direction: SortDirection;
  onClick: () => void;
  children: React.ReactNode;
}

/** Column sort toggle; its accessible name states the current direction. */
export function SortButton({ direction, onClick, children }: SortButtonProps) {
  const state =
    direction === 'ascending'
      ? 'sorted ascending'
      : direction === 'descending'
        ? 'sorted descending'
        : 'not sorted';
  return (
    <button type="button" className="mn-table__sort" data-direction={direction} onClick={onClick}>
      {children}
      <span className="mn-visually-hidden">, {state}</span>
      <ChevronDownGlyph className="mn-table__sort-icon" />
    </button>
  );
}

export interface TableCellProps extends TdHTMLAttributes<HTMLTableCellElement> {
  numeric?: boolean;
}

export function TableCell({ numeric, className, ...rest }: TableCellProps) {
  return (
    <td {...rest} data-numeric={numeric || undefined} className={cx('mn-table__cell', className)} />
  );
}

export function TableCaption({ className, ...rest }: HTMLAttributes<HTMLTableCaptionElement>) {
  return <caption {...rest} className={cx('mn-table__caption', className)} />;
}

export interface TableSelectCellProps extends Omit<CheckboxProps, 'label' | 'description'> {
  /** Accessible name, e.g. "Select ticket 42" or "Select all". */
  label: string;
  /** Render as a header cell (for the select-all checkbox). */
  header?: boolean;
}

/** Narrow checkbox column for row selection. */
export function TableSelectCell({
  label,
  header = false,
  className,
  ...rest
}: TableSelectCellProps) {
  const Tag = header ? 'th' : 'td';
  return (
    <Tag
      {...(header ? { scope: 'col' } : {})}
      className={cx('mn-table__select', header ? 'mn-table__head' : 'mn-table__cell', className)}
    >
      <Checkbox aria-label={label} {...rest} />
    </Tag>
  );
}
