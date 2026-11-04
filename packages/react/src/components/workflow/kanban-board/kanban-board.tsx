'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useLayoutEffect,
  useRef,
  useState,
  type ForwardedRef,
  type HTMLAttributes,
  type KeyboardEvent,
  type PointerEvent as ReactPointerEvent,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph, WarningGlyph } from '../../../internal/icons.js';
import { inertProps } from '../../../internal/inert.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';
import {
  columnAt,
  edgeSpeed,
  indexAt,
  locate,
  moveCard,
  type BoardLocation,
} from './kanban-board.drag.js';

/** One column of the board. */
export interface KanbanColumn<T> {
  /** Stable, unique id. Passed back in `onMove`. */
  id: string;
  /** Plain-text name, shown in the header and used in announcements. */
  title: string;
  /** The cards, in display order. */
  items: readonly T[];
  /**
   * Work-in-progress limit. The header shows "count / limit" and the column is
   * marked at and over the limit. With `enforceWipLimits` a full column refuses drops.
   */
  wipLimit?: number;
  /** Decorative icon or colour dot before the title. */
  icon?: ReactNode;
  /** Header actions, such as an "Add card" icon button. */
  actions?: ReactNode;
  /** Content under the cards, such as an "Add card" button. */
  footer?: ReactNode;
  /** Shown when the column has no cards. Default `messages.empty`. */
  empty?: ReactNode;
}

/** A finished move, passed to `onMove`. */
export interface KanbanMove<T> {
  item: T;
  itemId: string;
  fromColumnId: string;
  fromIndex: number;
  toColumnId: string;
  toIndex: number;
}

/** What `renderCard` knows about the card it draws. */
export interface KanbanCardState {
  columnId: string;
  /** Position in the column as shown, including a move in progress. */
  index: number;
  /** This card is being moved (pointer or keyboard). */
  dragging: boolean;
  /** This render is the copy that follows the pointer. */
  overlay: boolean;
  /** The card's move is waiting for `onMove` to settle. */
  pending: boolean;
  /** The card cannot be moved (`disabled` or `isItemDisabled`). */
  disabled: boolean;
}

/** Labels and announcements. Override them to translate. */
export interface KanbanBoardMessages {
  /** Accessible name of a card's move handle. */
  handleLabel: (card: string) => string;
  /** Read by every handle (aria-describedby). */
  instructions: string;
  pickedUp: (card: string, column: string, position: number, total: number) => string;
  moved: (card: string, column: string, position: number, total: number) => string;
  dropped: (card: string, column: string, position: number, total: number) => string;
  cancelled: (card: string, column: string, position: number, total: number) => string;
  /** A target column refuses the card (`canMove` or a full WIP limit). */
  notAllowed: (card: string, column: string) => string;
  /** `onMove` rejected; the card is back where it was. */
  moveFailed: (card: string, column: string) => string;
  /** Accessible name of the per-card "Move to" menu button. */
  moveMenuLabel: (card: string) => string;
  moveMenuItem: (column: string) => string;
  /** Text alternative for the header count. */
  count: (count: number, limit?: number) => string;
  collapse: (column: string) => string;
  expand: (column: string) => string;
  /** Shown in an empty column. */
  empty: string;
  /** Shown on a column that refuses the card being dragged. */
  blocked: string;
}

const cards = (n: number) => `${n} ${n === 1 ? 'card' : 'cards'}`;

const DEFAULT_MESSAGES: KanbanBoardMessages = {
  handleLabel: (card) => `Move ${card}`,
  instructions:
    'Press Space or Enter to pick up. Arrow up and down move within a column, arrow left and right move between columns. Space or Enter drops, Escape cancels.',
  pickedUp: (card, col, p, n) => `Picked up ${card}. ${col}, position ${p} of ${n}.`,
  moved: (card, col, p, n) => `${card}: ${col}, position ${p} of ${n}.`,
  dropped: (card, col, p, n) => `${card} dropped in ${col}, position ${p} of ${n}.`,
  cancelled: (card, col, p, n) =>
    `Move cancelled. ${card} returned to ${col}, position ${p} of ${n}.`,
  notAllowed: (card, col) => `${card} cannot move to ${col}.`,
  moveFailed: (card, col) => `Could not move ${card}. It is back in ${col}.`,
  moveMenuLabel: (card) => `Move ${card} to…`,
  moveMenuItem: (col) => `Move to ${col}`,
  count: (n, limit) =>
    limit === undefined
      ? cards(n)
      : `${cards(n)}, limit ${limit}${n > limit ? ', over the limit' : n === limit ? ', at the limit' : ''}`,
  collapse: (col) => `Collapse ${col}`,
  expand: (col) => `Expand ${col}`,
  empty: 'No cards',
  blocked: 'Not allowed here',
};

