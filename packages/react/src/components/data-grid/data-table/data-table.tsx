'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
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
  /**
   * Name of the scrolling wrapper. While the table overflows its wrapper, the
   * wrapper becomes a focusable region so keyboard users can scroll it (WCAG
   * 2.1.1). It is named by this, else by the table's own name (its caption,
   * `aria-label` or `aria-labelledby`).
   */
  scrollLabel?: string | undefined;
}

/** True while the element's content overflows it (re-measured on resize). */
function useOverflow(ref: { readonly current: HTMLElement | null }): boolean {
  const [overflows, setOverflows] = useState(false);
  useEffect(() => {
    const el = ref.current;
    if (!el || typeof ResizeObserver === 'undefined') return;
    const measure = () =>
      setOverflows(el.scrollWidth > el.clientWidth + 1 || el.scrollHeight > el.clientHeight + 1);
    measure();
    const ro = new ResizeObserver(measure);
    ro.observe(el);
    if (el.firstElementChild) ro.observe(el.firstElementChild);
    return () => ro.disconnect();
  }, [ref]);
  return overflows;
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
    scrollLabel,
    className,
    ...rest
  },
  ref,
) {
  const wrapRef = useRef<HTMLDivElement>(null);
  const scrolls = useOverflow(wrapRef);
  const autoId = useId();
  const tableId = rest.id ?? autoId;
  return (
    <div
      ref={wrapRef}
      className="mn-table-wrap"
      style={maxHeight === undefined ? undefined : { maxHeight }}
      {...(scrolls
        ? {
            role: 'region',
            tabIndex: 0,
            ...(scrollLabel ? { 'aria-label': scrollLabel } : { 'aria-labelledby': tableId }),
          }
        : {})}
      data-scrolls={scrolls || undefined}
    >
      <table
        ref={ref}
        {...rest}
        id={tableId}
        data-density={density}
        data-sticky={stickyHeader || undefined}
        data-sticky-first={stickyFirstColumn || undefined}
        className={cx('mn-table', className)}
      />
    </div>
  );
});

export const TableHeader = forwardRef<
  HTMLTableSectionElement,
  HTMLAttributes<HTMLTableSectionElement>
>(function TableHeader({ className, ...rest }, ref) {
  return <thead ref={ref} {...rest} className={cx('mn-table__header', className)} />;
});
TableHeader.displayName = 'TableHeader';

export const TableBody = forwardRef<
  HTMLTableSectionElement,
  HTMLAttributes<HTMLTableSectionElement>
>(function TableBody({ className, ...rest }, ref) {
  return <tbody ref={ref} {...rest} className={cx('mn-table__body', className)} />;
});
TableBody.displayName = 'TableBody';

export interface TableRowProps extends HTMLAttributes<HTMLTableRowElement> {
  /** Highlights the row (also sets aria-selected). */
  selected?: boolean;
  /** Pointer affordance for rows that open something on click. Keep a real link or button inside. */
  interactive?: boolean;
}

export const TableRow = forwardRef<HTMLTableRowElement, TableRowProps>(function TableRow(
  { selected, interactive, className, ...rest },
  ref,
) {
  return (
    <tr
      ref={ref}
      {...rest}
      aria-selected={selected ?? rest['aria-selected']}
      data-selected={selected || undefined}
      data-interactive={interactive || undefined}
      className={cx('mn-table__row', className)}
    />
  );
});
TableRow.displayName = 'TableRow';

export type SortDirection = 'ascending' | 'descending' | 'none';

export interface TableHeadProps extends ThHTMLAttributes<HTMLTableCellElement> {
  /** Current sort of this column. Renders a SortButton and sets aria-sort. */
  sort?: SortDirection;
  /** Called when the sort button is pressed. Required when `sort` is set. */
  onSortChange?: (next: 'ascending' | 'descending') => void;
  /** Right-align numbers. */
  numeric?: boolean;
  /** Translated sort states read after the header text. Defaults are English ("sorted ascending"…). */
  sortStateLabels?: Partial<Record<SortDirection, string>> | undefined;
}

