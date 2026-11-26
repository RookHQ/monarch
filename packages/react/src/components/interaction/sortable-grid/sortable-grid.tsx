'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type CSSProperties,
  type ForwardedRef,
  type HTMLAttributes,
  type KeyboardEvent,
  type PointerEvent as ReactPointerEvent,
  type ReactElement,
  type ReactNode,
} from 'react';
import { useIsomorphicLayoutEffect } from '../../../internal/layout-effect.js';
import { cx } from '../../../internal/cx.js';
import { useComposedRef } from '../../../internal/compose-refs.js';
import { moveItem } from '../reorderable-list/index.js';
import { DragHandle } from '../drag-handle/index.js';
import { LiveRegion } from '../live-region/index.js';
import { useDirection } from '../../../internal/direction.js';

/** Root class, declared before any other `mn-` string so dist/manifest.json reads it as the root. */
const ROOT = 'mn-sortable-grid';

/** Announcements and labels. Override them to translate. */
export interface SortableGridMessages {
  /** Accessible name of an item's drag handle. */
  handleLabel: (label: string) => string;
  /** Read once by every handle (aria-describedby). */
  instructions: string;
  pickedUp: (label: string, position: number, total: number) => string;
  moved: (label: string, position: number, total: number) => string;
  dropped: (label: string, position: number, total: number) => string;
  cancelled: (label: string, position: number, total: number) => string;
}

const DEFAULT_MESSAGES: SortableGridMessages = {
  handleLabel: (label) => `Reorder ${label}`,
  instructions:
    'Press Space or Enter to pick up. Use the arrow keys to move left, right, up or down a row, Space or Enter to drop, Escape to cancel.',
  pickedUp: (label, p, n) => `Picked up ${label}. Position ${p} of ${n}.`,
  moved: (label, p, n) => `${label} moved to position ${p} of ${n}.`,
  dropped: (label, p, n) => `${label} dropped at position ${p} of ${n}.`,
  cancelled: (label, p, n) => `Reorder cancelled. ${label} returned to position ${p} of ${n}.`,
};

/** What `renderItem` knows about the item it draws. */
export interface SortableGridItemState {
  /** Position as currently shown (zero-based), including a move in progress. */
  index: number;
  /** This item is being moved, by pointer or keyboard. */
  active: boolean;
  /** This item cannot be moved. */
  disabled: boolean;
}

/** Details of a finished move, passed to `onReorder`. */
export interface SortableGridMove<T> {
  item: T;
  from: number;
  to: number;
}

export interface SortableGridProps<T> extends Omit<
  HTMLAttributes<HTMLUListElement>,
  'children' | 'onChange' | 'defaultValue'
> {
  /** The items in order (controlled). Update it from `onReorder`. */
  items?: readonly T[] | undefined;
  /** The starting order when uncontrolled. */
  defaultItems?: readonly T[] | undefined;
  /** Called once per finished move with the new order. */
  onReorder?: ((items: T[], move: SortableGridMove<T>) => void) | undefined;
  /** A stable, unique key for each item. */
  getKey: (item: T) => string;
  /** Plain-text name of each item, used in labels and announcements. */
  getLabel: (item: T) => string;
  /** Draws the tile's content; the drag handle is added beside it. */
  renderItem: (item: T, state: SortableGridItemState) => ReactNode;
  /** An item that cannot be moved. Other items can still move past it. */
  isItemDisabled?: ((item: T) => boolean) | undefined;
  /** No item can be moved. */
  disabled?: boolean | undefined;
  /** Fixed number of columns. Without it, as many columns as fit `minItemWidth`. */
  columns?: number | undefined;
  /** Minimum tile width when `columns` is not set: a CSS length or px. Default `12rem`. */
  minItemWidth?: string | number | undefined;
  /** Override labels and announcements. */
  messages?: Partial<SortableGridMessages> | undefined;
}

interface Drag {
  key: string;
  from: number;
  mode: 'pointer' | 'keyboard';
  pointerId?: number;
  startX?: number;
  startY?: number;
  dx?: number;
  dy?: number;
  /** Centre of every tile at drag start, by key (pointer mode). */
  centres?: Map<string, { x: number; y: number }>;
  /** Layout position of the dragged tile at drag start. */
  origin?: { left: number; top: number };
}

const NBSP = String.fromCharCode(160);

const DRAG_THRESHOLD = 4;

function reducedMotion() {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );
}

