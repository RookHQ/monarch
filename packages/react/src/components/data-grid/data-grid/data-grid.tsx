'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type FocusEvent as ReactFocusEvent,
  type ForwardedRef,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type MouseEvent as ReactMouseEvent,
  type PointerEvent as ReactPointerEvent,
  type ReactElement,
  type ReactNode,
  type RefAttributes,
} from 'react';
import { useIsomorphicLayoutEffect } from '../../../internal/layout-effect.js';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph, MoreGlyph } from '../../../internal/icons.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { LoadingState } from '../../feedback/loading-state/index.js';
import {
  nextSort,
  sortRows,
  type DataTableColumn,
  type DataTableSort,
} from '../data-table/index.js';
import { orderColumns } from '../column-menu/index.js';
import {
  EditableCell,
  type EditableCellOption,
  type EditableCellType,
  type EditableCellValue,
} from '../editable-cell/index.js';
import { RowActionsMenu, type RowAction } from '../row-actions-menu/index.js';
import { useVirtualizer } from '../virtual-list/index.js';
import { useDirection } from '../../../internal/direction.js';
import {
  dateTimeFormat,
  useClientLocale,
  useClientZone,
} from '../../../internal/dates/client-time.js';

const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useIsomorphicLayoutEffect;

/* ------------------------------------------------------------------ types */

/** Where a column is pinned: kept visible at the start or end while the grid scrolls sideways. */
export type DataGridPin = 'start' | 'end';

/**
 * A DataGrid column: DataTable's column model plus pinning and editing.
 * Columns are resizable unless `resizable: false`.
 */
export interface DataGridColumn<T> extends DataTableColumn<T> {
  /** Pin the column to the start or end edge. The user can change it from the column menu. */
  pinned?: DataGridPin | undefined;
  /** Allow pinning from the column menu. Default true. */
  pinnable?: boolean | undefined;
  /** Cells can be edited in place (Enter, F2, typing or double-click). Needs `onCellEdit`. */
  editable?: boolean | ((row: T) => boolean) | undefined;
  /** Editor of an editable column. Default `number` for numeric columns, otherwise `text`. */
  editor?: EditableCellType | undefined;
  /** Choices of a `select` editor. */
  options?: readonly EditableCellOption[] | undefined;
  /** An empty value is not allowed. */
  required?: boolean | undefined;
  /** Bounds of `number` and `date` editors. */
  min?: number | string | undefined;
  max?: number | string | undefined;
  /** Checks an edited value; return a message to block the save. */
  validate?: ((value: EditableCellValue, row: T) => ReactNode | null | undefined) | undefined;
}

export interface DataGridMessages {
  /** Header checkbox. */
  selectAll: string;
  /** Row checkbox. */
  selectRow: (rowLabel: string) => string;
  /** Visually hidden header of the actions column. */
  actionsHeader: string;
  /** Row actions trigger. */
  rowActions: (rowLabel: string) => string;
  /** Column menu button. */
  columnMenu: (columnLabel: string) => string;
  sortAscending: string;
  sortDescending: string;
  clearSort: string;
  pinStart: string;
  pinEnd: string;
  unpin: string;
  hideColumn: string;
  /** Editor name: "Status for Acme". */
  editLabel: (columnLabel: string, rowLabel: string) => string;
  loading: string;
  emptyTitle: string;
  emptyDescription: string;
}

export const defaultDataGridMessages: DataGridMessages = {
  selectAll: 'Select all rows',
  selectRow: (row) => `Select ${row}`,
  actionsHeader: 'Actions',
  rowActions: (row) => `Actions for ${row}`,
  columnMenu: (column) => `${column} column options`,
  sortAscending: 'Sort ascending',
  sortDescending: 'Sort descending',
  clearSort: 'Clear sort',
  pinStart: 'Pin to start',
  pinEnd: 'Pin to end',
  unpin: 'Unpin',
  hideColumn: 'Hide column',
  editLabel: (column, row) => `${column} for ${row}`,
  loading: 'Loading rows',
  emptyTitle: 'Nothing here yet',
  emptyDescription: 'Rows appear here once they are added.',
};