export interface KanbanBoardProps<T> extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'defaultValue' | 'onChange'
> {
  /** The columns and their cards (controlled). Update them from `onMove`. */
  columns?: readonly KanbanColumn<T>[];
  /** The starting columns when uncontrolled. */
  defaultColumns?: readonly KanbanColumn<T>[];
  /** A stable, unique key for each card. */
  getItemId: (item: T) => string;
  /** Plain-text name of each card, used in labels and announcements. */
  getItemLabel: (item: T) => string;
  /** Draws a card's content. The move handle (and menu) are added around it. */
  renderCard: (item: T, state: KanbanCardState) => ReactNode;
  /**
   * Called once per finished move with the move and the columns after it.
   * The product persists the move here. Return a promise to keep the card in
   * its new place while it is pending: if it rejects, the card goes back and
   * the failure is announced. Controlled boards should pass the new `columns`
   * before the promise resolves (or right after, from a refetch).
   */
  onMove?: (move: KanbanMove<T>, columns: KanbanColumn<T>[]) => unknown;
  /** Called when the promise returned by `onMove` rejects. */
  onMoveError?: (error: unknown, move: KanbanMove<T>) => void;
  /** Whether a card may move to a column (its own column is always allowed). */
  canMove?: (item: T, toColumnId: string, fromColumnId: string) => boolean;
  /** A column at or over its `wipLimit` refuses new cards. */
  enforceWipLimits?: boolean;
  /** A card that cannot be moved, such as one the user may not edit. */
  isItemDisabled?: (item: T) => boolean;
  /** No card can be moved. */
  disabled?: boolean;
  /**
   * `handle` (default): drag from the grip. `card`: a mouse or pen can also
   * drag from anywhere on the card except its buttons, links and fields;
   * touch still uses the grip so the column can scroll.
   */
  dragFrom?: 'handle' | 'card';
  /**
   * Add a "Move to…" menu to every card: a single-pointer alternative to
   * dragging (WCAG 2.5.7). The card goes to the end of the chosen column.
   */
  moveMenu?: boolean;
  /** Show a collapse button in every column header. */
  collapsible?: boolean;
  /** Collapsed column ids (controlled). */
  collapsedColumnIds?: readonly string[];
  /** Collapsed column ids at first render (uncontrolled). */
  defaultCollapsedColumnIds?: readonly string[];
  onCollapsedColumnIdsChange?: (ids: string[]) => void;
  /** Heading level of the column titles. Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | 6;
  /** Override labels and announcements. */
  messages?: Partial<KanbanBoardMessages>;
}

interface Drag<T> {
  key: string;
  item: T;
  from: BoardLocation;
  mode: 'pointer' | 'keyboard';
  pointerId?: number;
  /** Pointer offset inside the card, and the card's size, at pick-up. */
  dx?: number;
  dy?: number;
  width?: number;
}

interface Pending<T> {
  seq: number;
  key: string;
  to: BoardLocation;
  /** The `columns` prop version when the move was made. */
  version: number;
  settled: boolean;
  move: KanbanMove<T>;
}

const NBSP = String.fromCharCode(160);
/** Pixels a pointer must travel before a press becomes a drag. */
const DRAG_THRESHOLD = 4;
const INTERACTIVE = 'a[href], button, input, select, textarea, [contenteditable], [role="button"]';

function GripGlyph() {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="currentColor"
      aria-hidden
      focusable={false}
    >
      <circle cx="6" cy="4" r="1.25" />
      <circle cx="10" cy="4" r="1.25" />
      <circle cx="6" cy="8" r="1.25" />
      <circle cx="10" cy="8" r="1.25" />
      <circle cx="6" cy="12" r="1.25" />
      <circle cx="10" cy="12" r="1.25" />
    </svg>
  );
}

function MoreGlyph() {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="currentColor"
      aria-hidden
      focusable={false}
    >
      <circle cx="3.5" cy="8" r="1.25" />
      <circle cx="8" cy="8" r="1.25" />
      <circle cx="12.5" cy="8" r="1.25" />
    </svg>
  );
}

function isPromiseLike(v: unknown): v is PromiseLike<unknown> {
  return typeof (v as { then?: unknown } | null)?.then === 'function';
}