function SortableGridInner<T>(
  {
    items: itemsProp,
    defaultItems,
    onReorder,
    getKey,
    getLabel,
    renderItem,
    isItemDisabled,
    disabled = false,
    columns,
    minItemWidth = '12rem',
    messages: messagesProp,
    className,
    style,
    ...rest
  }: SortableGridProps<T>,
  ref: ForwardedRef<HTMLUListElement>,
) {
  const readDir = useDirection();
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const id = useId();
  const instructionsId = `mn-sortable-grid-${id}-instructions`;
  const [internal, setInternal] = useState<readonly T[]>(defaultItems ?? []);
  const items = itemsProp ?? internal;
  const [drag, setDrag] = useState<Drag | null>(null);
  const dragRef = useRef<Drag | null>(null);
  dragRef.current = drag;
  const [preview, setPreview] = useState<readonly T[] | null>(null);
  const shown = preview ?? items;
  const [announcement, setAnnouncement] = useState('');
  const listRef = useRef<HTMLUListElement | null>(null);
  const setListRef = useComposedRef(listRef, ref);
  const nodes = useRef(new Map<string, HTMLLIElement>());
  const handles = useRef(new Map<string, HTMLButtonElement>());
  const lastPos = useRef(new Map<string, { left: number; top: number }>());
  const focusAfter = useRef<string | null>(null);

  const announce = useCallback((text: string) => {
    setAnnouncement((prev) => (prev === text ? `${text}${NBSP}` : text));
  }, []);

  const commit = useCallback(
    (next: T[], move: SortableGridMove<T>) => {
      if (itemsProp === undefined) setInternal(next);
      onReorder?.(next, move);
    },
    [itemsProp, onReorder],
  );

  const indexOfKey = (list: readonly T[], key: string) => list.findIndex((i) => getKey(i) === key);
  const isLocked = (item: T) => disabled || (isItemDisabled?.(item) ?? false);

  // FLIP: tiles that change place glide to it instead of jumping.
  useIsomorphicLayoutEffect(() => {
    const animate = !reducedMotion();
    const current = dragRef.current;
    for (const key of lastPos.current.keys())
      if (!nodes.current.has(key)) lastPos.current.delete(key);
    for (const [key, node] of nodes.current) {
      const pos = { left: node.offsetLeft, top: node.offsetTop };
      const before = lastPos.current.get(key);
      lastPos.current.set(key, pos);
      if (!animate || !before || (before.left === pos.left && before.top === pos.top)) continue;
      if (current?.mode === 'pointer' && current.key === key) continue;
      node.style.transition = 'none';
      node.style.transform = `translate(${before.left - pos.left}px, ${before.top - pos.top}px)`;
      void node.offsetHeight;
      node.style.transition = '';
      node.style.transform = '';
    }
    if (current?.mode === 'pointer' && current.origin) {
      const node = nodes.current.get(current.key);
      if (node)
        node.style.transform = `translate(${current.origin.left + (current.dx ?? 0) - node.offsetLeft}px, ${current.origin.top + (current.dy ?? 0) - node.offsetTop}px)`;
    }
    if (focusAfter.current) {
      const el = handles.current.get(focusAfter.current);
      focusAfter.current = null;
      if (el && document.activeElement !== el) el.focus();
    }
  });

  const endDrag = () => {
    const d = dragRef.current;
    if (d) {
      const node = nodes.current.get(d.key);
      if (node) node.style.transform = '';
    }
    dragRef.current = null;
    setDrag(null);
    setPreview(null);
  };

  const finish = (cancel: boolean) => {
    const d = dragRef.current;
    if (!d) return;
    const list = preview ?? items;
    const to = indexOfKey(list, d.key);
    const item = items[d.from];
    if (item === undefined) return endDrag();
    const label = getLabel(item);
    if (cancel || to === d.from || to === -1) {
      announce(
        cancel
          ? messages.cancelled(label, d.from + 1, items.length)
          : messages.dropped(label, d.from + 1, items.length),
      );
    } else {
      announce(messages.dropped(label, to + 1, items.length));
      commit(list.slice(), { item, from: d.from, to });
    }
    if (d.mode === 'keyboard') focusAfter.current = d.key;
    else if (d.origin)
      lastPos.current.set(d.key, {
        left: d.origin.left + (d.dx ?? 0),
        top: d.origin.top + (d.dy ?? 0),
      });
    endDrag();
  };

  // The tiles changed under a drag (someone else added, removed or moved one):
  // carry the held tile over to the new list at the place it is held, so the
  // drop commits the current tiles, not the ones from when it was picked up.
  const dragBase = useRef<readonly T[]>(items);
  useEffect(() => {
    const base = dragBase.current;
    dragBase.current = items;
    if (!drag) return;
    const from = indexOfKey(items, drag.key);
    if (from === -1) return endDrag();
    if (
      base.length === items.length &&
      base.every((item, i) => getKey(item) === getKey(items[i] as T))
    )
      return;
    const held = preview ? indexOfKey(preview, drag.key) : from;
    const d = { ...drag, from };
    dragRef.current = d;
    setDrag(d);
    setPreview(moveItem(items, from, held === -1 ? from : held));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items]);

  /** Tiles per row: the `columns` prop, or counted from the layout. */
  const perRow = (list: readonly T[]) => {
    if (columns && columns > 0) return Math.floor(columns);
    const first = list[0];
    const firstNode = first === undefined ? undefined : nodes.current.get(getKey(first));
    if (!firstNode) return Math.max(1, list.length);
    let n = 0;
    for (const item of list) {
      if (nodes.current.get(getKey(item))?.offsetTop !== firstNode.offsetTop) break;
      n++;
    }
    return Math.max(1, n);
  };

  const startKeyboard = (item: T) => {
    const key = getKey(item);
    const from = indexOfKey(items, key);
    const d: Drag = { key, from, mode: 'keyboard' };
    dragRef.current = d;
    setDrag(d);
    setPreview(items);
    announce(messages.pickedUp(getLabel(item), from + 1, items.length));
  };

  const moveKeyboard = (to: number) => {
    const d = dragRef.current;
    if (!d) return;
    const list = preview ?? items;
    const from = indexOfKey(list, d.key);
    const item = list[from];
    if (item === undefined || to < 0 || to > list.length - 1) return;
    if (to !== from) {
      setPreview(moveItem(list, from, to));
      focusAfter.current = d.key;
    }
    announce(messages.moved(getLabel(item), to + 1, list.length));
  };

  const onHandleKeyDown = (e: KeyboardEvent<HTMLButtonElement>, item: T) => {
    const d = dragRef.current;
    const key = getKey(item);
    const grabbed = d?.mode === 'keyboard' && d.key === key;
    const list = preview ?? items;
    const index = indexOfKey(list, key);
    const rtl = readDir(listRef.current) === 'rtl';
    const step = (delta: number) => {
      if (!grabbed) return;
      e.preventDefault();
      moveKeyboard(index + delta);
    };
    switch (e.key) {
      case ' ':
      case 'Enter':
        e.preventDefault();
        if (e.repeat) return;
        if (grabbed) finish(false);
        else if (!d) startKeyboard(item);
        break;
      case 'ArrowLeft':
        step(rtl ? 1 : -1);
        break;
      case 'ArrowRight':
        step(rtl ? -1 : 1);
        break;
      case 'ArrowUp':
        step(-perRow(list));
        break;
      case 'ArrowDown':
        step(perRow(list));
        break;
      case 'Home':
        step(-index);
        break;
      case 'End':
        step(list.length - 1 - index);
        break;
      case 'Escape':
        if (d) {
          e.preventDefault();
          e.stopPropagation();
          finish(true);
        }
        break;
      case 'Tab':
        if (grabbed) finish(true);
        break;
    }
  };

  const pending = useRef<{ item: T; pointerId: number; x: number; y: number } | null>(null);

  const startPointer = (item: T, pointerId: number, x: number, y: number) => {
    const key = getKey(item);
    const node = nodes.current.get(key);
    if (!node) return;
    const centres = new Map<string, { x: number; y: number }>();
    for (const [k, n] of nodes.current)
      centres.set(k, { x: n.offsetLeft + n.offsetWidth / 2, y: n.offsetTop + n.offsetHeight / 2 });
    const d: Drag = {
      key,
      from: indexOfKey(items, key),
      mode: 'pointer',
      pointerId,
      startX: x,
      startY: y,
      dx: 0,
      dy: 0,
      centres,
      origin: { left: node.offsetLeft, top: node.offsetTop },
    };
    dragRef.current = d;
    setDrag(d);
    setPreview(items);
    announce(messages.pickedUp(getLabel(item), d.from + 1, items.length));
  };

  const onPointerDown = (e: ReactPointerEvent<HTMLButtonElement>, item: T) => {
    if (dragRef.current || e.button !== 0) return;
    e.preventDefault();
    e.currentTarget.setPointerCapture?.(e.pointerId);
    e.currentTarget.focus({ preventScroll: true });
    pending.current = { item, pointerId: e.pointerId, x: e.clientX, y: e.clientY };
  };

  const onPointerMove = (e: ReactPointerEvent<HTMLButtonElement>) => {
    const p = pending.current;
    if (p && p.pointerId === e.pointerId) {
      if (Math.hypot(e.clientX - p.x, e.clientY - p.y) < DRAG_THRESHOLD) return;
      pending.current = null;
      startPointer(p.item, p.pointerId, p.x, p.y);
    }
    const d = dragRef.current;
    if (!d || d.mode !== 'pointer' || d.pointerId !== e.pointerId || !d.centres) return;
    d.dx = e.clientX - (d.startX ?? 0);
    d.dy = e.clientY - (d.startY ?? 0);
    const own = d.centres.get(d.key) ?? { x: 0, y: 0 };
    const at = { x: own.x + d.dx, y: own.y + d.dy };
    // The new slot: the original tile position whose centre is nearest the dragged centre.
    let to = 0;
    let best = Infinity;
    items.forEach((item, i) => {
      const c = d.centres?.get(getKey(item));
      if (!c) return;
      const dist = Math.hypot(c.x - at.x, c.y - at.y);
      if (dist < best) {
        best = dist;
        to = i;
      }
    });
    const list = preview ?? items;
    const from = indexOfKey(list, d.key);
    const node = nodes.current.get(d.key);
    if (to !== from) {
      setPreview(moveItem(list, from, to));
      const item = list[from];
      if (item !== undefined) announce(messages.moved(getLabel(item), to + 1, list.length));
    } else if (node && d.origin) {
      node.style.transform = `translate(${d.origin.left + d.dx - node.offsetLeft}px, ${d.origin.top + d.dy - node.offsetTop}px)`;
    }
  };

  const onPointerEnd = (e: ReactPointerEvent<HTMLButtonElement>, cancel: boolean) => {
    if (pending.current?.pointerId === e.pointerId) pending.current = null;
    const d = dragRef.current;
    if (!d || d.mode !== 'pointer' || d.pointerId !== e.pointerId) return;
    finish(cancel);
  };

  const gridStyle: Record<string, string | number> = columns
    ? { '--mn-sortable-grid-columns': Math.max(1, Math.floor(columns)) }
    : {
        '--mn-sortable-grid-min':
          typeof minItemWidth === 'number' ? `${minItemWidth}px` : minItemWidth,
      };

  return (
    <div className={ROOT} data-dragging={drag ? drag.mode : undefined}>
      <ul
        ref={setListRef}
        {...rest}
        className={cx('mn-sortable-grid__list', className)}
        style={{ ...gridStyle, ...style } as CSSProperties}
        data-layout={columns ? 'columns' : 'auto'}
      >
        {shown.map((item, index) => {
          const key = getKey(item);
          const label = getLabel(item);
          const locked = isLocked(item);
          const active = drag?.key === key;
          return (
            <li
              key={key}
              ref={(node) => {
                if (node) nodes.current.set(key, node);
                else {
                  // Not the remembered position: this callback is a new function
                  // on every render, so React calls it with null each time, and
                  // forgetting the position here would stop rows from gliding.
                  nodes.current.delete(key);
                  handles.current.delete(key);
                }
              }}
              className="mn-sortable-grid__item"
              data-active={active ? drag?.mode : undefined}
              data-disabled={locked || undefined}
            >
              <DragHandle
                ref={(node) => {
                  if (node) handles.current.set(key, node);
                }}
                className="mn-sortable-grid__handle"
                label={messages.handleLabel(label)}
                axis="both"
                size="sm"
                describe={false}
                aria-describedby={instructionsId}
                state={active && drag?.mode === 'keyboard' ? 'grabbed' : 'idle'}
                disabled={locked}
                onKeyDown={(e) => onHandleKeyDown(e, item)}
                onBlur={(e) => {
                  const next = e.relatedTarget as Node | null;
                  if (
                    dragRef.current?.mode === 'keyboard' &&
                    next &&
                    !listRef.current?.contains(next)
                  )
                    finish(true);
                }}
                onPointerDown={(e) => onPointerDown(e, item)}
                onPointerMove={onPointerMove}
                onPointerUp={(e) => onPointerEnd(e, false)}
                onPointerCancel={(e) => onPointerEnd(e, true)}
                onLostPointerCapture={(e) => onPointerEnd(e, false)}
              />
              <div className="mn-sortable-grid__content">
                {renderItem(item, { index, active, disabled: locked })}
              </div>
            </li>
          );
        })}
      </ul>
      <span id={instructionsId} className="mn-visually-hidden">
        {messages.instructions}
      </span>
      <LiveRegion politeness="assertive">{announcement}</LiveRegion>
    </div>
  );
}

/**
 * A grid of tiles the user reorders by dragging a handle (mouse, pen or
 * touch) or from the keyboard: Space picks a tile up, Left/Right move it one
 * place, Up/Down a row, Space drops and Escape cancels. Every step is announced.
 */
export const SortableGrid = forwardRef(SortableGridInner) as <T>(
  props: SortableGridProps<T> & { ref?: ForwardedRef<HTMLUListElement> },
) => ReactElement | null;