export interface DataGridProps<T> extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  columns: readonly DataGridColumn<T>[];
  rows: readonly T[];
  /** Stable id of a row. */
  getRowId: (row: T, index: number) => string;
  /** Plain-text name of a row, for checkbox, menu and editor names. Default: the id. */
  getRowLabel?: ((row: T) => string) | undefined;
  /** Accessible name of the grid. Required unless `aria-labelledby` is set. */
  label?: string | undefined;
  density?: 'compact' | 'comfortable' | undefined;
  /** Height of the scroll area. Default 480. The grid needs a bounded height to virtualize. */
  height?: number | string | undefined;
  /** Estimated row height in px before rows are measured. Default 36 (compact) or 44. */
  estimatedRowHeight?: number | undefined;
  /** Rows matching the view when only some are loaded (infinite loading). Sets `aria-rowcount`. */
  totalRowCount?: number | undefined;

  /** Sort state (controlled). */
  sort?: readonly DataTableSort[] | undefined;
  defaultSort?: readonly DataTableSort[] | undefined;
  onSortChange?: ((sort: DataTableSort[]) => void) | undefined;
  /** Shift+Enter or Shift+click adds a column to the sort. Default false. */
  multiSort?: boolean | undefined;
  /** The rows arrive sorted (server-side sorting): the grid only reports sort intent. */
  manualSorting?: boolean | undefined;

  /** Adds a checkbox column; Space toggles the focused row, Ctrl/⌘+A selects all. */
  selectable?: boolean | undefined;
  selectedRowIds?: readonly string[] | undefined;
  defaultSelectedRowIds?: readonly string[] | undefined;
  onSelectedRowIdsChange?: ((ids: string[]) => void) | undefined;
  isRowSelectable?: ((row: T) => boolean) | undefined;

  /** Visible columns by id (controlled). Missing ids fall back to `!defaultHidden`. */
  columnVisibility?: Readonly<Record<string, boolean>> | undefined;
  defaultColumnVisibility?: Readonly<Record<string, boolean>> | undefined;
  /** Called by "Hide column" in a column menu. */
  onColumnVisibilityChange?: ((visibility: Record<string, boolean>) => void) | undefined;
  /** Column ids in display order. */
  columnOrder?: readonly string[] | undefined;
  defaultColumnOrder?: readonly string[] | undefined;
  /** Column widths in px by id (controlled). */
  columnWidths?: Readonly<Record<string, number>> | undefined;
  defaultColumnWidths?: Readonly<Record<string, number>> | undefined;
  onColumnWidthsChange?: ((widths: Record<string, number>) => void) | undefined;
  /** Pinned columns by id (controlled). `false` unpins a column pinned in its definition. */
  columnPinning?: Readonly<Record<string, DataGridPin | false>> | undefined;
  defaultColumnPinning?: Readonly<Record<string, DataGridPin | false>> | undefined;
  onColumnPinningChange?: ((pinning: Record<string, DataGridPin | false>) => void) | undefined;
  /** Header menus with sort, pin and hide. Default true. */
  columnMenu?: boolean | undefined;

  /** Saves an edited cell. Return a promise for the pending state; reject to keep the editor open with the error. */
  onCellEdit?:
    ((row: T, columnId: string, value: EditableCellValue) => void | Promise<void>) | undefined;
  /** Actions in a "⋯" menu in a trailing pinned column. */
  rowActions?: ((row: T) => RowAction[]) | undefined;
  /** Opens a row: Enter or double-click on a cell that is not editable. */
  onRowOpen?: ((row: T) => void) | undefined;
  /** Called when the last rows come into view, to load more. */
  onEndReached?: (() => void) | undefined;

  /** Rows are loading. Without rows a skeleton shows; with rows the grid is busy. */
  loading?: boolean | undefined;
  /** Replaces the built-in empty state. */
  emptyState?: ReactNode;
  messages?: Partial<DataGridMessages> | undefined;
}

/* ---------------------------------------------------------------- helpers */

const SELECT_WIDTH = 44;
const ACTIONS_WIDTH = 52;
const DEFAULT_WIDTH = 160;

const labelOf = <T,>(c: DataTableColumn<T>) =>
  c.label ?? (typeof c.header === 'string' ? c.header : c.id);

const valueOf = <T,>(c: DataTableColumn<T>, row: T): unknown =>
  c.accessor ? c.accessor(row) : (row as Record<string, unknown>)[c.id];

/** Default cell text. Dates use the given locale and zone, so server and client HTML agree. */
function formatValue(v: unknown, locale: string, timeZone: string): ReactNode {
  if (v === null || v === undefined) return '';
  if (v instanceof Date) {
    return Number.isNaN(v.getTime()) ? '' : dateTimeFormat(locale, { timeZone }).format(v);
  }
  if (typeof v === 'object') return '';
  return String(v);
}

