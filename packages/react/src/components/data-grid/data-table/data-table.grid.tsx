'use client';

import {
  forwardRef,
  Fragment,
  useCallback,
  useEffect,
  useId,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type ForwardedRef,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type MouseEvent as ReactMouseEvent,
  type PointerEvent as ReactPointerEvent,
  type ReactElement,
  type ReactNode,
  type RefAttributes,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  ChevronDownGlyph,
  ChevronRightGlyph,
  ColumnsGlyph,
  MoreGlyph,
} from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';
import { Popover, PopoverContent, PopoverTrigger } from '../../overlays/popover/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { ErrorState } from '../../feedback/error-state/index.js';
import { LoadingState } from '../../feedback/loading-state/index.js';
import { Pagination } from '../../navigation/pagination/index.js';
import { Table, TableBody, TableCaption, TableHeader, TableRow } from './data-table.js';

// Leading utility columns (expand, select) are 2.5rem wide; the pinned first
// data column starts after them.
const UTILITY_WIDTH = 2.5;

/* ------------------------------------------------------------------ types */

export type DataTableSortDirection = 'ascending' | 'descending';

/** One sorted column. A sort is an ordered list: the first entry is the primary sort. */
export interface DataTableSort {
  /** Column id. */
  id: string;
  direction: DataTableSortDirection;
}

export interface DataTableColumn<T> {
  /** Stable id, used for sort, visibility, order and width state. */
  id: string;
  /** Header content. */
  header: ReactNode;
  /**
   * Plain-text name used by the column chooser, card labels, the resize
   * handle and sort announcements. Default: `header` when it is a string,
   * otherwise `id`.
   */
  label?: string;
  /** Cell content. Default: the `accessor` value (or `row[id]`) as text. */
  cell?: (row: T, index: number) => ReactNode;
  /** The value used for the default cell and for built-in sorting. Default `row[id]`. */
  accessor?: (row: T) => unknown;
  /** Header becomes a sort button. */
  sortable?: boolean;
  /** Custom comparator for built-in sorting (ascending order). */
  sortFn?: (a: T, b: T) => number;
  /** Right-align and use tabular figures. */
  numeric?: boolean;
  /** Width in px. Used as the starting width when the column is resizable. */
  width?: number;
  /** Minimum width in px when resizing. Default 64. */
  minWidth?: number;
  /** Maximum width in px when resizing. Default 640. */
  maxWidth?: number;
  /** Show a resize handle on the header (pointer drag and keyboard). */
  resizable?: boolean;
  /** Whether the column chooser can hide it. Default true. */
  hideable?: boolean;
  /** Hidden until the user shows it in the column chooser. */
  defaultHidden?: boolean;
  /**
   * Responsive priority: hide the column while the table is narrower than this
   * many px. Use it for secondary columns; the card layout still shows them.
   */
  hideBelow?: number;
  /** Render the cells as row headers (`th scope="row"`). Default: true for the first visible column. */
  rowHeader?: boolean;
  className?: string;
  headerClassName?: string;
}

export interface DataTableRowAction {
  id: string;
  label: string;
  icon?: ReactNode;
  onSelect: () => void;
  /** `danger` styling. Confirm destructive actions in the handler. */
  tone?: 'default' | 'danger';
  disabled?: boolean;
  /** Show as a visible button next to the overflow menu instead of inside it. */
  inline?: boolean;
}

export interface DataTableMessages {
  /** Header checkbox. */
  selectAll: string;
  /** Row checkbox, from the row label. */
  selectRow: (rowLabel: string) => string;
  /** Overflow menu button of a row. */
  rowActions: (rowLabel: string) => string;
  /** Accessible name of an inline row action button. */
  inlineAction: (actionLabel: string, rowLabel: string) => string;
  /** Visually hidden header of the actions column. */
  actionsHeader: string;
  /** Expand toggle of a row. */
  expandRow: (rowLabel: string) => string;
  /** Visually hidden header of the expand column. */
  expandHeader: string;
  /** Card layout "open" button. */
  openRow: (rowLabel: string) => string;
  /** Sort button state suffix, e.g. ", sorted ascending". */
  sortState: (direction: DataTableSortDirection | 'none', priority?: number) => string;
  /** Resize handle. */
  resizeColumn: (columnLabel: string) => string;
  loading: string;
  emptyTitle: string;
  emptyDescription: string;
  filteredTitle: string;
  filteredDescription: string;
  errorTitle: string;
  /** Selection banner when every row on the page is selected and more rows match. */
  pageSelected: (count: number) => string;
  selectAllMatching: (total: number) => string;
  allMatchingSelected: (total: number) => string;
  clearSelection: string;
  pagination: string;
}

export const defaultDataTableMessages: DataTableMessages = {
  selectAll: 'Select all rows on this page',
  selectRow: (row) => `Select ${row}`,
  rowActions: (row) => `Actions for ${row}`,
  inlineAction: (action, row) => `${action} ${row}`,
  actionsHeader: 'Actions',
  expandRow: (row) => `Details for ${row}`,
  expandHeader: 'Details',
  openRow: (row) => `Open ${row}`,
  sortState: (direction, priority) =>
    direction === 'none'
      ? ', not sorted'
      : `, sorted ${direction}${priority && priority > 1 ? `, sort ${priority}` : ''}`,
  resizeColumn: (column) => `Resize ${column} column`,
  loading: 'Loading rows',
  emptyTitle: 'Nothing here yet',
  emptyDescription: 'Rows appear here once they are added.',
  filteredTitle: 'No rows match these filters',
  filteredDescription: 'Try removing a filter or searching for something else.',
  errorTitle: 'These rows could not be loaded',
  pageSelected: (n) => `All ${n} rows on this page are selected.`,
  selectAllMatching: (total) => `Select all ${total}`,
  allMatchingSelected: (total) => `All ${total} rows are selected.`,
  clearSelection: 'Clear selection',
  pagination: 'Table pages',
};

