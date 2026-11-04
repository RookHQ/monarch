/**
 * Drag internals for KanbanBoard: pure list maths and pointer hit-testing.
 *
 * ReorderableList (wave 4) has the same keyboard model but keeps its drag
 * code inline. These helpers are written to be lifted into a shared
 * `src/internal/drag/` once both have landed.
 */

/** Where a card is: its column and its position in that column. */
export interface BoardLocation {
  columnId: string;
  index: number;
}

/** The minimal column shape the helpers work on. */
export interface BoardColumnLike<T> {
  id: string;
  items: readonly T[];
}

/** Finds a card by key. Returns null when no column holds it. */
export function locate<T>(
  columns: readonly BoardColumnLike<T>[],
  key: string,
  getKey: (item: T) => string,
): (BoardLocation & { item: T }) | null {
  for (const column of columns) {
    const index = column.items.findIndex((i) => getKey(i) === key);
    if (index !== -1) return { columnId: column.id, index, item: column.items[index] as T };
  }
  return null;
}

/**
 * Returns a copy of `columns` with the card `key` moved to `to`. The target
 * index is clamped to the column. Unknown cards or columns leave the columns
 * unchanged (the same array is returned), so the move is safe to replay on
 * newer data.
 */
export function moveCard<C extends BoardColumnLike<T>, T>(
  columns: readonly C[],
  key: string,
  to: BoardLocation,
  getKey: (item: T) => string,
): C[] {
  const from = locate(columns, key, getKey);
  const target = columns.find((c) => c.id === to.columnId);
  if (!from || !target) return columns as C[];
  return columns.map((column) => {
    if (column.id !== from.columnId && column.id !== to.columnId) return column;
    const items = column.items.slice();
    if (column.id === from.columnId) items.splice(from.index, 1);
    if (column.id === to.columnId) {
      items.splice(Math.max(0, Math.min(to.index, items.length)), 0, from.item);
    }
    return { ...column, items };
  });
}

/** Rectangle subset used for hit-testing, so tests can supply plain objects. */
export interface Box {
  left: number;
  right: number;
  top: number;
  bottom: number;
}

/**
 * The column under a pointer: the one whose horizontal span contains `x`.
 * Board columns sit side by side, so the vertical position does not matter.
 */
export function columnAt(columns: readonly { id: string; box: Box }[], x: number): string | null {
  for (const c of columns) if (x >= c.box.left && x <= c.box.right) return c.id;
  return null;
}

/**
 * The insertion index for a pointer at `y` among a column's other cards: how
 * many card centres lie above it. The dragged card must be left out of `boxes`.
 */
export function indexAt(boxes: readonly Box[], y: number): number {
  let index = 0;
  for (const b of boxes) if ((b.top + b.bottom) / 2 < y) index++;
  return index;
}

/** Pixels from a scroller's edge at which dragging starts to scroll it. */
export const EDGE = 48;

/**
 * Scroll speed (px per frame) for a pointer near an edge of `box`: negative
 * towards the start, positive towards the end, 0 outside the edge bands.
 */
export function edgeSpeed(start: number, end: number, at: number): number {
  if (at < start || at > end) return 0;
  if (at < start + EDGE) return -Math.ceil(((start + EDGE - at) / EDGE) * 16);
  if (at > end - EDGE) return Math.ceil(((at - (end - EDGE)) / EDGE) * 16);
  return 0;
}