export const TableHead = forwardRef<HTMLTableCellElement, TableHeadProps>(function TableHead(
  { sort, onSortChange, numeric, sortStateLabels, className, children, scope = 'col', ...rest },
  ref,
) {
  const sortable = sort !== undefined;
  return (
    <th
      ref={ref}
      {...rest}
      scope={scope}
      aria-sort={sortable && sort !== 'none' ? sort : undefined}
      data-numeric={numeric || undefined}
      className={cx('mn-table__head', className)}
    >
      {sortable ? (
        <SortButton
          direction={sort}
          stateLabels={sortStateLabels}
          onClick={() => onSortChange?.(sort === 'ascending' ? 'descending' : 'ascending')}
        >
          {children}
        </SortButton>
      ) : (
        children
      )}
    </th>
  );
});
TableHead.displayName = 'TableHead';

export interface SortButtonProps {
  direction: SortDirection;
  onClick: () => void;
  children: React.ReactNode;
  /** Translated sort states read after the label. Defaults: "sorted ascending", "sorted descending", "not sorted". */
  stateLabels?: Partial<Record<SortDirection, string>> | undefined;
}

const DEFAULT_SORT_STATE: Record<SortDirection, string> = {
  ascending: 'sorted ascending',
  descending: 'sorted descending',
  none: 'not sorted',
};

/** Column sort toggle; its accessible name states the current direction. */
export const SortButton = forwardRef<HTMLButtonElement, SortButtonProps>(function SortButton(
  { direction, onClick, children, stateLabels },
  ref,
) {
  const state = stateLabels?.[direction] ?? DEFAULT_SORT_STATE[direction];
  return (
    <button
      ref={ref}
      type="button"
      className="mn-table__sort"
      data-direction={direction}
      onClick={onClick}
    >
      {children}
      <span className="mn-visually-hidden">, {state}</span>
      <ChevronDownGlyph className="mn-table__sort-icon" />
    </button>
  );
});
SortButton.displayName = 'SortButton';

export interface TableCellProps extends TdHTMLAttributes<HTMLTableCellElement> {
  numeric?: boolean;
}

export const TableCell = forwardRef<HTMLTableCellElement, TableCellProps>(function TableCell(
  { numeric, className, ...rest },
  ref,
) {
  return (
    <td
      ref={ref}
      {...rest}
      data-numeric={numeric || undefined}
      className={cx('mn-table__cell', className)}
    />
  );
});
TableCell.displayName = 'TableCell';

export const TableCaption = forwardRef<
  HTMLTableCaptionElement,
  HTMLAttributes<HTMLTableCaptionElement>
>(function TableCaption({ className, ...rest }, ref) {
  return <caption ref={ref} {...rest} className={cx('mn-table__caption', className)} />;
});
TableCaption.displayName = 'TableCaption';

export interface TableSelectCellProps extends Omit<CheckboxProps, 'label' | 'description'> {
  /** Accessible name, e.g. "Select ticket 42" or "Select all". */
  label: string;
  /** Render as a header cell (for the select-all checkbox). */
  header?: boolean;
}

/** Narrow checkbox column for row selection. */
export const TableSelectCell = forwardRef<HTMLTableCellElement, TableSelectCellProps>(
  function TableSelectCell({ label, header = false, className, ...rest }, ref) {
    const Tag = header ? 'th' : 'td';
    return (
      <Tag
        ref={ref}
        {...(header ? { scope: 'col' } : {})}
        className={cx('mn-table__select', header ? 'mn-table__head' : 'mn-table__cell', className)}
      >
        <Checkbox aria-label={label} {...rest} />
      </Tag>
    );
  },
);
TableSelectCell.displayName = 'TableSelectCell';