export interface DataTableProps<T> extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  columns: readonly DataTableColumn<T>[];
  rows: readonly T[];
  /** Stable id of a row. */
  getRowId: (row: T, index: number) => string;
  /** Plain-text name of a row for checkbox, menu and expand labels ("Select Acme"). Default: the id. */
  getRowLabel?: (row: T) => string;
  /** Accessible name of the table. Rendered as a `<caption>`. */
  caption: string;
  /** Show the caption visually. Default false (it stays available to assistive technology). */
  showCaption?: boolean;
  density?: 'compact' | 'comfortable';
  /** Keep the header visible while the body scrolls. Set `maxHeight` too. */
  stickyHeader?: boolean;
  /** Keep the selection/expand columns and the first data column visible while scrolling sideways. */
  stickyFirstColumn?: boolean;
  /** Maximum height of the scroll area. */
  maxHeight?: number | string;

  /** Sort state (controlled). */
  sort?: readonly DataTableSort[];
  /** Initial sort (uncontrolled). */
  defaultSort?: readonly DataTableSort[];
  onSortChange?: (sort: DataTableSort[]) => void;
  /** Shift+click (or Shift+Enter) adds a column to the sort. Default false. */
  multiSort?: boolean;
  /** The rows arrive sorted (server-side sorting): the table only reports sort intent. */
  manualSorting?: boolean;

  /** Adds a checkbox column. */
  selectable?: boolean;
  /** Selected row ids (controlled). */
  selectedRowIds?: readonly string[];
  /** Initially selected row ids (uncontrolled). */
  defaultSelectedRowIds?: readonly string[];
  onSelectedRowIdsChange?: (ids: string[]) => void;
  /** Rows that cannot be selected get a disabled checkbox. */
  isRowSelectable?: (row: T) => boolean;
  /** Rows matching the current filters across all pages (server-driven). Enables "select all N". */
  totalRowCount?: number;
  /** Called by "Select all N" in the selection banner. The app records that every match is selected. */
  onSelectAllMatching?: () => void;
  /** Every matching row (not just this page) is selected; shows the banner state. */
  allMatchingSelected?: boolean;

  /** Visible columns by id (controlled). Missing ids fall back to `!defaultHidden`. */
  columnVisibility?: Readonly<Record<string, boolean>>;
  /** Initial visibility (uncontrolled; change it with `columnVisibility` and a DataTableColumnChooser). */
  defaultColumnVisibility?: Readonly<Record<string, boolean>>;
  /** Column ids in display order (controlled). Unknown ids are ignored, missing ones appended. */
  columnOrder?: readonly string[];
  defaultColumnOrder?: readonly string[];
  /** Column widths in px by id (controlled). */
  columnWidths?: Readonly<Record<string, number>>;
  defaultColumnWidths?: Readonly<Record<string, number>>;
  onColumnWidthsChange?: (widths: Record<string, number>) => void;

  /** Rows per page. Adds a Pagination footer. */
  pageSize?: number;
  /** 1-based page (controlled). */
  page?: number;
  defaultPage?: number;
  onPageChange?: (page: number) => void;
  /** The rows are already the current page (server-side pagination). Needs `totalRowCount`. */
  manualPagination?: boolean;

  /** Rows are loading. Without rows a skeleton fills the body; with rows they stay and the table is busy. */
  loading?: boolean;
  /** Loading failed. A string or node becomes the description. Without rows it replaces the body. */
  error?: ReactNode;
  onRetry?: () => void;
  /** The rows are empty because of filters: shows the filtered empty state. */
  filtered?: boolean;
  onClearFilters?: () => void;
  /** Replaces the built-in empty state. */
  emptyState?: ReactNode;

  /** Actions for a row: inline buttons and an overflow menu in a trailing column. */
  rowActions?: (row: T) => DataTableRowAction[];
  /** Opens a row: click anywhere that is not a control, or Enter on a focused cell. */
  onRowOpen?: (row: T) => void;
  /** Detail content of an expanded row. Adds a leading expand column. */
  renderExpanded?: (row: T) => ReactNode;
  isRowExpandable?: (row: T) => boolean;
  expandedRowIds?: readonly string[];
  defaultExpandedRowIds?: readonly string[];
  onExpandedRowIdsChange?: (ids: string[]) => void;

  /**
   * Keyboard grid navigation (WAI-ARIA grid): the table becomes `role="grid"`
   * with one tab stop and arrow-key movement between cells. Default: on when
   * `onRowOpen` is set.
   */
  keyboardNavigation?: boolean;
  /**
   * `table` scrolls sideways on narrow screens; `cards` always renders a
   * stacked list; `auto` switches to cards below `cardBreakpoint`.
   */
  layout?: 'table' | 'cards' | 'auto';
  /** Width in px below which `layout="auto"` shows cards. Default 640. */
  cardBreakpoint?: number;
  /** Content under the table, e.g. a LoadMore button. */
  footer?: ReactNode;
  messages?: Partial<DataTableMessages>;
}

/* ---------------------------------------------------------------- helpers */

const labelOf = <T,>(c: DataTableColumn<T>) =>
  c.label ?? (typeof c.header === 'string' ? c.header : c.id);

const valueOf = <T,>(c: DataTableColumn<T>, row: T): unknown =>
  c.accessor ? c.accessor(row) : (row as Record<string, unknown>)[c.id];

function compareValues(a: unknown, b: unknown): number {
  const empty = (v: unknown) => v === null || v === undefined || v === '';
  if (empty(a) || empty(b)) return empty(a) === empty(b) ? 0 : empty(a) ? 1 : -1;
  if (a instanceof Date && b instanceof Date) return a.getTime() - b.getTime();
  if (typeof a === 'number' && typeof b === 'number') return a - b;
  if (typeof a === 'boolean' && typeof b === 'boolean') return Number(a) - Number(b);
  return String(a).localeCompare(String(b), undefined, { numeric: true, sensitivity: 'base' });
}

/**
 * Sorts rows by an ordered sort list, the way DataTable does client-side.
 * Stable; empty values sort last in both directions.
 */
export function sortRows<T>(
  rows: readonly T[],
  sort: readonly DataTableSort[],
  columns: readonly DataTableColumn<T>[],
): T[] {
  const keys = sort
    .map((s) => ({ s, c: columns.find((c) => c.id === s.id) }))
    .filter((k): k is { s: DataTableSort; c: DataTableColumn<T> } => !!k.c);
  if (!keys.length) return rows.slice();
  return rows
    .map((row, i) => ({ row, i }))
    .sort((x, y) => {
      for (const { s, c } of keys) {
        const ax = valueOf(c, x.row);
        const ay = valueOf(c, y.row);
        const emptyX = ax === null || ax === undefined || ax === '';
        const emptyY = ay === null || ay === undefined || ay === '';
        if (!c.sortFn && (emptyX || emptyY)) {
          if (emptyX !== emptyY) return emptyX ? 1 : -1;
          continue;
        }
        const r = c.sortFn ? c.sortFn(x.row, y.row) : compareValues(ax, ay);
        if (r !== 0) return s.direction === 'ascending' ? r : -r;
      }
      return x.i - y.i;
    })
    .map((x) => x.row);
}

/** Next sort after pressing a header: ascending → descending → off. */
export function nextSort(
  current: readonly DataTableSort[],
  id: string,
  additive = false,
): DataTableSort[] {
  const existing = current.find((s) => s.id === id);
  const next: DataTableSort | null = !existing
    ? { id, direction: 'ascending' }
    : existing.direction === 'ascending'
      ? { id, direction: 'descending' }
      : null;
  if (!additive) return next ? [next] : [];
  const rest = current.filter((s) => s.id !== id);
  if (!next) return rest;
  return existing ? current.map((s) => (s.id === id ? next : s)) : [...rest, next];
}

function useControllable<V>(
  value: V | undefined,
  defaultValue: V,
  onChange?: (v: V) => void,
): [V, (v: V) => void] {
  const [inner, setInner] = useState(defaultValue);
  const controlled = value !== undefined;
  const current = controlled ? value : inner;
  const set = useCallback(
    (v: V) => {
      if (!controlled) setInner(v);
      onChange?.(v);
    },
    [controlled, onChange],
  );
  return [current, set];
}