function KanbanBoardInner<T>(
  {
    columns: columnsProp,
    defaultColumns,
    getItemId,
    getItemLabel,
    renderCard,
    onMove,
    onMoveError,
    canMove,
    enforceWipLimits = false,
    isItemDisabled,
    disabled = false,
    dragFrom = 'handle',
    moveMenu = false,
    collapsible = false,
    collapsedColumnIds,
    defaultCollapsedColumnIds,
    onCollapsedColumnIdsChange,
    headingLevel = 3,
    messages: messagesProp,
    className,
    onClickCapture,
    ...rest
  }: KanbanBoardProps<T>,
  ref: ForwardedRef<HTMLDivElement>,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const baseId = useId();
  const instructionsId = `${baseId}-instructions`;
  const Heading = `h${headingLevel}` as 'h3';

  const [internal, setInternal] = useState<readonly KanbanColumn<T>[]>(defaultColumns ?? []);
  const base = columnsProp ?? internal;

  // A new `columns` prop is new truth from the product: settled moves give way to it.
  const version = useRef(0);
  const lastProp = useRef(columnsProp);
  const [pending, setPending] = useState<Pending<T>[]>([]);
  if (lastProp.current !== columnsProp) {
    lastProp.current = columnsProp;
    version.current++;
  }
  useEffect(() => {
    setPending((list) => (list.some((p) => p.settled) ? list.filter((p) => !p.settled) : list));
  }, [columnsProp]);

  const committed = pending.reduce<KanbanColumn<T>[]>(
    (cols, p) => moveCard(cols, p.key, p.to, getItemId),
    base.slice(),
  );

  const [drag, setDrag] = useState<Drag<T> | null>(null);
  const dragRef = useRef<Drag<T> | null>(null);
  const [preview, setPreviewState] = useState<KanbanColumn<T>[] | null>(null);
  const previewRef = useRef<KanbanColumn<T>[] | null>(null);
  const setPreview = (p: KanbanColumn<T>[] | null) => {
    previewRef.current = p;
    setPreviewState(p);
  };
  const shown = preview ?? committed;
  const committedRef = useRef(committed);
  committedRef.current = committed;
  /** Column under the pointer during a pointer drag. */
  const [over, setOverState] = useState<{ columnId: string; blocked: boolean } | null>(null);
  const overRef = useRef(over);
  const setOver = (o: { columnId: string; blocked: boolean } | null) => {
    if (overRef.current?.columnId === o?.columnId && overRef.current?.blocked === o?.blocked)
      return;
    overRef.current = o;
    setOverState(o);
  };

  const [collapsedInternal, setCollapsedInternal] = useState<readonly string[]>(
    defaultCollapsedColumnIds ?? [],
  );
  const collapsed = new Set(collapsedColumnIds ?? collapsedInternal);
  const collapsedRef = useRef(collapsed);
  collapsedRef.current = collapsed;

  const [announcement, setAnnouncement] = useState('');
  const announce = useCallback((text: string) => {
    // Re-announce identical text by changing it slightly.
    setAnnouncement((prev) => (prev === text ? `${text}${NBSP}` : text));
  }, []);

  const rootRef = useRef<HTMLDivElement | null>(null);
  const scrollerRef = useRef<HTMLDivElement | null>(null);
  const columnNodes = useRef(new Map<string, HTMLElement>());
  const listNodes = useRef(new Map<string, HTMLElement>());
  const cardNodes = useRef(new Map<string, HTMLLIElement>());
  const handleNodes = useRef(new Map<string, HTMLButtonElement>());
  const toggleNodes = useRef(new Map<string, HTMLButtonElement>());
  const overlayRef = useRef<HTMLDivElement | null>(null);
  const pointer = useRef({ x: 0, y: 0 });
  const focusAfter = useRef<string | null>(null);
  const suppressClick = useRef(false);

  const titleOf = (cols: readonly KanbanColumn<T>[], id: string) =>
    cols.find((c) => c.id === id)?.title ?? id;
  const lengthOf = (cols: readonly KanbanColumn<T>[], id: string) =>
    cols.find((c) => c.id === id)?.items.length ?? 0;
  const isLocked = (item: T) => disabled || (isItemDisabled?.(item) ?? false);

  /** Whether the dragged card may go to `columnId`. */
  const isBlocked = (d: Pick<Drag<T>, 'item' | 'from' | 'key'>, columnId: string) => {
    if (columnId === d.from.columnId) return false;
    const column = committedRef.current.find((c) => c.id === columnId);
    if (!column) return true;
    const others = column.items.filter((i) => getItemId(i) !== d.key).length;
    if (enforceWipLimits && column.wipLimit !== undefined && others >= column.wipLimit) return true;
    return canMove ? !canMove(d.item, columnId, d.from.columnId) : false;
  };

  // Keep focus on the moved card's handle (it remounts when it changes column),
  // or on the column toggle when the card went into a collapsed column. The
  // pointer copy follows the pointer from wherever the layout put it.
  useLayoutEffect(() => {
    if (focusAfter.current) {
      const key = focusAfter.current;
      focusAfter.current = null;
      const where = locate(previewRef.current ?? committedRef.current, key, getItemId);
      const target =
        where && collapsedRef.current.has(where.columnId)
          ? toggleNodes.current.get(where.columnId)
          : handleNodes.current.get(key);
      if (target && document.activeElement !== target) target.focus({ preventScroll: false });
    }
    const d = dragRef.current;
    if (d?.mode === 'pointer' && overlayRef.current) {
      overlayRef.current.style.transform = `translate(${pointer.current.x - (d.dx ?? 0)}px, ${pointer.current.y - (d.dy ?? 0)}px)`;
    }
  });

  const commit = (key: string, item: T, from: BoardLocation, to: BoardLocation) => {
    const start = committedRef.current;
    const next = moveCard(start, key, to, getItemId);
    const move: KanbanMove<T> = {
      item,
      itemId: key,
      fromColumnId: from.columnId,
      fromIndex: from.index,
      toColumnId: to.columnId,
      toIndex: to.index,
    };
    const fail = (error: unknown) => {
      announce(
        messages.moveFailed(getItemLabel(item), titleOf(committedRef.current, from.columnId)),
      );
      onMoveError?.(error, move);
    };
    let result: unknown;
    try {
      result = onMove?.(move, next);
    } catch (error) {
      fail(error);
      return;
    }
    if (!isPromiseLike(result)) {
      if (columnsProp === undefined) setInternal(next);
      return;
    }
    const seq = Date.now() + Math.random();
    const startVersion = version.current;
    setPending((list) => [...list, { seq, key, to, version: startVersion, settled: false, move }]);
    result.then(
      () => {
        if (columnsProp === undefined) {
          setInternal((cols) => moveCard(cols, key, to, getItemId));
          setPending((list) => list.filter((p) => p.seq !== seq));
        } else {
          // Keep the card in place until the product's next `columns` arrive.
          setPending((list) =>
            version.current !== startVersion
              ? list.filter((p) => p.seq !== seq)
              : list.map((p) => (p.seq === seq ? { ...p, settled: true } : p)),
          );
        }
      },
      (error: unknown) => {
        setPending((list) => list.filter((p) => p.seq !== seq));
        fail(error);
      },
    );
  };

  const stopAutoScroll = useRef<(() => void) | null>(null);

  const endDrag = () => {
    dragRef.current = null;
    setDrag(null);
    setPreview(null);
    setOver(null);
    stopAutoScroll.current?.();
    stopAutoScroll.current = null;
  };

  /** Ends the move. `reason` cancel = Escape / Tab / outside; blocked = dropped on a refusing column. */
  const finish = (reason: 'drop' | 'cancel' | 'blocked') => {
    const d = dragRef.current;
    if (!d) return;
    const label = getItemLabel(d.item);
    const fromTitle = titleOf(committedRef.current, d.from.columnId);
    const fromTotal = lengthOf(committedRef.current, d.from.columnId);
    const to = locate(previewRef.current ?? committedRef.current, d.key, getItemId);
    if (reason === 'blocked' && overRef.current) {
      announce(
        `${messages.notAllowed(label, titleOf(committedRef.current, overRef.current.columnId))} ${messages.cancelled(label, fromTitle, d.from.index + 1, fromTotal)}`,
      );
    } else if (reason === 'cancel' || !to) {
      announce(messages.cancelled(label, fromTitle, d.from.index + 1, fromTotal));
    } else if (to.columnId === d.from.columnId && to.index === d.from.index) {
      announce(messages.dropped(label, fromTitle, d.from.index + 1, fromTotal));
    } else {
      const total = lengthOf(previewRef.current ?? committedRef.current, to.columnId);
      announce(
        messages.dropped(label, titleOf(committedRef.current, to.columnId), to.index + 1, total),
      );
      commit(d.key, d.item, d.from, { columnId: to.columnId, index: to.index });
    }
    focusAfter.current = d.key;
    endDrag();
  };

  // A drag abandoned because its card disappeared from the data ends quietly.
  useEffect(() => {
    const d = dragRef.current;
    if (d && !locate(committed, d.key, getItemId)) endDrag();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [base]);

  // ---- Keyboard ----------------------------------------------------------

  const startKeyboard = (item: T) => {
    const key = getItemId(item);
    const from = locate(committedRef.current, key, getItemId);
    if (!from) return;
    const d: Drag<T> = {
      key,
      item,
      from: { columnId: from.columnId, index: from.index },
      mode: 'keyboard',
    };
    dragRef.current = d;
    setDrag(d);
    setPreview(committedRef.current.slice());
    announce(
      messages.pickedUp(
        getItemLabel(item),
        titleOf(committedRef.current, from.columnId),
        from.index + 1,
        lengthOf(committedRef.current, from.columnId),
      ),
    );
  };

  const moveKeyboard = (to: BoardLocation) => {
    const d = dragRef.current;
    const list = previewRef.current;
    if (!d || !list) return;
    const current = locate(list, d.key, getItemId);
    if (!current) return;
    const label = getItemLabel(d.item);
    const sameColumn = to.columnId === current.columnId;
    const max = lengthOf(list, to.columnId) - (sameColumn ? 1 : 0);
    const index = Math.max(0, Math.min(to.index, max));
    if (!sameColumn || index !== current.index) {
      setPreview(moveCard(list, d.key, { columnId: to.columnId, index }, getItemId));
      focusAfter.current = d.key;
    }
    announce(messages.moved(label, titleOf(list, to.columnId), index + 1, max + 1));
  };

  /** The next column in `step` direction a keyboard move may enter. */
  const nextColumn = (fromId: string, step: 1 | -1, d: Drag<T>) => {
    const list = previewRef.current ?? committedRef.current;
    let i = list.findIndex((c) => c.id === fromId) + step;
    const refused: string[] = [];
    for (; i >= 0 && i < list.length; i += step) {
      const c = list[i] as KanbanColumn<T>;
      if (collapsedRef.current.has(c.id)) continue;
      if (!isBlocked(d, c.id)) return { id: c.id, refused };
      refused.push(c.title);
    }
    return { id: null, refused };
  };

  const onHandleKeyDown = (e: KeyboardEvent<HTMLButtonElement>, item: T) => {
    const d = dragRef.current;
    const key = getItemId(item);
    const grabbed = d?.mode === 'keyboard' && d.key === key;
    const here = grabbed ? locate(previewRef.current ?? [], key, getItemId) : null;
    const root = rootRef.current;
    const rtl =
      !!root &&
      (root.closest('[dir]')?.getAttribute('dir') === 'rtl' ||
        getComputedStyle(root).direction === 'rtl');
    const horizontal = (dir: 1 | -1) => {
      if (!grabbed || !here || !d) return;
      e.preventDefault();
      const step = (rtl ? -dir : dir) as 1 | -1;
      const { id, refused } = nextColumn(here.columnId, step, d);
      if (id) moveKeyboard({ columnId: id, index: here.index });
      else if (refused.length)
        announce(messages.notAllowed(getItemLabel(item), refused[0] as string));
      else moveKeyboard({ columnId: here.columnId, index: here.index });
    };
    switch (e.key) {
      case ' ':
      case 'Enter':
        e.preventDefault();
        if (e.repeat) return;
        if (grabbed) finish('drop');
        else if (!d) startKeyboard(item);
        break;
      case 'ArrowUp':
        if (grabbed && here) {
          e.preventDefault();
          moveKeyboard({ columnId: here.columnId, index: here.index - 1 });
        }
        break;
      case 'ArrowDown':
        if (grabbed && here) {
          e.preventDefault();
          moveKeyboard({ columnId: here.columnId, index: here.index + 1 });
        }
        break;
      case 'ArrowLeft':
        horizontal(-1);
        break;
      case 'ArrowRight':
        horizontal(1);
        break;
      case 'Home':
        if (grabbed && here) {
          e.preventDefault();
          moveKeyboard({ columnId: here.columnId, index: 0 });
        }
        break;
      case 'End':
        if (grabbed && here) {
          e.preventDefault();
          moveKeyboard({ columnId: here.columnId, index: Number.MAX_SAFE_INTEGER });
        }
        break;
      case 'Escape':
        if (d) {
          e.preventDefault();
          e.stopPropagation();
          finish('cancel');
        }
        break;
      case 'Tab':
        if (grabbed) finish('cancel');
        break;
    }
  };

  // ---- Pointer -----------------------------------------------------------

  /** Moves the preview to wherever the pointer is. */
  const hitTest = () => {
    const d = dragRef.current;
    const list = previewRef.current;
    if (!d || !list) return;
    const { x, y } = pointer.current;
    const boxes = list
      .map((c) => ({ id: c.id, node: columnNodes.current.get(c.id) }))
      .filter((c): c is { id: string; node: HTMLElement } => !!c.node)
      .map((c) => ({ id: c.id, box: c.node.getBoundingClientRect() }));
    const columnId = columnAt(boxes, x);
    if (!columnId) return setOver(null);
    if (isBlocked(d, columnId)) return setOver({ columnId, blocked: true });
    setOver({ columnId, blocked: false });
    const column = list.find((c) => c.id === columnId);
    if (!column) return;
    const others = column.items.filter((i) => getItemId(i) !== d.key);
    const index = collapsedRef.current.has(columnId)
      ? others.length
      : indexAt(
          others
            .map((i) => cardNodes.current.get(getItemId(i))?.getBoundingClientRect())
            .filter((b): b is DOMRect => !!b),
          y,
        );
    const current = locate(list, d.key, getItemId);
    if (current && (current.columnId !== columnId || current.index !== index)) {
      setPreview(moveCard(list, d.key, { columnId, index }, getItemId));
      announce(messages.moved(getItemLabel(d.item), column.title, index + 1, others.length + 1));
    }
  };

  const positionOverlay = () => {
    const d = dragRef.current;
    if (d?.mode === 'pointer' && overlayRef.current)
      overlayRef.current.style.transform = `translate(${pointer.current.x - (d.dx ?? 0)}px, ${pointer.current.y - (d.dy ?? 0)}px)`;
  };

  const startAutoScroll = () => {
    if (typeof requestAnimationFrame !== 'function') return;
    let frame = 0;
    const tick = () => {
      const { x, y } = pointer.current;
      let moved = false;
      const scroller = scrollerRef.current;
      if (scroller) {
        const r = scroller.getBoundingClientRect();
        const sx = y >= r.top && y <= r.bottom ? edgeSpeed(r.left, r.right, x) : 0;
        if (sx) {
          scroller.scrollLeft += sx;
          moved = true;
        }
      }
      const columnId = overRef.current?.columnId;
      const listNode = columnId ? listNodes.current.get(columnId) : undefined;
      if (listNode) {
        const r = listNode.getBoundingClientRect();
        const sy = x >= r.left && x <= r.right ? edgeSpeed(r.top, r.bottom, y) : 0;
        if (sy) {
          listNode.scrollTop += sy;
          moved = true;
        }
      }
      if (moved) hitTest();
      frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    stopAutoScroll.current = () => cancelAnimationFrame(frame);
  };

  const press = useRef<{ item: T; pointerId: number; x: number; y: number } | null>(null);

  const startPointer = (item: T, pointerId: number) => {
    const key = getItemId(item);
    const from = locate(committedRef.current, key, getItemId);
    const node = cardNodes.current.get(key);
    if (!from || !node) return;
    const rect = node.getBoundingClientRect();
    const p = press.current;
    const d: Drag<T> = {
      key,
      item,
      from: { columnId: from.columnId, index: from.index },
      mode: 'pointer',
      pointerId,
      dx: (p?.x ?? rect.left) - rect.left,
      dy: (p?.y ?? rect.top) - rect.top,
      width: rect.width,
    };
    dragRef.current = d;
    setDrag(d);
    setPreview(committedRef.current.slice());
    setOver({ columnId: from.columnId, blocked: false });
    startAutoScroll();
    announce(
      messages.pickedUp(
        getItemLabel(item),
        titleOf(committedRef.current, from.columnId),
        from.index + 1,
        lengthOf(committedRef.current, from.columnId),
      ),
    );
  };

  // Window listeners: the card remounts as it changes column, so pointer
  // capture on the original element would be lost mid-drag.
  const listening = useRef<(() => void) | null>(null);
  const listen = () => {
    if (listening.current) return;
    const move = (e: PointerEvent) => {
      const p = press.current;
      if (p && p.pointerId === e.pointerId) {
        if (Math.hypot(e.clientX - p.x, e.clientY - p.y) < DRAG_THRESHOLD) return;
        pointer.current = { x: e.clientX, y: e.clientY };
        startPointer(p.item, p.pointerId);
        press.current = null;
      }
      const d = dragRef.current;
      if (!d || d.mode !== 'pointer' || d.pointerId !== e.pointerId) return;
      e.preventDefault();
      pointer.current = { x: e.clientX, y: e.clientY };
      positionOverlay();
      hitTest();
    };
    const up = (e: PointerEvent) => end(e, false);
    const cancel = (e: PointerEvent) => end(e, true);
    const key = (e: globalThis.KeyboardEvent) => {
      if (e.key === 'Escape' && dragRef.current?.mode === 'pointer') {
        e.preventDefault();
        finish('cancel');
        unlisten();
      }
    };
    const end = (e: PointerEvent, cancelled: boolean) => {
      if (press.current?.pointerId === e.pointerId) {
        press.current = null;
        unlisten();
        return;
      }
      const d = dragRef.current;
      if (!d || d.mode !== 'pointer' || d.pointerId !== e.pointerId) return;
      pointer.current = { x: e.clientX, y: e.clientY };
      if (!cancelled) hitTest();
      const o = overRef.current;
      finish(cancelled || !o ? 'cancel' : o.blocked ? 'blocked' : 'drop');
      // The click that follows a drag must not open the card.
      suppressClick.current = true;
      setTimeout(() => {
        suppressClick.current = false;
      }, 0);
      unlisten();
    };
    const unlisten = () => {
      window.removeEventListener('pointermove', move);
      window.removeEventListener('pointerup', up);
      window.removeEventListener('pointercancel', cancel);
      window.removeEventListener('keydown', key, true);
      listening.current = null;
    };
    window.addEventListener('pointermove', move, { passive: false });
    window.addEventListener('pointerup', up);
    window.addEventListener('pointercancel', cancel);
    window.addEventListener('keydown', key, true);
    listening.current = unlisten;
  };
  useEffect(() => () => listening.current?.(), []);

  const onCardPointerDown = (e: ReactPointerEvent<HTMLLIElement>, item: T) => {
    if (dragRef.current || e.button !== 0 || isLocked(item)) return;
    const key = getItemId(item);
    const handle = handleNodes.current.get(key);
    const target = e.target as Element;
    const onHandle = !!handle && handle.contains(target);
    if (!onHandle) {
      if (dragFrom !== 'card' || e.pointerType === 'touch') return;
      const interactive = target.closest(INTERACTIVE);
      if (interactive && e.currentTarget.contains(interactive)) return;
    }
    // Keep the page from selecting text or scrolling; focus the handle ourselves.
    e.preventDefault();
    handle?.focus({ preventScroll: true });
    press.current = { item, pointerId: e.pointerId, x: e.clientX, y: e.clientY };
    listen();
  };

  // ---- Other actions -----------------------------------------------------

  const moveByMenu = (item: T, columnId: string) => {
    const key = getItemId(item);
    const from = locate(committedRef.current, key, getItemId);
    if (!from || from.columnId === columnId) return;
    const index = lengthOf(committedRef.current, columnId);
    announce(
      messages.dropped(
        getItemLabel(item),
        titleOf(committedRef.current, columnId),
        index + 1,
        index + 1,
      ),
    );
    focusAfter.current = key;
    commit(key, item, { columnId: from.columnId, index: from.index }, { columnId, index });
  };

  const toggleColumn = (id: string) => {
    const next = collapsed.has(id) ? [...collapsed].filter((c) => c !== id) : [...collapsed, id];
    if (collapsedColumnIds === undefined) setCollapsedInternal(next);
    onCollapsedColumnIdsChange?.(next);
  };

  const pendingKeys = new Set(pending.filter((p) => !p.settled).map((p) => p.key));
  const dragItem = drag?.item;

  return (
    <div
      ref={(node) => {
        rootRef.current = node;
        if (typeof ref === 'function') ref(node);
        else if (ref) ref.current = node;
      }}
      role={rest['aria-label'] || rest['aria-labelledby'] ? 'region' : undefined}
      {...rest}
      className={cx('mn-kanban-board', className)}
      data-dragging={drag ? drag.mode : undefined}
      onClickCapture={(e) => {
        if (suppressClick.current) {
          suppressClick.current = false;
          e.preventDefault();
          e.stopPropagation();
          return;
        }
        onClickCapture?.(e);
      }}
    >
      <div ref={scrollerRef} className="mn-kanban-board__columns">
        {shown.map((column) => {
          const titleId = `${baseId}-${column.id}-title`;
          const listId = `${baseId}-${column.id}-list`;
          const isCollapsed = collapsed.has(column.id);
          const count = column.items.length;
          const limit = column.wipLimit;
          const wip =
            limit === undefined
              ? undefined
              : count > limit
                ? 'over'
                : count === limit
                  ? 'at'
                  : undefined;
          const blocked = drag ? isBlocked(drag, column.id) : false;
          const isOver =
            drag?.mode === 'pointer'
              ? over?.columnId === column.id
              : drag
                ? locate(shown, drag.key, getItemId)?.columnId === column.id
                : false;
          return (
            <div
              key={column.id}
              role="group"
              ref={(node) => {
                if (node) columnNodes.current.set(column.id, node);
                else columnNodes.current.delete(column.id);
              }}
              className="mn-kanban-board__column"
              aria-labelledby={titleId}
              data-column-id={column.id}
              data-collapsed={isCollapsed || undefined}
              data-wip={wip}
              data-drop={drag ? (blocked ? 'blocked' : 'allowed') : undefined}
              data-over={isOver || undefined}
            >
              <div className="mn-kanban-board__header">
                {collapsible ? (
                  <button
                    ref={(node) => {
                      if (node) toggleNodes.current.set(column.id, node);
                      else toggleNodes.current.delete(column.id);
                    }}
                    type="button"
                    className="mn-kanban-board__toggle"
                    aria-expanded={!isCollapsed}
                    aria-controls={listId}
                    aria-label={
                      isCollapsed ? messages.expand(column.title) : messages.collapse(column.title)
                    }
                    onClick={() => toggleColumn(column.id)}
                  >
                    <ChevronDownGlyph />
                  </button>
                ) : null}
                <Heading id={titleId} className="mn-kanban-board__title">
                  {column.icon ? (
                    <span className="mn-kanban-board__icon" aria-hidden>
                      {column.icon}
                    </span>
                  ) : null}
                  <span className="mn-kanban-board__title-text">{column.title}</span>
                </Heading>
                <span className="mn-kanban-board__count" data-wip={wip}>
                  {wip === 'over' ? <WarningGlyph /> : null}
                  <span aria-hidden>
                    {count}
                    {limit !== undefined ? ` / ${limit}` : ''}
                  </span>
                  <span className="mn-visually-hidden">{messages.count(count, limit)}</span>
                </span>
                {column.actions && !isCollapsed ? (
                  <span className="mn-kanban-board__actions">{column.actions}</span>
                ) : null}
              </div>
              {drag && blocked && !isCollapsed ? (
                <p className="mn-kanban-board__blocked">{messages.blocked}</p>
              ) : null}
              <ul
                id={listId}
                ref={(node) => {
                  if (node) listNodes.current.set(column.id, node);
                  else listNodes.current.delete(column.id);
                }}
                className="mn-kanban-board__list"
                aria-labelledby={titleId}
                hidden={isCollapsed}
              >
                {column.items.map((item, index) => {
                  const key = getItemId(item);
                  const label = getItemLabel(item);
                  const locked = isLocked(item);
                  const dragging = drag?.key === key;
                  const isPending = pendingKeys.has(key);
                  const state: KanbanCardState = {
                    columnId: column.id,
                    index,
                    dragging,
                    overlay: false,
                    pending: isPending,
                    disabled: locked,
                  };
                  return (
                    <li
                      key={key}
                      ref={(node) => {
                        if (node) cardNodes.current.set(key, node);
                        else cardNodes.current.delete(key);
                      }}
                      className="mn-kanban-board__card"
                      data-card-id={key}
                      data-dragging={dragging ? drag?.mode : undefined}
                      data-pending={isPending || undefined}
                      data-disabled={locked || undefined}
                      data-drag-from={dragFrom}
                      aria-busy={isPending || undefined}
                      onPointerDown={(e) => onCardPointerDown(e, item)}
                    >
                      <button
                        ref={(node) => {
                          if (node) handleNodes.current.set(key, node);
                          else handleNodes.current.delete(key);
                        }}
                        type="button"
                        className="mn-kanban-board__handle"
                        aria-label={messages.handleLabel(label)}
                        aria-describedby={instructionsId}
                        aria-pressed={dragging && drag?.mode === 'keyboard'}
                        disabled={locked}
                        onKeyDown={(e) => onHandleKeyDown(e, item)}
                        onBlur={(e) => {
                          // Focus leaving the board during a keyboard move cancels it.
                          const next = e.relatedTarget as Node | null;
                          if (
                            dragRef.current?.mode === 'keyboard' &&
                            next &&
                            !rootRef.current?.contains(next)
                          )
                            finish('cancel');
                        }}
                      >
                        <GripGlyph />
                      </button>
                      <div className="mn-kanban-board__content">{renderCard(item, state)}</div>
                      {moveMenu && !locked ? (
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <button
                              type="button"
                              className="mn-kanban-board__menu"
                              aria-label={messages.moveMenuLabel(label)}
                              disabled={drag !== null}
                            >
                              <MoreGlyph />
                            </button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent
                            align="end"
                            onCloseAutoFocus={(e) => {
                              // The card may have moved; focus follows it instead.
                              if (focusAfter.current === key) e.preventDefault();
                            }}
                          >
                            <DropdownMenuLabel>{messages.moveMenuLabel(label)}</DropdownMenuLabel>
                            {shown
                              .filter((c) => c.id !== column.id)
                              .map((c) => (
                                <DropdownMenuItem
                                  key={c.id}
                                  disabled={isBlocked(
                                    { key, item, from: { columnId: column.id, index } },
                                    c.id,
                                  )}
                                  onSelect={() => moveByMenu(item, c.id)}
                                >
                                  {messages.moveMenuItem(c.title)}
                                </DropdownMenuItem>
                              ))}
                          </DropdownMenuContent>
                        </DropdownMenu>
                      ) : null}
                    </li>
                  );
                })}
              </ul>
              {!isCollapsed && count === 0 ? (
                <div className="mn-kanban-board__empty">{column.empty ?? messages.empty}</div>
              ) : null}
              {!isCollapsed && column.footer ? (
                <div className="mn-kanban-board__footer">{column.footer}</div>
              ) : null}
            </div>
          );
        })}
      </div>
      {drag?.mode === 'pointer' && dragItem !== undefined ? (
        <div
          ref={overlayRef}
          className="mn-kanban-board__overlay"
          style={{ width: drag.width }}
          aria-hidden
          {...inertProps()}
        >
          <div className="mn-kanban-board__card" data-overlay="">
            <span className="mn-kanban-board__handle" data-overlay="">
              <GripGlyph />
            </span>
            <div className="mn-kanban-board__content">
              {renderCard(dragItem, {
                columnId: over?.columnId ?? drag.from.columnId,
                index: locate(shown, drag.key, getItemId)?.index ?? drag.from.index,
                dragging: true,
                overlay: true,
                pending: false,
                disabled: false,
              })}
            </div>
          </div>
        </div>
      ) : null}
      <span id={instructionsId} className="mn-visually-hidden">
        {messages.instructions}
      </span>
      <span className="mn-visually-hidden" aria-live="assertive" aria-atomic="true">
        {announcement}
      </span>
    </div>
  );
}

/**
 * A board of columns whose cards can be dragged between and within columns
 * with a mouse, pen or touch, or moved from the keyboard: Space picks a card
 * up, the arrow keys move it, Space drops it and Escape cancels. Every step is
 * announced. Columns show counts and WIP limits and can collapse. The product
 * renders the cards and persists moves through `onMove` (optimistic, with
 * rollback when its promise rejects).
 */
export const KanbanBoard = forwardRef(KanbanBoardInner) as <T>(
  props: KanbanBoardProps<T> & { ref?: ForwardedRef<HTMLDivElement> },
) => ReactElement | null;