function editValue(v: unknown): EditableCellValue {
  if (v === null || v === undefined) return null;
  if (v instanceof Date) return Number.isNaN(v.getTime()) ? null : v.toISOString().slice(0, 10);
  if (typeof v === 'number' || typeof v === 'string') return v;
  if (typeof v === 'boolean') return String(v);
  return null;
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

const FOCUSABLE =
  'a[href],button:not([disabled]),input:not([disabled]):not([type="hidden"]),select:not([disabled]),textarea:not([disabled]),[contenteditable="true"]';

function widgetsOf(cell: HTMLElement): HTMLElement[] {
  return Array.from(cell.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(
    (el) =>
      !el.closest('[aria-hidden="true"]') &&
      !el.hasAttribute('data-mn-grid-skip') &&
      el.closest('[data-mn-cell]') === cell,
  );
}

/** Where focus goes for a cell: its only widget, or the cell itself. */
function targetOf(cell: HTMLElement): HTMLElement {
  const w = widgetsOf(cell);
  return w.length === 1 && w[0] ? w[0] : cell;
}

/** A column as laid out: data column or a utility column. */
type Slot<T> =
  | { kind: 'select'; key: string; width: number; pin: 'start' }
  | { kind: 'actions'; key: string; width: number; pin: 'end' }
  | {
      kind: 'data';
      key: string;
      width: number;
      pin: DataGridPin | null;
      column: DataGridColumn<T>;
    };

/* -------------------------------------------------------------- component */

function DataGridInner<T>(props: DataGridProps<T>, ref: ForwardedRef<HTMLDivElement>) {
  const readDir = useDirection();
  const {
    columns,
    rows,
    getRowId,
    getRowLabel,
    label,
    density = 'comfortable',
    height = 480,
    estimatedRowHeight,
    totalRowCount,
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
    columnVisibility: visibilityProp,
    defaultColumnVisibility = {},
    onColumnVisibilityChange,
    columnOrder,
    defaultColumnOrder,
    columnWidths: widthsProp,
    defaultColumnWidths = {},
    onColumnWidthsChange,
    columnPinning: pinningProp,
    defaultColumnPinning = {},
    onColumnPinningChange,
    columnMenu = true,
    onCellEdit,
    rowActions,
    onRowOpen,
    onEndReached,
    loading = false,
    emptyState,
    messages: messagesProp,
    className,
    style,
    'aria-labelledby': labelledBy,
    ...rest
  } = props;
  const m = useMemo(() => ({ ...defaultDataGridMessages, ...messagesProp }), [messagesProp]);
  const uid = useId();
  // Hydration-safe: the server and the hydrating client format dates alike.
  const dateLocale = useClientLocale();
  const dateZone = useClientZone();
  const gridRef = useRef<HTMLDivElement | null>(null);
  const scrollerRef = useRef<HTMLDivElement | null>(null);
  const headerRef = useRef<HTMLDivElement | null>(null);

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
  const [widths, setWidths] = useControllable<Readonly<Record<string, number>>>(
    widthsProp,
    defaultColumnWidths,
    onColumnWidthsChange as ((v: Readonly<Record<string, number>>) => void) | undefined,
  );
  const [pinning, setPinning] = useControllable<Readonly<Record<string, DataGridPin | false>>>(
    pinningProp,
    defaultColumnPinning,
    onColumnPinningChange as
      ((v: Readonly<Record<string, DataGridPin | false>>) => void) | undefined,
  );
  const [visibility, setVisibility] = useControllable<Readonly<Record<string, boolean>>>(
    visibilityProp,
    defaultColumnVisibility,
    onColumnVisibilityChange as ((v: Readonly<Record<string, boolean>>) => void) | undefined,
  );
  const order = columnOrder ?? defaultColumnOrder;

  /* ------------------------------------------------------------- columns */
  const isShown = useCallback(
    (c: DataGridColumn<T>) =>
      c.hideable === false ? true : (visibility[c.id] ?? !c.defaultHidden),
    [visibility],
  );
  const pinOf = useCallback(
    (c: DataGridColumn<T>): DataGridPin | null => {
      const p = pinning[c.id];
      if (p === false) return null;
      return p ?? c.pinned ?? null;
    },
    [pinning],
  );
  const widthOf = useCallback(
    (c: DataGridColumn<T>) => widths[c.id] ?? c.width ?? DEFAULT_WIDTH,
    [widths],
  );
  const dataColumns = useMemo(() => {
    const visible = orderColumns(columns, order).filter(isShown);
    return [
      ...visible.filter((c) => pinOf(c) === 'start'),
      ...visible.filter((c) => !pinOf(c)),
      ...visible.filter((c) => pinOf(c) === 'end'),
    ];
  }, [columns, order, isShown, pinOf]);
  const slots = useMemo<Slot<T>[]>(
    () => [
      ...(selectable
        ? [{ kind: 'select' as const, key: '__select', width: SELECT_WIDTH, pin: 'start' as const }]
        : []),
      ...dataColumns.map((c) => ({
        kind: 'data' as const,
        key: c.id,
        width: widthOf(c),
        pin: pinOf(c),
        column: c,
      })),
      ...(rowActions
        ? [
            {
              kind: 'actions' as const,
              key: '__actions',
              width: ACTIONS_WIDTH,
              pin: 'end' as const,
            },
          ]
        : []),
    ],
    [selectable, dataColumns, widthOf, pinOf, rowActions],
  );
  // Sticky offsets of pinned slots.
  const offsets = useMemo(() => {
    const out = new Map<string, { side: DataGridPin; px: number; edge: boolean }>();
    let start = 0;
    const startSlots = slots.filter((s) => s.pin === 'start');
    startSlots.forEach((s, i) => {
      out.set(s.key, { side: 'start', px: start, edge: i === startSlots.length - 1 });
      start += s.width;
    });
    let end = 0;
    const endSlots = slots.filter((s) => s.pin === 'end');
    endSlots
      .slice()
      .reverse()
      .forEach((s, i) => {
        out.set(s.key, { side: 'end', px: end, edge: i === endSlots.length - 1 });
        end += s.width;
      });
    return { map: out, start, end };
  }, [slots]);
  const totalWidth = slots.reduce((n, s) => n + s.width, 0);
  const template = slots.map((s) => `${s.width}px`).join(' ');
  const firstDataId = dataColumns.find((c) => c.rowHeader !== false)?.id;
  const anyEditable = !!onCellEdit && dataColumns.some((c) => c.editable);

  /* ---------------------------------------------------------------- rows */
  const sorted = useMemo(
    () => (manualSorting || !sort.length ? rows.slice() : sortRows(rows, sort, columns)),
    [rows, sort, columns, manualSorting],
  );
  const indexOfRow = useMemo(() => new Map(rows.map((r, i) => [r, i])), [rows]);
  const rowId = useCallback(
    (row: T) => getRowId(row, indexOfRow.get(row) ?? 0),
    [getRowId, indexOfRow],
  );
  const rowLabel = (row: T) => getRowLabel?.(row) ?? rowId(row);
  const selectedSet = useMemo(() => new Set(selected), [selected]);
  const canSelect = (row: T) => isRowSelectable?.(row) ?? true;
  const selectableIds = useMemo(
    () => (selectable ? sorted.filter((r) => isRowSelectable?.(r) ?? true).map(rowId) : []),
    [selectable, sorted, isRowSelectable, rowId],
  );
  const selectedCount = selectableIds.filter((id) => selectedSet.has(id)).length;
  const allSelected = selectableIds.length > 0 && selectedCount === selectableIds.length;
  // The Shift-range anchor is a row id, not a position: after a sort the
  // same position holds another row.
  const lastToggled = useRef<string | null>(null);

  const toggleRow = (index: number, on: boolean, range = false) => {
    const next = new Set(selected);
    const apply = (r: T) => {
      if (!canSelect(r)) return;
      if (on) next.add(rowId(r));
      else next.delete(rowId(r));
    };
    const target = sorted[index];
    const anchorId = lastToggled.current;
    const anchor = range && anchorId !== null ? sorted.findIndex((r) => rowId(r) === anchorId) : -1;
    if (anchor !== -1) {
      const [a, b] = [anchor, index].sort((x, y) => x - y) as [number, number];
      for (const r of sorted.slice(a, b + 1)) apply(r);
    } else if (target !== undefined) apply(target);
    lastToggled.current = target === undefined ? null : rowId(target);
    setSelected([...next]);
  };
  const toggleAll = (on: boolean) => {
    const next = new Set(selected);
    for (const id of selectableIds) {
      if (on) next.add(id);
      else next.delete(id);
    }
    setSelected([...next]);
  };

  /* ---------------------------------------------------------- active cell */
  // r: 0 is the header row, 1… the data rows in display order; c: slot index.
  const [active, setActive] = useState({ r: 0, c: 0 });
  const activeR = Math.min(active.r, sorted.length);
  const activeC = Math.min(active.c, Math.max(0, slots.length - 1));
  const pendingFocus = useRef(false);
  const entered = useRef(false);
  const [editing, setEditing] = useState<{ row: string; col: string; draft?: string } | null>(null);
  const [menuFor, setMenuFor] = useState<string | null>(null);
  // A column-menu choice runs after the menu closes: changing columns while
  // the menu is open would move its trigger mid-close.
  const menuAction = useRef<(() => void) | null>(null);
  const later = (action: () => void) => {
    menuAction.current = action;
  };
  const pendingHeader = useRef<{ id: string; c: number } | null>(null);

  const [headerHeight, setHeaderHeight] = useState(0);
  useIsoLayoutEffect(() => {
    const el = headerRef.current;
    if (!el) return;
    const read = () => setHeaderHeight(el.getBoundingClientRect().height);
    read();
    if (typeof ResizeObserver === 'undefined') return;
    const ro = new ResizeObserver(read);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const v = useVirtualizer({
    count: sorted.length,
    getScrollElement: () => scrollerRef.current,
    estimateSize: estimatedRowHeight ?? (density === 'compact' ? 36 : 44),
    getItemKey: (i) => {
      const r = sorted[i];
      return r === undefined ? i : rowId(r);
    },
    scrollMargin: headerHeight,
    keepMounted: activeR > 0 ? [activeR - 1] : undefined,
  });

  // Ask for more rows when the last ones come into view.
  const asked = useRef(-1);
  useEffect(() => {
    if (!onEndReached || loading || !sorted.length) return;
    if (v.visibleRange.end >= sorted.length - 1 && asked.current !== sorted.length) {
      asked.current = sorted.length;
      onEndReached();
    }
  }, [v.visibleRange.end, sorted.length, loading, onEndReached]);

  const cellAt = (r: number, c: number): HTMLElement | null =>
    gridRef.current?.querySelector<HTMLElement>(`[data-mn-cell][data-r="${r}"][data-c="${c}"]`) ??
    null;

  // Roving tabindex: one tab stop, on the active cell or its only widget.
  useIsoLayoutEffect(() => {
    const grid = gridRef.current;
    if (!grid) return;
    grid.querySelectorAll<HTMLElement>('[data-mn-cell]').forEach((cell) => {
      const isActive = Number(cell.dataset.r) === activeR && Number(cell.dataset.c) === activeC;
      const widgets = widgetsOf(cell);
      const target = widgets.length === 1 ? widgets[0] : cell;
      for (const w of widgets) if (w !== target || !isActive) w.tabIndex = -1;
      if (target !== cell) cell.tabIndex = -1;
      if (target) target.tabIndex = isActive ? 0 : -1;
    });
    if (pendingHeader.current) {
      // Follow the column to its new place, or a neighbour when it was hidden.
      const { id, c } = pendingHeader.current;
      const found = slots.findIndex((s) => s.key === id);
      const at = found >= 0 ? found : Math.max(0, Math.min(c, slots.length - 1) - 1);
      pendingHeader.current = null;
      if (at !== activeC || activeR !== 0) {
        pendingFocus.current = true;
        setActive({ r: 0, c: at });
        return;
      }
      const cell = cellAt(0, at);
      if (cell) targetOf(cell).focus();
    }
    if (pendingFocus.current) {
      pendingFocus.current = false;
      const cell = cellAt(activeR, activeC);
      if (cell) targetOf(cell).focus();
    }
  });

  const focusCell = (r: number, c: number) => {
    const rr = Math.max(0, Math.min(r, sorted.length));
    const cc = Math.max(0, Math.min(c, slots.length - 1));
    entered.current = false;
    if (rr > 0) v.scrollToIndex(rr - 1);
    pendingFocus.current = true;
    setActive({ r: rr, c: cc });
    // Already rendered: focus now so the next key press lands on it.
    const cell = cellAt(rr, cc);
    if (cell) {
      pendingFocus.current = false;
      targetOf(cell).tabIndex = 0;
      targetOf(cell).focus();
    }
  };

  /* ------------------------------------------------------------- actions */
  const pressSort = (c: DataGridColumn<T>, additive: boolean) =>
    setSort(nextSort(sort, c.id, multiSort && additive));
  const sortTo = (c: DataGridColumn<T>, direction: 'ascending' | 'descending' | null) => {
    const rest = multiSort ? sort.filter((s) => s.id !== c.id) : [];
    setSort(direction ? [{ id: c.id, direction }, ...rest] : rest);
  };
  const resizeTo = (c: DataGridColumn<T>, w: number) =>
    setWidths({
      ...widths,
      [c.id]: Math.round(Math.min(c.maxWidth ?? 640, Math.max(c.minWidth ?? 64, w))),
    });
  const pinTo = (c: DataGridColumn<T>, pin: DataGridPin | null) =>
    setPinning({ ...pinning, [c.id]: pin ?? false });
  const hide = (c: DataGridColumn<T>) => setVisibility({ ...visibility, [c.id]: false });

  const isEditable = (c: DataGridColumn<T>, row: T) =>
    !!onCellEdit && (typeof c.editable === 'function' ? c.editable(row) : !!c.editable);

  const startEdit = (row: T, c: DataGridColumn<T>, draft?: string) => {
    if (!isEditable(c, row)) return false;
    setEditing(
      draft === undefined ? { row: rowId(row), col: c.id } : { row: rowId(row), col: c.id, draft },
    );
    return true;
  };

  /* ------------------------------------------------------------ keyboard */
  const locate = (target: HTMLElement) => {
    const grid = gridRef.current;
    if (!grid || !grid.contains(target)) return null;
    const cell = target.closest<HTMLElement>('[data-mn-cell]');
    if (!cell) return null;
    return { cell, r: Number(cell.dataset.r), c: Number(cell.dataset.c) };
  };
  const rowAt = (r: number) => (r > 0 ? sorted[r - 1] : undefined);
  const pageSize = () => {
    const { start, end } = v.visibleRange;
    return start >= 0 && end > start ? end - start : 10;
  };

  const onKeyDownCapture = (e: ReactKeyboardEvent<HTMLDivElement>) => {
    // Menu triggers open on ArrowDown; inside the grid, arrows move between cells.
    const t = e.target as HTMLElement;
    if ((e.key === 'ArrowDown' || e.key === 'ArrowUp') && t.matches('[aria-haspopup]')) {
      const at = locate(t);
      if (!at) return;
      e.preventDefault();
      e.stopPropagation();
      focusCell(at.r + (e.key === 'ArrowDown' ? 1 : -1), at.c);
    }
  };

  const onKeyDown = (e: ReactKeyboardEvent<HTMLDivElement>) => {
    if (e.defaultPrevented) return;
    const t = e.target as HTMLElement;
    const at = locate(t);
    if (!at) return;
    const { cell, r, c } = at;
    const onCell = t === cell;
    const slot = slots[c];
    const row = rowAt(r);

    // Inside an editor: Tab saves (on blur) and moves along the row.
    if (t.closest('.mn-editable-cell__editor')) {
      if (e.key === 'Tab') {
        e.preventDefault();
        focusCell(r, c + (e.shiftKey ? -1 : 1));
      }
      return;
    }
    const widgets = widgetsOf(cell);
    if (!onCell && widgets.length > 1) {
      if (e.key === 'Escape') {
        e.preventDefault();
        entered.current = false;
        cell.focus();
      } else if (e.key === 'Tab') {
        const i = widgets.indexOf(t);
        if (i === -1) return;
        e.preventDefault();
        widgets[(i + (e.shiftKey ? -1 : 1) + widgets.length) % widgets.length]?.focus();
      }
      return;
    }
    if (!onCell && t.matches('input:not([type="checkbox"]),textarea,select')) return;

    const rtl = readDir(gridRef.current ?? cell) === 'rtl';
    const column = slot?.kind === 'data' ? slot.column : undefined;

    if (r === 0 && column) {
      if (e.altKey && (e.key === 'ArrowLeft' || e.key === 'ArrowRight')) {
        if (column.resizable === false) return;
        e.preventDefault();
        const dir = (e.key === 'ArrowRight' ? 1 : -1) * (rtl ? -1 : 1);
        resizeTo(column, widthOf(column) + dir * (e.shiftKey ? 64 : 16));
        return;
      }
      if (
        columnMenu &&
        ((e.altKey && e.key === 'ArrowDown') ||
          e.key === 'ContextMenu' ||
          (e.shiftKey && e.key === 'F10'))
      ) {
        e.preventDefault();
        setMenuFor(column.id);
        return;
      }
    }

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
          ? focusCell(sorted.length, slots.length - 1)
          : focusCell(r, slots.length - 1);
      case 'PageDown':
        e.preventDefault();
        return focusCell(r + pageSize(), c);
      case 'PageUp':
        e.preventDefault();
        return focusCell(r - pageSize(), c);
    }
    if (!onCell) return;

    if (e.key === 'Enter' || e.key === 'F2') {
      if (r === 0) {
        if (!column) return;
        e.preventDefault();
        if (e.key === 'Enter' && column.sortable) pressSort(column, e.shiftKey);
        else if (columnMenu) setMenuFor(column.id);
        return;
      }
      if (row !== undefined && column && startEdit(row, column)) {
        e.preventDefault();
        return;
      }
      if (widgets.length > 1) {
        e.preventDefault();
        entered.current = true;
        widgets[0]?.focus();
        return;
      }
      if (e.key === 'Enter' && row !== undefined && onRowOpen) {
        e.preventDefault();
        onRowOpen(row);
      }
      return;
    }
    if (e.key === ' ' && selectable && row !== undefined) {
      e.preventDefault();
      if (canSelect(row)) toggleRow(r - 1, !selectedSet.has(rowId(row)), e.shiftKey);
      return;
    }
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'a' && selectable) {
      e.preventDefault();
      toggleAll(!allSelected);
      return;
    }
    // Typing on an editable cell starts editing with that character.
    if (
      row !== undefined &&
      column &&
      e.key.length === 1 &&
      !e.ctrlKey &&
      !e.metaKey &&
      !e.altKey &&
      e.key !== ' '
    ) {
      const kind = column.editor ?? (column.numeric ? 'number' : 'text');
      if ((kind === 'text' || kind === 'number') && startEdit(row, column, e.key))
        e.preventDefault();
    }
  };

  const onFocus = (e: ReactFocusEvent<HTMLDivElement>) => {
    const at = locate(e.target as HTMLElement);
    if (!at) return;
    if (at.r !== activeR || at.c !== activeC) setActive({ r: at.r, c: at.c });
  };

  /* ------------------------------------------------------------ rendering */
  const pinStyle = (key: string): CSSProperties | undefined => {
    const o = offsets.map.get(key);
    if (!o) return undefined;
    return o.side === 'start' ? { insetInlineStart: o.px } : { insetInlineEnd: o.px };
  };
  const pinAttrs = (key: string) => {
    const o = offsets.map.get(key);
    return {
      'data-pinned': o?.side,
      'data-pinned-edge': o?.edge || undefined,
    };
  };

  const headerCell = (slot: Slot<T>, c: number) => {
    const base = {
      'data-mn-cell': '',
      'data-r': 0,
      'data-c': c,
      'aria-colindex': c + 1,
      tabIndex: -1,
      style: pinStyle(slot.key),
      ...pinAttrs(slot.key),
    };
    if (slot.kind === 'select') {
      return (
        <div
          key={slot.key}
          {...base}
          role="columnheader"
          className="mn-data-grid__cell mn-data-grid__head mn-data-grid__utility"
        >
          <Checkbox
            aria-label={m.selectAll}
            disabled={selectableIds.length === 0}
            checked={allSelected ? true : selectedCount > 0 ? 'indeterminate' : false}
            onCheckedChange={(on) => toggleAll(on === true)}
          />
        </div>
      );
    }
    if (slot.kind === 'actions') {
      return (
        <div
          key={slot.key}
          {...base}
          role="columnheader"
          className="mn-data-grid__cell mn-data-grid__head mn-data-grid__utility"
        >
          <span className="mn-visually-hidden">{m.actionsHeader}</span>
        </div>
      );
    }
    const col = slot.column;
    const s = sort.findIndex((x) => x.id === col.id);
    const dir = sort[s]?.direction ?? 'none';
    const name = labelOf(col);
    const canPin = col.pinnable !== false;
    const canHide = col.hideable !== false && dataColumns.length > 1;
    const pin = pinOf(col);
    return (
      // Keyboard sorting is handled by the grid (Enter on the focused header cell).
      // eslint-disable-next-line jsx-a11y/click-events-have-key-events
      <div
        key={slot.key}
        {...base}
        tabIndex={-1}
        role="columnheader"
        // Named by its label only, not the menu button inside it.
        aria-labelledby={`${uid}-h-${col.id}`}
        aria-sort={col.sortable && s === 0 ? dir : undefined}
        data-sortable={col.sortable || undefined}
        data-numeric={col.numeric || undefined}
        data-column-id={col.id}
        className={cx('mn-data-grid__cell mn-data-grid__head', col.headerClassName)}
        onClick={(e) => {
          if (!col.sortable) return;
          if ((e.target as HTMLElement).closest('button,[data-mn-resize]')) return;
          pressSort(col, e.shiftKey);
        }}
      >
        <span id={`${uid}-h-${col.id}`} className="mn-data-grid__head-label">
          {col.header}
        </span>
        {col.sortable ? (
          <ChevronDownGlyph className="mn-data-grid__sort-icon" data-direction={dir} aria-hidden />
        ) : null}
        {sort.length > 1 && s !== -1 ? (
          <span className="mn-data-grid__sort-priority" aria-hidden>
            {s + 1}
          </span>
        ) : null}
        {columnMenu && (col.sortable || canPin || canHide) ? (
          <DropdownMenu
            open={menuFor === col.id}
            onOpenChange={(open) => setMenuFor(open ? col.id : null)}
          >
            <DropdownMenuTrigger asChild>
              <button
                type="button"
                className="mn-data-grid__menu-button"
                aria-label={m.columnMenu(name)}
                tabIndex={-1}
                // Reached with Alt+ArrowDown from the header cell, not as the cell's widget.
                data-mn-grid-skip=""
              >
                <MoreGlyph aria-hidden />
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent
              align="end"
              className="mn-data-grid__menu"
              onCloseAutoFocus={(e) => {
                // Run the chosen action once the menu is gone (it can move or
                // remove this column), then focus the header cell again.
                e.preventDefault();
                const action = menuAction.current;
                menuAction.current = null;
                action?.();
                pendingHeader.current = { id: col.id, c };
                setActive({ r: 0, c });
              }}
            >
              {col.sortable ? (
                <>
                  <DropdownMenuItem
                    disabled={dir === 'ascending'}
                    onSelect={() => later(() => sortTo(col, 'ascending'))}
                  >
                    {m.sortAscending}
                  </DropdownMenuItem>
                  <DropdownMenuItem
                    disabled={dir === 'descending'}
                    onSelect={() => later(() => sortTo(col, 'descending'))}
                  >
                    {m.sortDescending}
                  </DropdownMenuItem>
                  {s !== -1 ? (
                    <DropdownMenuItem onSelect={() => later(() => sortTo(col, null))}>
                      {m.clearSort}
                    </DropdownMenuItem>
                  ) : null}
                </>
              ) : null}
              {col.sortable && (canPin || canHide) ? <DropdownMenuSeparator /> : null}
              {canPin ? (
                <>
                  {pin !== 'start' ? (
                    <DropdownMenuItem onSelect={() => later(() => pinTo(col, 'start'))}>
                      {m.pinStart}
                    </DropdownMenuItem>
                  ) : null}
                  {pin !== 'end' ? (
                    <DropdownMenuItem onSelect={() => later(() => pinTo(col, 'end'))}>
                      {m.pinEnd}
                    </DropdownMenuItem>
                  ) : null}
                  {pin ? (
                    <DropdownMenuItem onSelect={() => later(() => pinTo(col, null))}>
                      {m.unpin}
                    </DropdownMenuItem>
                  ) : null}
                </>
              ) : null}
              {canPin && canHide ? <DropdownMenuSeparator /> : null}
              {canHide ? (
                <DropdownMenuItem onSelect={() => later(() => hide(col))}>
                  {m.hideColumn}
                </DropdownMenuItem>
              ) : null}
            </DropdownMenuContent>
          </DropdownMenu>
        ) : null}
        {col.resizable !== false ? (
          <ResizeHandle
            value={widthOf(col)}
            onChange={(w) => resizeTo(col, w)}
            onReset={() =>
              setWidths(Object.fromEntries(Object.entries(widths).filter(([k]) => k !== col.id)))
            }
          />
        ) : null}
      </div>
    );
  };

  const bodyCell = (slot: Slot<T>, c: number, row: T, r: number, id: string) => {
    const base = {
      'data-mn-cell': '',
      'data-r': r,
      'data-c': c,
      'aria-colindex': c + 1,
      tabIndex: -1,
      style: pinStyle(slot.key),
      ...pinAttrs(slot.key),
    };
    if (slot.kind === 'select') {
      return (
        <div
          key={slot.key}
          {...base}
          role="gridcell"
          className="mn-data-grid__cell mn-data-grid__utility"
        >
          <Checkbox
            aria-label={m.selectRow(rowLabel(row))}
            checked={selectedSet.has(id)}
            disabled={!canSelect(row)}
            onClick={(e) => {
              if (e.shiftKey) {
                e.preventDefault();
                toggleRow(r - 1, !selectedSet.has(id), true);
              }
            }}
            onCheckedChange={(on) => toggleRow(r - 1, on === true)}
          />
        </div>
      );
    }
    if (slot.kind === 'actions') {
      return (
        <div
          key={slot.key}
          {...base}
          role="gridcell"
          className="mn-data-grid__cell mn-data-grid__utility"
        >
          <RowActionsMenu
            size="xs"
            actions={rowActions?.(row) ?? []}
            rowLabel={rowLabel(row)}
            messages={{ trigger: m.rowActions }}
          />
        </div>
      );
    }
    const col = slot.column;
    const raw = valueOf(col, row);
    const content = col.cell ? col.cell(row, r - 1) : formatValue(raw, dateLocale, dateZone);
    const header = col.rowHeader ?? col.id === firstDataId;
    const editable = isEditable(col, row);
    const isEditing = !!editing && editing.row === id && editing.col === col.id;
    return (
      // A grid cell (focusable through `base`); double-click mirrors Enter / F2.
      // eslint-disable-next-line jsx-a11y/no-static-element-interactions
      <div
        key={slot.key}
        {...base}
        role={header ? 'rowheader' : 'gridcell'}
        aria-readonly={anyEditable && !editable ? true : undefined}
        data-numeric={col.numeric || undefined}
        data-editable={editable || undefined}
        data-editing={isEditing || undefined}
        data-column-id={col.id}
        className={cx('mn-data-grid__cell', header && 'mn-data-grid__row-head', col.className)}
        onDoubleClick={(e: ReactMouseEvent) => {
          if ((e.target as HTMLElement).closest('button,a,input,select,textarea')) return;
          if (!startEdit(row, col) && onRowOpen) onRowOpen(row);
        }}
      >
        {editable ? (
          <EditableCell
            trigger="none"
            type={col.editor ?? (col.numeric ? 'number' : 'text')}
            label={m.editLabel(labelOf(col), rowLabel(row))}
            value={editValue(raw)}
            format={() => content}
            {...(col.options ? { options: col.options } : {})}
            {...(col.required ? { required: true } : {})}
            {...(col.min !== undefined ? { min: col.min } : {})}
            {...(col.max !== undefined ? { max: col.max } : {})}
            {...(col.validate
              ? { validate: (next: EditableCellValue) => col.validate?.(next, row) }
              : {})}
            {...(col.numeric !== undefined ? { numeric: col.numeric } : {})}
            editing={isEditing}
            {...(isEditing && editing?.draft !== undefined ? { initialDraft: editing.draft } : {})}
            onCommit={(next) => onCellEdit?.(row, col.id, next)}
            onEditingChange={(on, reason) => {
              if (on) return;
              setEditing((cur) => (cur && cur.row === id && cur.col === col.id ? null : cur));
              if (reason !== 'blur') {
                pendingFocus.current = true;
                setActive({ r, c });
              }
            }}
          />
        ) : (
          content
        )}
      </div>
    );
  };

  const empty = sorted.length === 0;
  const name = labelledBy ? { 'aria-labelledby': labelledBy } : { 'aria-label': label };

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-data-grid', className)}
      data-density={density}
      data-busy={loading || undefined}
      style={style}
    >
      <span className="mn-visually-hidden" role="status">
        {loading ? m.loading : ''}
      </span>
      {loading && !empty ? <div className="mn-data-grid__progress" aria-hidden /> : null}
      <div
        ref={scrollerRef}
        className="mn-data-grid__scroller"
        style={{
          height: empty ? undefined : height,
          scrollPaddingBlockStart: headerHeight,
          scrollPaddingInlineStart: offsets.start,
          scrollPaddingInlineEnd: offsets.end,
        }}
      >
        {/* Focus lives on the cells (roving tabindex), not the grid itself. */}
        {/* eslint-disable-next-line jsx-a11y/interactive-supports-focus */}
        <div
          ref={gridRef}
          role="grid"
          {...name}
          aria-rowcount={(totalRowCount ?? sorted.length) + 1}
          aria-colcount={slots.length}
          aria-multiselectable={selectable || undefined}
          aria-busy={loading || undefined}
          className="mn-data-grid__grid"
          style={{ '--_cols': template, inlineSize: totalWidth } as CSSProperties}
          onKeyDownCapture={onKeyDownCapture}
          onKeyDown={onKeyDown}
          onFocus={onFocus}
        >
          <div ref={headerRef} role="rowgroup" className="mn-data-grid__header">
            <div role="row" aria-rowindex={1} className="mn-data-grid__row">
              {slots.map(headerCell)}
            </div>
          </div>
          <div role="rowgroup" className="mn-data-grid__body" style={{ blockSize: v.totalSize }}>
            {v.items.map((item) => {
              const row = sorted[item.index] as T;
              const id = rowId(row);
              const r = item.index + 1;
              const isSelected = selectable && selectedSet.has(id);
              return (
                <div
                  key={id}
                  ref={v.measureElement}
                  role="row"
                  data-index={item.index}
                  data-row-id={id}
                  aria-rowindex={r + 1}
                  aria-selected={selectable ? isSelected : undefined}
                  data-selected={isSelected || undefined}
                  className="mn-data-grid__row"
                  style={{ transform: `translateY(${item.start}px)` }}
                >
                  {slots.map((slot, c) => bodyCell(slot, c, row, r, id))}
                </div>
              );
            })}
          </div>
        </div>
      </div>
      {empty ? (
        <div className="mn-data-grid__state">
          {loading ? (
            <LoadingState label={m.loading} variant="rows" rows={5} />
          ) : (
            (emptyState ?? (
              <EmptyState
                size="compact"
                headingLevel="p"
                title={m.emptyTitle}
                description={m.emptyDescription}
              />
            ))
          )}
        </div>
      ) : null}
    </div>
  );
}

/**
 * A spreadsheet-like data grid (WAI-ARIA grid): one tab stop with arrow-key
 * movement between cells, rows virtualized for tens of thousands of records,
 * sortable, resizable, pinnable and hideable columns with header menus, row
 * selection, inline cell editing (text, number, select, date) and row action
 * menus. Shares DataTable's column model, sort helpers and column-state hook.
 */
export const DataGrid = forwardRef(DataGridInner) as <T>(
  props: DataGridProps<T> & RefAttributes<HTMLDivElement>,
) => ReactElement | null;

/* ----------------------------------------------------------- resize handle */

function ResizeHandle({
  value,
  onChange,
  onReset,
}: {
  value: number;
  onChange: (value: number) => void;
  onReset: () => void;
}) {
  const readDir = useDirection();
  const start = useRef<{ x: number; w: number; dir: number } | null>(null);
  const onPointerDown = (e: ReactPointerEvent<HTMLSpanElement>) => {
    e.preventDefault();
    e.stopPropagation();
    const el = e.currentTarget;
    el.setPointerCapture?.(e.pointerId);
    start.current = {
      x: e.clientX,
      w: value,
      dir: readDir(el) === 'rtl' ? -1 : 1,
    };
    el.setAttribute('data-dragging', '');
  };
  const onPointerMove = (e: ReactPointerEvent<HTMLSpanElement>) => {
    const s = start.current;
    if (s) onChange(s.w + (e.clientX - s.x) * s.dir);
  };
  const end = (e: ReactPointerEvent<HTMLSpanElement>) => {
    start.current = null;
    e.currentTarget.removeAttribute('data-dragging');
  };
  // Pointer only: keyboard users resize with Alt+Arrow keys on the header cell.
  return (
    <span
      aria-hidden
      data-mn-resize=""
      className="mn-data-grid__resize"
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={end}
      onPointerCancel={end}
      onClick={(e) => e.stopPropagation()}
      onDoubleClick={(e) => {
        e.stopPropagation();
        onReset();
      }}
    />
  );
}