/** Width of an element, tracked with ResizeObserver (undefined until measured or on the server). */
function useWidth(ref: { current: HTMLElement | null }, enabled: boolean): number | undefined {
  const [width, setWidth] = useState<number>();
  useLayoutEffectSafe(() => {
    const el = ref.current;
    if (!enabled || !el || typeof ResizeObserver === 'undefined') return;
    const ro = new ResizeObserver((entries) => {
      const w = entries[0]?.contentRect.width;
      if (w !== undefined) setWidth(Math.round(w));
    });
    ro.observe(el);
    setWidth(Math.round(el.getBoundingClientRect().width) || undefined);
    return () => ro.disconnect();
  }, [enabled]);
  return width;
}

const useLayoutEffectSafe = typeof window === 'undefined' ? useEffect : useLayoutEffect;

/* ----------------------------------------------------- grid keyboard model */

const FOCUSABLE =
  'a[href],button:not([disabled]),input:not([disabled]):not([type="hidden"]),select:not([disabled]),textarea:not([disabled]),[contenteditable="true"],[role="separator"][aria-valuenow]';
const TEXT_ENTRY =
  'input:not([type="checkbox"]):not([type="radio"]),textarea,select,[contenteditable="true"]';

function widgetsOf(cell: HTMLElement): HTMLElement[] {
  return Array.from(cell.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(
    (el) =>
      !el.closest('[aria-hidden="true"]') &&
      !el.hasAttribute('data-mn-grid-skip') &&
      el.closest('td,th') === cell,
  );
}

/** Where focus goes for a cell: its only widget, or the cell itself. */
function targetOf(cell: HTMLElement): HTMLElement {
  const w = widgetsOf(cell);
  const only = w.length === 1 ? w[0] : undefined;
  return only ?? cell;
}

function gridRows(table: HTMLTableElement): HTMLTableRowElement[] {
  return Array.from(table.rows).filter((r) => r.closest('table') === table);
}

/* -------------------------------------------------------------- component */

function DataTableInner<T>(props: DataTableProps<T>, ref: ForwardedRef<HTMLDivElement>) {
  const {
    columns,
    rows,
    getRowId,
    getRowLabel,
    caption,
    showCaption = false,
    density = 'comfortable',
    stickyHeader = false,
    stickyFirstColumn = false,
    maxHeight,
    sort: sortProp,
    defaultSort = [],
    onSortChange,
    multiSort = false,
    manualSorting = false,
    selectable = false,
    selectedRowIds,
    defaultSelectedRowIds = [],
    onSelectedRowIdsChange,
    isRowSelectable,
    totalRowCount,
    onSelectAllMatching,
    allMatchingSelected = false,
    columnVisibility: visibilityProp,
    defaultColumnVisibility = {},
    columnOrder,
    defaultColumnOrder,
    columnWidths: widthsProp,
    defaultColumnWidths = {},
    onColumnWidthsChange,
    pageSize,
    page: pageProp,
    defaultPage = 1,
    onPageChange,
    manualPagination = false,
    loading = false,
    error,
    onRetry,
    filtered = false,
    onClearFilters,
    emptyState,
    rowActions,
    onRowOpen,
    renderExpanded,
    isRowExpandable,
    expandedRowIds,
    defaultExpandedRowIds = [],
    onExpandedRowIdsChange,
    keyboardNavigation,
    layout = 'table',
    cardBreakpoint = 640,
    footer,
    messages: messagesProp,
    className,
    ...rest
  } = props;
  const m = useMemo(() => ({ ...defaultDataTableMessages, ...messagesProp }), [messagesProp]);
  const uid = useId();
  const rootRef = useRef<HTMLDivElement | null>(null);
  const tableRef = useRef<HTMLTableElement | null>(null);
  const setRoot = useCallback(
    (el: HTMLDivElement | null) => {
      rootRef.current = el;
      if (typeof ref === 'function') ref(el);
      else if (ref) ref.current = el;
    },
    [ref],
  );

  const needsWidth = layout === 'auto' || columns.some((c) => c.hideBelow !== undefined);
  const width = useWidth(rootRef, needsWidth);
  const cards =
    layout === 'cards' || (layout === 'auto' && width !== undefined && width < cardBreakpoint);

  const [sort, setSort] = useControllable<readonly DataTableSort[]>(
    sortProp,
    defaultSort,
    onSortChange as ((v: readonly DataTableSort[]) => void) | undefined,
  );
  const [selected, setSelected] = useControllable<readonly string[]>(
    selectedRowIds,
    defaultSelectedRowIds,
    onSelectedRowIdsChange as ((v: readonly string[]) => void) | undefined,
  );
  const [expanded, setExpanded] = useControllable<readonly string[]>(
    expandedRowIds,
    defaultExpandedRowIds,
    onExpandedRowIdsChange as ((v: readonly string[]) => void) | undefined,
  );
  const [widths, setWidths] = useControllable<Readonly<Record<string, number>>>(
    widthsProp,
    defaultColumnWidths,
    onColumnWidthsChange as ((v: Readonly<Record<string, number>>) => void) | undefined,
  );
  const [page, setPage] = useControllable<number>(pageProp, defaultPage, onPageChange);
  const visibility = visibilityProp ?? defaultColumnVisibility;
  const order = columnOrder ?? defaultColumnOrder;

  // Columns in display order, filtered by visibility and responsive priority.
  const visibleColumns = useMemo(() => {
    let ordered = columns.slice();
    if (order?.length) {
      const pos = new Map(order.map((id, i) => [id, i]));
      ordered = ordered
        .map((c, i) => ({ c, i }))
        .sort(
          (a, b) =>
            (pos.get(a.c.id) ?? order.length + a.i) - (pos.get(b.c.id) ?? order.length + b.i),
        )
        .map((x) => x.c);
    }
    return ordered.filter((c) => {
      const shown = c.hideable === false ? true : (visibility[c.id] ?? !c.defaultHidden);
      if (!shown) return false;
      if (!cards && c.hideBelow !== undefined && width !== undefined && width < c.hideBelow)
        return false;
      return true;
    });
  }, [columns, order, visibility, cards, width]);

  const sorted = useMemo(
    () => (manualSorting || !sort.length ? rows.slice() : sortRows(rows, sort, columns)),
    [rows, sort, columns, manualSorting],
  );
  const total = manualPagination ? (totalRowCount ?? rows.length) : sorted.length;
  const pageCount = pageSize ? Math.max(1, Math.ceil(total / pageSize)) : 1;
  const currentPage = Math.min(Math.max(1, page), pageCount);
  const pageRows = useMemo(
    () =>
      pageSize && !manualPagination
        ? sorted.slice((currentPage - 1) * pageSize, currentPage * pageSize)
        : sorted,
    [sorted, pageSize, manualPagination, currentPage],
  );

  const rowId = (row: T) => getRowId(row, rows.indexOf(row));
  const rowLabel = (row: T) => getRowLabel?.(row) ?? rowId(row);
  const selectedSet = useMemo(() => new Set(selected), [selected]);
  const expandedSet = useMemo(() => new Set(expanded), [expanded]);
  const canSelect = (row: T) => isRowSelectable?.(row) ?? true;
  const pageSelectableIds = pageRows.filter(canSelect).map(rowId);
  const pageSelectedCount = pageSelectableIds.filter((id) => selectedSet.has(id)).length;
  const allPageSelected =
    pageSelectableIds.length > 0 && pageSelectedCount === pageSelectableIds.length;
  const lastClicked = useRef<number | null>(null);
  const shiftRef = useRef(false);

  const toggleRow = (row: T, index: number, on: boolean) => {
    const id = rowId(row);
    const next = new Set(selected);
    if (shiftRef.current && lastClicked.current !== null) {
      const [a, b] = [lastClicked.current, index].sort((x, y) => x - y) as [number, number];
      for (const r of pageRows.slice(a, b + 1)) {
        if (!canSelect(r)) continue;
        if (on) next.add(rowId(r));
        else next.delete(rowId(r));
      }
    } else if (on) next.add(id);
    else next.delete(id);
    shiftRef.current = false;
    lastClicked.current = index;
    setSelected([...next]);
  };
  const togglePage = (on: boolean) => {
    const next = new Set(selected);
    for (const id of pageSelectableIds) {
      if (on) next.add(id);
      else next.delete(id);
    }
    setSelected([...next]);
  };
  const toggleExpanded = (id: string) => {
    const next = new Set(expanded);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    setExpanded([...next]);
  };

  const pressSort = (c: DataTableColumn<T>, additive: boolean) =>
    setSort(nextSort(sort, c.id, multiSort && additive));

  const expandable = !!renderExpanded;
  const hasActions = !!rowActions;
  const grid = (keyboardNavigation ?? !!onRowOpen) && !cards;
  const resizableAny = visibleColumns.some((c) => c.resizable);
  const widthOf = (c: DataTableColumn<T>) =>
    widths[c.id] ?? c.width ?? (resizableAny ? 160 : undefined);
  const leading = (expandable ? 1 : 0) + (selectable ? 1 : 0);
  const colCount = leading + visibleColumns.length + (hasActions ? 1 : 0);
  const rowById = new Map(pageRows.map((r) => [rowId(r), r] as const));

  /* ------------------------------------------------ grid keyboard support */
  const [active, setActive] = useState<{ r: number; c: number }>({ r: 0, c: 0 });
  const entered = useRef(false);

  useLayoutEffectSafe(() => {
    const table = tableRef.current;
    if (!table) return;
    const rowsEl = gridRows(table);
    if (!grid) return;
    const r = Math.min(active.r, rowsEl.length - 1);
    rowsEl.forEach((tr, ri) => {
      Array.from(tr.cells).forEach((cell, ci) => {
        const isActive = ri === r && ci === Math.min(active.c, tr.cells.length - 1);
        const target = targetOf(cell);
        cell.setAttribute('data-mn-cell', '');
        for (const w of widgetsOf(cell)) {
          w.tabIndex = -1;
          w.setAttribute('data-mn-rove', '');
        }
        if (target === cell) cell.tabIndex = isActive ? 0 : -1;
        else {
          cell.removeAttribute('tabindex');
          target.tabIndex = isActive ? 0 : -1;
        }
      });
    });
    return () => {
      table.querySelectorAll<HTMLElement>('[data-mn-rove]').forEach((el) => {
        el.removeAttribute('tabindex');
        el.removeAttribute('data-mn-rove');
      });
      table.querySelectorAll<HTMLElement>('[data-mn-cell]').forEach((el) => {
        el.removeAttribute('tabindex');
        el.removeAttribute('data-mn-cell');
      });
    };
  });

  const focusCell = (r: number, c: number) => {
    const table = tableRef.current;
    if (!table) return;
    const rowsEl = gridRows(table);
    const rr = Math.max(0, Math.min(r, rowsEl.length - 1));
    const tr = rowsEl[rr];
    if (!tr) return;
    const cc = Math.max(0, Math.min(c, tr.cells.length - 1));
    const cell = tr.cells[cc];
    if (!cell) return;
    entered.current = false;
    setActive({ r: rr, c: cc });
    const target = targetOf(cell);
    target.tabIndex = 0;
    target.focus();
  };

  const resizeBy = (c: DataTableColumn<T>, delta: number) => {
    const w = widthOf(c) ?? 160;
    const next = Math.min(c.maxWidth ?? 640, Math.max(c.minWidth ?? 64, w + delta));
    setWidths({ ...widths, [c.id]: next });
  };

  const onGridKeyDown = (e: ReactKeyboardEvent<HTMLTableElement>) => {
    const table = tableRef.current;
    const t = e.target as HTMLElement;
    const cell = t.closest('td,th') as HTMLTableCellElement | null;
    if (!grid || !table || !cell || cell.closest('table') !== table) return;
    const rowsEl = gridRows(table);
    const tr = cell.parentElement as HTMLTableRowElement;
    const r = rowsEl.indexOf(tr);
    const c = Array.from(tr.cells).indexOf(cell);
    const onCell = t === cell;
    const widgets = widgetsOf(cell);
    const insideMulti = !onCell && widgets.length > 1;

    if (insideMulti) {
      if (e.key === 'Escape') {
        e.preventDefault();
        cell.tabIndex = 0;
        cell.focus();
        entered.current = false;
      } else if (e.key === 'Tab') {
        const i = widgets.indexOf(t);
        if (i === -1) return;
        e.preventDefault();
        widgets[(i + (e.shiftKey ? -1 : 1) + widgets.length) % widgets.length]?.focus();
      }
      return;
    }
    if (!onCell && t.matches(TEXT_ENTRY)) return;
    if (!onCell && t.getAttribute('role') === 'separator') return;

    const columnOfCell = (): DataTableColumn<T> | undefined => {
      const id = cell.getAttribute('data-column-id');
      return visibleColumns.find((col) => col.id === id);
    };
    if (e.altKey && (e.key === 'ArrowLeft' || e.key === 'ArrowRight') && cell.tagName === 'TH') {
      const col = columnOfCell();
      if (col?.resizable) {
        e.preventDefault();
        const dir = getComputedStyle(table).direction === 'rtl' ? -1 : 1;
        resizeBy(col, (e.key === 'ArrowRight' ? 16 : -16) * dir * (e.shiftKey ? 4 : 1));
      }
      return;
    }
    const rtl = getComputedStyle(table).direction === 'rtl';
    const lastRow = rowsEl.length - 1;
    switch (e.key) {
      case 'ArrowRight':
        e.preventDefault();
        return focusCell(r, c + (rtl ? -1 : 1));
      case 'ArrowLeft':
        e.preventDefault();
        return focusCell(r, c + (rtl ? 1 : -1));
      case 'ArrowDown':
        e.preventDefault();
        return focusCell(r + 1, c);
      case 'ArrowUp':
        e.preventDefault();
        return focusCell(r - 1, c);
      case 'Home':
        e.preventDefault();
        return e.ctrlKey || e.metaKey ? focusCell(0, 0) : focusCell(r, 0);
      case 'End':
        e.preventDefault();
        return e.ctrlKey || e.metaKey
          ? focusCell(lastRow, Number.MAX_SAFE_INTEGER)
          : focusCell(r, Number.MAX_SAFE_INTEGER);
      case 'PageDown':
        e.preventDefault();
        return focusCell(r + 5, c);
      case 'PageUp':
        e.preventDefault();
        return focusCell(r - 5, c);
      case 'Enter':
      case 'F2':
        if (!onCell) return;
        if (widgets.length > 1) {
          e.preventDefault();
          entered.current = true;
          widgets[0]?.focus();
          return;
        }
        if (e.key === 'Enter' && onRowOpen) {
          const row = rowById.get(tr.getAttribute('data-row-id') ?? '');
          if (row !== undefined) {
            e.preventDefault();
            onRowOpen(row);
          }
        }
        return;
      case ' ': {
        if (!onCell || !selectable) return;
        const row = rowById.get(tr.getAttribute('data-row-id') ?? '');
        if (row === undefined || !canSelect(row)) return;
        e.preventDefault();
        toggleRow(row, pageRows.indexOf(row), !selectedSet.has(rowId(row)));
        return;
      }
    }
  };

  const onGridFocus = (e: React.FocusEvent<HTMLTableElement>) => {
    const table = tableRef.current;
    if (!grid || !table) return;
    const cell = (e.target as HTMLElement).closest('td,th') as HTMLTableCellElement | null;
    if (!cell || cell.closest('table') !== table) return;
    const tr = cell.parentElement as HTMLTableRowElement;
    const r = gridRows(table).indexOf(tr);
    const c = Array.from(tr.cells).indexOf(cell);
    if (r !== active.r || c !== active.c) setActive({ r, c });
  };

  /* ------------------------------------------------------------ rendering */
  const openFromClick = (e: ReactMouseEvent, row: T) => {
    if (!onRowOpen) return;
    const el = e.target as HTMLElement;
    if (el.closest('a,button,input,select,textarea,label,[role="menuitem"],[role="separator"]'))
      return;
    if (typeof window !== 'undefined' && window.getSelection?.()?.toString()) return;
    onRowOpen(row);
  };

  const pinStyle = (offsetRem: number): CSSProperties | undefined =>
    stickyFirstColumn ? { insetInlineStart: `${offsetRem}rem` } : undefined;

  const actionsFor = (row: T) => {
    const list = rowActions?.(row) ?? [];
    const inline = list.filter((a) => a.inline);
    const menu = list.filter((a) => !a.inline);
    const label = rowLabel(row);
    return (
      <div className="mn-data-table__actions">
        {inline.map((a) => (
          <Button
            key={a.id}
            size="xs"
            variant="ghost"
            leadingIcon={a.icon}
            disabled={a.disabled}
            data-
            {...(a.tone ? { tone: a.tone } : {})}
            aria-label={m.inlineAction(a.label, label)}
            onClick={a.onSelect}
          >
            {a.label}
          </Button>
        ))}
        {menu.length ? (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <IconButton size="xs" label={m.rowActions(label)} icon={<MoreGlyph />} />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              {menu.map((a) => (
                <DropdownMenuItem
                  key={a.id}
                  icon={a.icon}
                  {...(a.tone ? { tone: a.tone } : {})}
                  disabled={a.disabled}
                  onSelect={a.onSelect}
                >
                  {a.label}
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
        ) : null}
      </div>
    );
  };

  const expandButton = (row: T, id: string) => {
    if (isRowExpandable && !isRowExpandable(row)) return null;
    const open = expandedSet.has(id);
    return (
      <button
        type="button"
        className="mn-data-table__expand"
        aria-expanded={open}
        aria-controls={open ? `${uid}-detail-${id}` : undefined}
        aria-label={m.expandRow(rowLabel(row))}
        data-state={open ? 'open' : 'closed'}
        onClick={() => toggleExpanded(id)}
      >
        <ChevronRightGlyph />
      </button>
    );
  };

  const rowCheckbox = (row: T, index: number, id: string) => (
    <Checkbox
      aria-label={m.selectRow(rowLabel(row))}
      checked={selectedSet.has(id)}
      disabled={!canSelect(row)}
      onClick={(e) => {
        shiftRef.current = e.shiftKey;
      }}
      onCheckedChange={(on) => toggleRow(row, index, on === true)}
    />
  );

  const emptyBody = () => {
    if (error && !pageRows.length)
      return (
        <ErrorState
          size="compact"
          headingLevel="p"
          title={m.errorTitle}
          description={typeof error === 'boolean' ? undefined : error}
          {...(onRetry ? { onRetry } : {})}
        />
      );
    if (loading && !pageRows.length)
      return <LoadingState label={m.loading} variant="rows" rows={Math.min(pageSize ?? 5, 5)} />;
    if (!pageRows.length)
      return (
        emptyState ??
        (filtered ? (
          <EmptyState
            size="compact"
            headingLevel="p"
            kind="filtered"
            title={m.filteredTitle}
            description={m.filteredDescription}
            {...(onClearFilters ? { onClearFilters } : {})}
          />
        ) : (
          <EmptyState
            size="compact"
            headingLevel="p"
            title={m.emptyTitle}
            description={m.emptyDescription}
          />
        ))
      );
    return null;
  };
  const stateContent = emptyBody();

  const banner =
    selectable &&
    totalRowCount !== undefined &&
    (allMatchingSelected ||
      (allPageSelected && onSelectAllMatching && totalRowCount > pageSelectableIds.length)) ? (
      <div className="mn-data-table__banner">
        <span>
          {allMatchingSelected
            ? m.allMatchingSelected(totalRowCount)
            : m.pageSelected(pageSelectableIds.length)}
        </span>
        {allMatchingSelected ? (
          <Button size="xs" variant="ghost" onClick={() => setSelected([])}>
            {m.clearSelection}
          </Button>
        ) : (
          <Button size="xs" variant="ghost" onClick={onSelectAllMatching}>
            {m.selectAllMatching(totalRowCount)}
          </Button>
        )}
      </div>
    ) : null;

  const errorBanner =
    error && pageRows.length ? (
      <ErrorState
        className="mn-data-table__error"
        size="compact"
        layout="line"
        headingLevel="p"
        title={m.errorTitle}
        description={typeof error === 'boolean' ? undefined : error}
        {...(onRetry ? { onRetry } : {})}
      />
    ) : null;

  const headerCell = (c: DataTableColumn<T>, i: number) => {
    const s = sort.findIndex((x) => x.id === c.id);
    const dir = sort[s]?.direction ?? 'none';
    const w = widthOf(c);
    const pinned = stickyFirstColumn && i === 0;
    return (
      <th
        key={c.id}
        scope="col"
        data-column-id={c.id}
        data-numeric={c.numeric || undefined}
        data-pinned={pinned || undefined}
        data-pinned-last={pinned || undefined}
        style={pinStyle(leading * UTILITY_WIDTH)}
        aria-sort={c.sortable && s === 0 ? (dir === 'none' ? undefined : dir) : undefined}
        className={cx('mn-table__head', c.headerClassName)}
      >
        <div className="mn-data-table__head">
          {c.sortable ? (
            <button
              type="button"
              className="mn-table__sort"
              data-direction={dir}
              onClick={(e) => pressSort(c, e.shiftKey)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && e.shiftKey) {
                  e.preventDefault();
                  pressSort(c, true);
                }
              }}
            >
              {c.header}
              <span className="mn-visually-hidden">
                {m.sortState(dir, sort.length > 1 && s !== -1 ? s + 1 : undefined)}
              </span>
              <ChevronDownGlyph className="mn-table__sort-icon" />
              {sort.length > 1 && s !== -1 ? (
                <span className="mn-data-table__sort-priority" aria-hidden>
                  {s + 1}
                </span>
              ) : null}
            </button>
          ) : (
            c.header
          )}
        </div>
        {c.resizable ? (
          <ResizeHandle
            label={m.resizeColumn(labelOf(c))}
            value={w ?? 160}
            min={c.minWidth ?? 64}
            max={c.maxWidth ?? 640}
            focusable={!grid}
            onChange={(v) => setWidths({ ...widths, [c.id]: v })}
            onReset={() => {
              setWidths(Object.fromEntries(Object.entries(widths).filter(([k]) => k !== c.id)));
            }}
          />
        ) : null}
      </th>
    );
  };

  const renderTable = () => (
    <Table
      ref={tableRef}
      role={grid ? 'grid' : undefined}
      aria-rowcount={
        grid && manualPagination && totalRowCount !== undefined ? totalRowCount + 1 : undefined
      }
      aria-busy={loading || undefined}
      density={density}
      stickyHeader={stickyHeader}
      {...(maxHeight !== undefined ? { maxHeight } : {})}
      data-layout={resizableAny ? 'fixed' : undefined}
      style={
        resizableAny
          ? {
              width: `calc(${visibleColumns.reduce((n, c) => n + (widthOf(c) ?? 160), 0)}px + ${(leading + (hasActions ? 1 : 0)) * UTILITY_WIDTH}rem)`,
            }
          : undefined
      }
      onKeyDown={grid ? onGridKeyDown : undefined}
      onFocus={grid ? onGridFocus : undefined}
    >
      <TableCaption className={showCaption ? undefined : 'mn-visually-hidden'}>
        {caption}
      </TableCaption>
      {resizableAny || visibleColumns.some((c) => c.width) ? (
        <colgroup>
          {expandable ? <col className="mn-data-table__col-utility" /> : null}
          {selectable ? <col className="mn-data-table__col-utility" /> : null}
          {visibleColumns.map((c) => {
            const w = widthOf(c);
            return <col key={c.id} style={w ? { width: `${w}px` } : undefined} />;
          })}
          {hasActions ? <col /> : null}
        </colgroup>
      ) : null}
      <TableHeader>
        <TableRow>
          {expandable ? (
            <th
              scope="col"
              className="mn-table__head mn-data-table__utility"
              data-pinned={stickyFirstColumn || undefined}
              style={pinStyle(0)}
            >
              <span className="mn-visually-hidden">{m.expandHeader}</span>
            </th>
          ) : null}
          {selectable ? (
            <th
              scope="col"
              className="mn-table__head mn-table__select mn-data-table__utility"
              data-pinned={stickyFirstColumn || undefined}
              style={pinStyle(expandable ? UTILITY_WIDTH : 0)}
            >
              <Checkbox
                aria-label={m.selectAll}
                disabled={pageSelectableIds.length === 0}
                checked={allPageSelected ? true : pageSelectedCount > 0 ? 'indeterminate' : false}
                onCheckedChange={(on) => togglePage(on === true)}
              />
            </th>
          ) : null}
          {visibleColumns.map(headerCell)}
          {hasActions ? (
            <th scope="col" className="mn-table__head mn-data-table__actions-head">
              <span className="mn-visually-hidden">{m.actionsHeader}</span>
            </th>
          ) : null}
        </TableRow>
      </TableHeader>
      <TableBody>
        {stateContent !== null ? (
          <tr className="mn-data-table__state-row">
            <td colSpan={colCount} className="mn-table__cell mn-data-table__state">
              {stateContent}
            </td>
          </tr>
        ) : (
          pageRows.map((row, index) => {
            const id = rowId(row);
            const isSelected = selectedSet.has(id);
            const isOpen = expandable && expandedSet.has(id);
            return (
              <Fragment key={id}>
                <TableRow
                  data-row-id={id}
                  {...(selectable ? { selected: isSelected } : {})}
                  interactive={!!onRowOpen}
                  data-expanded={isOpen || undefined}
                  onClick={onRowOpen ? (e) => openFromClick(e, row) : undefined}
                >
                  {expandable ? (
                    <td
                      className="mn-table__cell mn-data-table__utility"
                      data-pinned={stickyFirstColumn || undefined}
                      style={pinStyle(0)}
                    >
                      {expandButton(row, id)}
                    </td>
                  ) : null}
                  {selectable ? (
                    <td
                      className="mn-table__cell mn-table__select mn-data-table__utility"
                      data-pinned={stickyFirstColumn || undefined}
                      style={pinStyle(expandable ? UTILITY_WIDTH : 0)}
                    >
                      {rowCheckbox(row, index, id)}
                    </td>
                  ) : null}
                  {visibleColumns.map((c, ci) => {
                    const content = c.cell ? c.cell(row, index) : formatValue(valueOf(c, row));
                    const header = c.rowHeader ?? ci === 0;
                    const pinned = stickyFirstColumn && ci === 0;
                    const shared = {
                      'data-column-id': c.id,
                      'data-numeric': c.numeric || undefined,
                      'data-pinned': pinned || undefined,
                      'data-pinned-last': pinned || undefined,
                      style: pinned ? pinStyle(leading * UTILITY_WIDTH) : undefined,
                      className: cx(
                        'mn-table__cell',
                        header && 'mn-data-table__row-head',
                        c.className,
                      ),
                    };
                    return header ? (
                      <th key={c.id} scope="row" {...shared}>
                        {content}
                      </th>
                    ) : (
                      <td key={c.id} role={grid ? 'gridcell' : undefined} {...shared}>
                        {content}
                      </td>
                    );
                  })}
                  {hasActions ? (
                    <td className="mn-table__cell mn-data-table__actions-cell">
                      {actionsFor(row)}
                    </td>
                  ) : null}
                </TableRow>
                {isOpen ? (
                  <tr className="mn-data-table__detail" id={`${uid}-detail-${id}`}>
                    <td colSpan={colCount} className="mn-table__cell">
                      {renderExpanded?.(row)}
                    </td>
                  </tr>
                ) : null}
              </Fragment>
            );
          })
        )}
      </TableBody>
    </Table>
  );

  const renderCards = () => {
    if (stateContent !== null) return <div className="mn-data-table__state">{stateContent}</div>;
    const [first, ...others] = visibleColumns;
    return (
      <ul className="mn-data-table__cards" aria-label={caption} aria-busy={loading || undefined}>
        {pageRows.map((row, index) => {
          const id = rowId(row);
          const isSelected = selectedSet.has(id);
          const isOpen = expandable && expandedSet.has(id);
          return (
            <li
              key={id}
              className="mn-data-table__card"
              data-selected={(selectable && isSelected) || undefined}
            >
              <div className="mn-data-table__card-head">
                {selectable ? rowCheckbox(row, index, id) : null}
                <div className="mn-data-table__card-title">
                  {first
                    ? first.cell
                      ? first.cell(row, index)
                      : formatValue(valueOf(first, row))
                    : null}
                </div>
                {hasActions ? actionsFor(row) : null}
                {onRowOpen ? (
                  <IconButton
                    size="xs"
                    label={m.openRow(rowLabel(row))}
                    icon={<ChevronRightGlyph />}
                    onClick={() => onRowOpen(row)}
                  />
                ) : null}
              </div>
              {others.length ? (
                <dl className="mn-data-table__card-fields">
                  {others.map((c) => (
                    <div
                      key={c.id}
                      className="mn-data-table__card-field"
                      data-numeric={c.numeric || undefined}
                    >
                      <dt>{labelOf(c)}</dt>
                      <dd>{c.cell ? c.cell(row, index) : formatValue(valueOf(c, row))}</dd>
                    </div>
                  ))}
                </dl>
              ) : null}
              {expandable && (!isRowExpandable || isRowExpandable(row)) ? (
                <>
                  <button
                    type="button"
                    className="mn-data-table__card-expand"
                    aria-expanded={isOpen}
                    aria-controls={isOpen ? `${uid}-detail-${id}` : undefined}
                    data-state={isOpen ? 'open' : 'closed'}
                    onClick={() => toggleExpanded(id)}
                  >
                    <ChevronRightGlyph aria-hidden />
                    {m.expandRow(rowLabel(row))}
                  </button>
                  {isOpen ? (
                    <div className="mn-data-table__card-detail" id={`${uid}-detail-${id}`}>
                      {renderExpanded?.(row)}
                    </div>
                  ) : null}
                </>
              ) : null}
            </li>
          );
        })}
      </ul>
    );
  };

  return (
    <div
      ref={setRoot}
      {...rest}
      className={cx('mn-data-table', className)}
      data-view={cards ? 'cards' : 'table'}
      data-busy={loading || undefined}
      data-sticky-first={stickyFirstColumn || undefined}
    >
      <span className="mn-visually-hidden" role="status">
        {loading ? m.loading : ''}
      </span>
      {banner}
      {errorBanner}
      {loading && pageRows.length ? <div className="mn-data-table__progress" aria-hidden /> : null}
      {cards ? (
        <>
          {showCaption ? <p className="mn-table__caption">{caption}</p> : null}
          {renderCards()}
        </>
      ) : (
        renderTable()
      )}
      {footer || (pageSize && pageCount > 1) ? (
        <div className="mn-data-table__footer">
          {footer}
          {pageSize && pageCount > 1 ? (
            <Pagination
              className="mn-data-table__pagination"
              page={currentPage}
              pageCount={pageCount}
              pageSize={pageSize}
              totalItems={total}
              variant={cards ? 'compact' : 'full'}
              label={m.pagination}
              onPageChange={setPage}
            />
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

function formatValue(v: unknown): ReactNode {
  if (v === null || v === undefined) return '';
  if (v instanceof Date) return v.toLocaleDateString();
  if (typeof v === 'object') return '';
  return String(v);
}

/**
 * A data table driven by column definitions: sorting (single or multi),
 * row selection with select-all-on-page, column visibility, order and
 * resizing, sticky header and first column, pagination (client or server),
 * expandable rows, row actions, built-in loading, empty and error states,
 * keyboard grid navigation and a stacked card layout for narrow screens.
 * Built on the Table parts.
 */
export const DataTable = forwardRef(DataTableInner) as <T>(
  props: DataTableProps<T> & RefAttributes<HTMLDivElement>,
) => ReactElement | null;

/* ----------------------------------------------------------- resize handle */

interface ResizeHandleProps {
  label: string;
  value: number;
  min: number;
  max: number;
  focusable: boolean;
  onChange: (value: number) => void;
  onReset: () => void;
}

function ResizeHandle({ label, value, min, max, focusable, onChange, onReset }: ResizeHandleProps) {
  const start = useRef<{ x: number; w: number; dir: number } | null>(null);
  const clamp = (v: number) => Math.round(Math.min(max, Math.max(min, v)));
  const onPointerDown = (e: ReactPointerEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    const el = e.currentTarget;
    el.setPointerCapture?.(e.pointerId);
    start.current = {
      x: e.clientX,
      w: value,
      dir: getComputedStyle(el).direction === 'rtl' ? -1 : 1,
    };
    el.setAttribute('data-dragging', '');
  };
  const onPointerMove = (e: ReactPointerEvent<HTMLDivElement>) => {
    const s = start.current;
    if (!s) return;
    onChange(clamp(s.w + (e.clientX - s.x) * s.dir));
  };
  const end = (e: ReactPointerEvent<HTMLDivElement>) => {
    start.current = null;
    e.currentTarget.removeAttribute('data-dragging');
  };
  return (
    // A focusable separator is the WAI-ARIA window-splitter widget (interactive).
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <div
      role="separator"
      aria-orientation="vertical"
      aria-label={label}
      aria-valuenow={value}
      aria-valuemin={min}
      aria-valuemax={max}
      // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex -- focusable splitter
      tabIndex={focusable ? 0 : undefined}
      data-mn-grid-skip=""
      className="mn-data-table__resize"
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={end}
      onPointerCancel={end}
      onClick={(e) => e.stopPropagation()}
      onDoubleClick={onReset}
      onKeyDown={(e) => {
        const step = e.shiftKey ? 64 : 16;
        const dir = getComputedStyle(e.currentTarget).direction === 'rtl' ? -1 : 1;
        const map: Record<string, number> = {
          ArrowRight: value + step * dir,
          ArrowLeft: value - step * dir,
          Home: min,
          End: max,
        };
        const next = map[e.key];
        if (next === undefined) return;
        e.preventDefault();
        e.stopPropagation();
        onChange(clamp(next));
      }}
    />
  );
}

/* ---------------------------------------------------------- column chooser */

export interface DataTableColumnChooserColumn {
  id: string;
  label: string;
  /** Cannot be hidden (still reorderable). Default true. */
  hideable?: boolean;
}

export interface DataTableColumnChooserMessages {
  trigger: string;
  title: string;
  moveUp: (column: string) => string;
  moveDown: (column: string) => string;
  reset: string;
}

export const defaultColumnChooserMessages: DataTableColumnChooserMessages = {
  trigger: 'Columns',
  title: 'Show columns',
  moveUp: (c) => `Move ${c} up`,
  moveDown: (c) => `Move ${c} down`,
  reset: 'Reset columns',
};

/** Column definitions → chooser entries (labels resolved the way DataTable does). */
export function columnChooserColumns<T>(
  columns: readonly DataTableColumn<T>[],
): DataTableColumnChooserColumn[] {
  return columns.map((c) => ({ id: c.id, label: labelOf(c), hideable: c.hideable !== false }));
}

export interface DataTableColumnChooserProps {
  columns: readonly DataTableColumnChooserColumn[];
  /** Visible columns by id; missing ids count as visible. */
  visibility: Readonly<Record<string, boolean>>;
  onVisibilityChange: (visibility: Record<string, boolean>) => void;
  /** Current order. With `onOrderChange`, each row gets move up/down buttons. */
  order?: readonly string[];
  onOrderChange?: (order: string[]) => void;
  /** Adds a "Reset columns" button. */
  onReset?: () => void;
  messages?: Partial<DataTableColumnChooserMessages>;
  /** Trigger size. Default `sm`. */
  size?: 'xs' | 'sm' | 'md';
  className?: string;
}

/**
 * Popover to show, hide and reorder DataTable columns. Reordering uses
 * buttons, so it works by keyboard and screen reader without drag and drop.
 */
export const DataTableColumnChooser = forwardRef<HTMLButtonElement, DataTableColumnChooserProps>(
  function DataTableColumnChooser(
    {
      columns,
      visibility,
      onVisibilityChange,
      order,
      onOrderChange,
      onReset,
      messages,
      size = 'sm',
      className,
    },
    ref,
  ) {
    const m = { ...defaultColumnChooserMessages, ...messages };
    const titleId = useId();
    const ordered = useMemo(() => {
      if (!order?.length) return columns.slice();
      const pos = new Map(order.map((id, i) => [id, i]));
      return columns
        .map((c, i) => ({ c, i }))
        .sort(
          (a, b) =>
            (pos.get(a.c.id) ?? order.length + a.i) - (pos.get(b.c.id) ?? order.length + b.i),
        )
        .map((x) => x.c);
    }, [columns, order]);
    const buttons = useRef(new Map<string, HTMLButtonElement | null>());
    const refocus = useRef<string | null>(null);
    useEffect(() => {
      if (!refocus.current) return;
      const el = buttons.current.get(refocus.current);
      refocus.current = null;
      if (el && !el.disabled) el.focus();
    });
    const move = (id: string, delta: -1 | 1) => {
      const ids = ordered.map((c) => c.id);
      const i = ids.indexOf(id);
      const j = i + delta;
      if (j < 0 || j >= ids.length) return;
      const a = ids[i];
      const b = ids[j];
      if (a === undefined || b === undefined) return;
      ids[i] = b;
      ids[j] = a;
      const key = `${id}:${delta}`;
      const atEdge = j === 0 || j === ids.length - 1;
      refocus.current = atEdge ? `${id}:${-delta}` : key;
      onOrderChange?.(ids);
    };
    return (
      <Popover>
        <PopoverTrigger asChild>
          <Button
            ref={ref}
            size={size}
            variant="secondary"
            className={className}
            leadingIcon={<ColumnsGlyph />}
          >
            {m.trigger}
          </Button>
        </PopoverTrigger>
        <PopoverContent
          align="end"
          aria-labelledby={`${titleId}-title`}
          className="mn-data-table__chooser-panel"
        >
          <p className="mn-data-table__chooser-title" id={`${titleId}-title`}>
            {m.title}
          </p>
          <ul className="mn-data-table__chooser">
            {ordered.map((c, i) => (
              <li key={c.id} className="mn-data-table__chooser-item">
                <Checkbox
                  label={c.label}
                  checked={visibility[c.id] ?? true}
                  disabled={c.hideable === false}
                  onCheckedChange={(on) =>
                    onVisibilityChange({ ...visibility, [c.id]: on === true })
                  }
                />
                {onOrderChange ? (
                  <span className="mn-data-table__chooser-move">
                    <IconButton
                      ref={(el) => {
                        buttons.current.set(`${c.id}:-1`, el);
                      }}
                      size="xs"
                      label={m.moveUp(c.label)}
                      disabled={i === 0}
                      icon={<ChevronDownGlyph style={{ transform: 'rotate(180deg)' }} />}
                      onClick={() => move(c.id, -1)}
                    />
                    <IconButton
                      ref={(el) => {
                        buttons.current.set(`${c.id}:1`, el);
                      }}
                      size="xs"
                      label={m.moveDown(c.label)}
                      disabled={i === ordered.length - 1}
                      icon={<ChevronDownGlyph />}
                      onClick={() => move(c.id, 1)}
                    />
                  </span>
                ) : null}
              </li>
            ))}
          </ul>
          {onReset ? (
            <Button
              size="xs"
              variant="ghost"
              className="mn-data-table__chooser-reset"
              onClick={onReset}
            >
              {m.reset}
            </Button>
          ) : null}
        </PopoverContent>
      </Popover>
    );
  },
);

/* ------------------------------------------------------ column state hook */

export interface UseDataTableColumnsResult {
  visibility: Record<string, boolean>;
  order: string[];
  setVisibility: (v: Record<string, boolean>) => void;
  setOrder: (o: string[]) => void;
  reset: () => void;
  /** Spread onto DataTable. */
  tableProps: {
    columnVisibility: Record<string, boolean>;
    columnOrder: string[];
  };
  /** Spread onto DataTableColumnChooser. */
  chooserProps: Pick<
    DataTableColumnChooserProps,
    'columns' | 'visibility' | 'onVisibilityChange' | 'order' | 'onOrderChange' | 'onReset'
  >;
}

/**
 * Column visibility and order state shared by a DataTable and its
 * DataTableColumnChooser. Persist `visibility`/`order` yourself if the
 * choice should survive a reload (saved views, local storage).
 */
export function useDataTableColumns<T>(
  columns: readonly DataTableColumn<T>[],
  initial: { visibility?: Record<string, boolean>; order?: string[] } = {},
): UseDataTableColumnsResult {
  const defaults = useMemo(() => {
    const v: Record<string, boolean> = {};
    for (const c of columns) v[c.id] = c.hideable === false ? true : !c.defaultHidden;
    return v;
  }, [columns]);
  const [visibility, setVisibility] = useState<Record<string, boolean>>(() => ({
    ...defaults,
    ...initial.visibility,
  }));
  const [order, setOrder] = useState<string[]>(() => initial.order ?? columns.map((c) => c.id));
  const reset = useCallback(() => {
    setVisibility(defaults);
    setOrder(columns.map((c) => c.id));
  }, [defaults, columns]);
  const chooserColumns = useMemo(() => columnChooserColumns(columns), [columns]);
  return {
    visibility,
    order,
    setVisibility,
    setOrder,
    reset,
    tableProps: { columnVisibility: visibility, columnOrder: order },
    chooserProps: {
      columns: chooserColumns,
      visibility,
      onVisibilityChange: setVisibility,
      order,
      onOrderChange: setOrder,
      onReset: reset,
    },
  };
}
