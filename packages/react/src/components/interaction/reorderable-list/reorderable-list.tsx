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
import { ChevronDownGlyph } from '../../../internal/icons.js';

/** Returns a copy of `list` with the item at `from` moved to `to`. */
export function moveItem<T>(list: readonly T[], from: number, to: number): T[] {
  const next = list.slice();
  if (from < 0 || from >= next.length) return next;
  const target = Math.max(0, Math.min(to, next.length - 1));
  const [item] = next.splice(from, 1);
  next.splice(target, 0, item as T);
  return next;
}

/** Announcements and labels. Override them to translate. */
export interface ReorderableListMessages {
  /** Accessible name of an item's drag handle. */
  handleLabel: (label: string) => string;
  /** Read once by every handle (aria-describedby). */
  instructions: string;
  /** Accessible names of the optional move buttons. */
  moveUp: (label: string) => string;
  moveDown: (label: string) => string;
  pickedUp: (label: string, position: number, total: number) => string;
  moved: (label: string, position: number, total: number) => string;
  dropped: (label: string, position: number, total: number) => string;
  cancelled: (label: string, position: number, total: number) => string;
}

const DEFAULT_MESSAGES: ReorderableListMessages = {
  handleLabel: (label) => `Reorder ${label}`,
  instructions:
    'Press Space or Enter to pick up. Use the arrow keys to move, Space or Enter to drop, Escape to cancel.',
  moveUp: (label) => `Move ${label} up`,
  moveDown: (label) => `Move ${label} down`,
  pickedUp: (label, p, n) => `Picked up ${label}. Position ${p} of ${n}.`,
  moved: (label, p, n) => `${label} moved to position ${p} of ${n}.`,
  dropped: (label, p, n) => `${label} dropped at position ${p} of ${n}.`,
  cancelled: (label, p, n) => `Reorder cancelled. ${label} returned to position ${p} of ${n}.`,
};

/** What `renderItem` knows about the item it draws. */
export interface ReorderableListItemState {
  /** Position in the list as currently shown (zero-based), including a move in progress. */
  index: number;
  /** This item is being moved, by pointer or keyboard. */
  active: boolean;
  /** This item cannot be moved (`isItemDisabled`). */
  disabled: boolean;
}

/** Details of a finished move, passed to `onReorder`. */
export interface ReorderableListMove<T> {
  item: T;
  from: number;
  to: number;
}

export interface ReorderableListProps<T> extends Omit<
  HTMLAttributes<HTMLUListElement>,
  'children' | 'onChange' | 'defaultValue'
> {
  /** The items in order (controlled). Update it from `onReorder`. */
  items?: readonly T[];
  /** The starting order when uncontrolled. */
  defaultItems?: readonly T[];
  /** Called once per finished move (drop, Space, or a move button) with the new order. */
  onReorder?: (items: T[], move: ReorderableListMove<T>) => void;
  /** A stable, unique key for each item. */
  getKey: (item: T) => string;
  /** Plain-text name of each item, used in labels and announcements. */
  getLabel: (item: T) => string;
  /** Draws the row's content; the handle and move buttons are added around it. */
  renderItem: (item: T, state: ReorderableListItemState) => ReactNode;
  /** An item that cannot be moved. Other items can still move past it. */
  isItemDisabled?: (item: T) => boolean;
  /** No item can be moved: handles and buttons are disabled. */
  disabled?: boolean;
  /**
   * Also show Move up / Move down buttons on each row: a single-click
   * alternative for people who cannot drag or hold keys.
   */
  moveButtons?: boolean;
  /** Override labels and announcements. */
  messages?: Partial<ReorderableListMessages>;
}

interface Drag {
  key: string;
  from: number;
  mode: 'pointer' | 'keyboard';
  pointerId?: number;
  startY?: number;
  dy?: number;
  /** Centre of every item at drag start, by key (pointer mode). */
  centres?: Map<string, number>;
  /** Layout top of the dragged item at drag start. */
  originTop?: number;
}

const NBSP = String.fromCharCode(160);

/** Pixels a pointer must travel before a press becomes a drag. */
const DRAG_THRESHOLD = 4;

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

function reducedMotion() {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );
}

function ReorderableListInner<T>(
  {
    items: itemsProp,
    defaultItems,
    onReorder,
    getKey,
    getLabel,
    renderItem,
    isItemDisabled,
    disabled = false,
    moveButtons = false,
    messages: messagesProp,
    className,
    ...rest
  }: ReorderableListProps<T>,
  ref: ForwardedRef<HTMLUListElement>,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const id = useId();
  const instructionsId = `mn-reorderable-${id}-instructions`;
  const [internal, setInternal] = useState<readonly T[]>(defaultItems ?? []);
  const items = itemsProp ?? internal;
  const [drag, setDrag] = useState<Drag | null>(null);
  const dragRef = useRef<Drag | null>(null);
  dragRef.current = drag;
  /** The order shown while a move is in progress. */
  const [preview, setPreview] = useState<readonly T[] | null>(null);
  const shown = preview ?? items;
  const [announcement, setAnnouncement] = useState('');
  const listRef = useRef<HTMLUListElement | null>(null);
  const nodes = useRef(new Map<string, HTMLLIElement>());
  const handles = useRef(new Map<string, HTMLButtonElement>());
  const lastTops = useRef(new Map<string, number>());
  const focusAfter = useRef<{ key: string; target: 'handle' | 'up' | 'down' } | null>(null);

  const announce = useCallback((text: string) => {
    // Re-announce identical text by clearing first.
    setAnnouncement((prev) => (prev === text ? `${text}${NBSP}` : text));
  }, []);

  const commit = useCallback(
    (next: T[], move: ReorderableListMove<T>) => {
      if (itemsProp === undefined) setInternal(next);
      onReorder?.(next, move);
    },
    [itemsProp, onReorder],
  );

  const indexOfKey = (list: readonly T[], key: string) => list.findIndex((i) => getKey(i) === key);
  const isLocked = (item: T) => disabled || (isItemDisabled?.(item) ?? false);

  // FLIP: rows that change place glide to it instead of jumping.
  useLayoutEffect(() => {
    const animate = !reducedMotion();
    const current = dragRef.current;
    for (const [key, node] of nodes.current) {
      const top = node.offsetTop;
      const before = lastTops.current.get(key);
      lastTops.current.set(key, top);
      if (!animate || before === undefined || before === top) continue;
      if (current?.mode === 'pointer' && current.key === key) continue;
      node.style.transition = 'none';
      node.style.transform = `translateY(${before - top}px)`;
      void node.offsetHeight;
      node.style.transition = '';
      node.style.transform = '';
    }
    // The dragged row follows the pointer from wherever the layout put it.
    if (current?.mode === 'pointer') {
      const node = nodes.current.get(current.key);
      if (node && current.originTop !== undefined)
        node.style.transform = `translateY(${current.originTop + (current.dy ?? 0) - node.offsetTop}px)`;
    }
    if (focusAfter.current) {
      const { key, target } = focusAfter.current;
      focusAfter.current = null;
      const row = nodes.current.get(key);
      const el =
        target === 'handle'
          ? handles.current.get(key)
          : (row?.querySelector<HTMLButtonElement>(`[data-move='${target}']:not(:disabled)`) ??
            handles.current.get(key));
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
    if (d.mode === 'keyboard') focusAfter.current = { key: d.key, target: 'handle' };
    else if (d.originTop !== undefined) lastTops.current.set(d.key, d.originTop + (d.dy ?? 0));
    endDrag();
  };

  // A move abandoned by losing the item (items changed under us) ends quietly.
  useEffect(() => {
    if (drag && indexOfKey(items, drag.key) === -1) endDrag();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items]);

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
    const target = Math.max(0, Math.min(to, list.length - 1));
    const item = list[from];
    if (item === undefined) return;
    if (target !== from) {
      setPreview(moveItem(list, from, target));
      focusAfter.current = { key: d.key, target: 'handle' };
    }
    announce(messages.moved(getLabel(item), target + 1, list.length));
  };

  const onHandleKeyDown = (e: KeyboardEvent<HTMLButtonElement>, item: T) => {
    const d = dragRef.current;
    const key = getKey(item);
    const grabbed = d?.mode === 'keyboard' && d.key === key;
    const list = preview ?? items;
    const index = indexOfKey(list, key);
    switch (e.key) {
      case ' ':
      case 'Enter':
        e.preventDefault();
        if (e.repeat) return;
        if (grabbed) finish(false);
        else if (!d) startKeyboard(item);
        break;
      case 'ArrowUp':
      case 'ArrowLeft':
        if (grabbed) {
          e.preventDefault();
          moveKeyboard(index - 1);
        }
        break;
      case 'ArrowDown':
      case 'ArrowRight':
        if (grabbed) {
          e.preventDefault();
          moveKeyboard(index + 1);
        }
        break;
      case 'Home':
        if (grabbed) {
          e.preventDefault();
          moveKeyboard(0);
        }
        break;
      case 'End':
        if (grabbed) {
          e.preventDefault();
          moveKeyboard(list.length - 1);
        }
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

  /** A press on a handle that has not moved far enough to be a drag yet. */
  const pending = useRef<{ item: T; pointerId: number; x: number; y: number } | null>(null);

  const startPointer = (item: T, pointerId: number, startY: number) => {
    const key = getKey(item);
    const node = nodes.current.get(key);
    if (!node) return;
    const centres = new Map<string, number>();
    for (const [k, n] of nodes.current) centres.set(k, n.offsetTop + n.offsetHeight / 2);
    const d: Drag = {
      key,
      from: indexOfKey(items, key),
      mode: 'pointer',
      pointerId,
      startY,
      dy: 0,
      centres,
      originTop: node.offsetTop,
    };
    dragRef.current = d;
    setDrag(d);
    setPreview(items);
    announce(messages.pickedUp(getLabel(item), d.from + 1, items.length));
  };

  const onPointerDown = (e: ReactPointerEvent<HTMLButtonElement>, item: T) => {
    if (dragRef.current || e.button !== 0) return;
    // Keep the page from scrolling or selecting text; focus the handle ourselves.
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
      startPointer(p.item, p.pointerId, p.y);
    }
    const d = dragRef.current;
    if (!d || d.mode !== 'pointer' || d.pointerId !== e.pointerId) return;
    const dy = e.clientY - (d.startY ?? 0);
    d.dy = dy;
    const node = nodes.current.get(d.key);
    const centre = (d.centres?.get(d.key) ?? 0) + dy;
    // The new slot: how many other rows' original centres lie above the dragged centre.
    let to = 0;
    for (const item of items) {
      const k = getKey(item);
      if (k !== d.key && (d.centres?.get(k) ?? 0) < centre) to++;
    }
    const list = preview ?? items;
    const from = indexOfKey(list, d.key);
    if (to !== from) {
      setPreview(moveItem(list, from, to));
      const item = list[from];
      if (item !== undefined) announce(messages.moved(getLabel(item), to + 1, list.length));
    } else if (node && d.originTop !== undefined) {
      node.style.transform = `translateY(${d.originTop + dy - node.offsetTop}px)`;
    }
  };

  const onPointerEnd = (e: ReactPointerEvent<HTMLButtonElement>, cancel: boolean) => {
    if (pending.current?.pointerId === e.pointerId) pending.current = null;
    const d = dragRef.current;
    if (!d || d.mode !== 'pointer' || d.pointerId !== e.pointerId) return;
    finish(cancel);
  };

  const moveByButton = (item: T, delta: -1 | 1) => {
    const from = indexOfKey(items, getKey(item));
    const to = from + delta;
    if (to < 0 || to >= items.length) return;
    const next = moveItem(items, from, to);
    announce(messages.moved(getLabel(item), to + 1, items.length));
    focusAfter.current = { key: getKey(item), target: delta < 0 ? 'up' : 'down' };
    commit(next, { item, from, to });
  };

  return (
    <div className="mn-reorderable-list" data-dragging={drag ? drag.mode : undefined}>
      <ul
        ref={(node) => {
          listRef.current = node;
          if (typeof ref === 'function') ref(node);
          else if (ref) ref.current = node;
        }}
        {...rest}
        className={cx('mn-reorderable-list__list', className)}
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
                  nodes.current.delete(key);
                  handles.current.delete(key);
                  lastTops.current.delete(key);
                }
              }}
              className="mn-reorderable-list__item"
              data-active={active ? drag?.mode : undefined}
              data-disabled={locked || undefined}
            >
              <button
                ref={(node) => {
                  if (node) handles.current.set(key, node);
                }}
                type="button"
                className="mn-reorderable-list__handle"
                aria-label={messages.handleLabel(label)}
                aria-describedby={instructionsId}
                aria-pressed={active && drag?.mode === 'keyboard'}
                disabled={locked}
                onKeyDown={(e) => onHandleKeyDown(e, item)}
                onBlur={(e) => {
                  // Focus leaving the list during a keyboard move cancels it.
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
              >
                <GripGlyph />
              </button>
              <div className="mn-reorderable-list__content">
                {renderItem(item, { index, active, disabled: locked })}
              </div>
              {moveButtons ? (
                <span className="mn-reorderable-list__moves">
                  <button
                    type="button"
                    className="mn-reorderable-list__move"
                    data-move="up"
                    aria-label={messages.moveUp(label)}
                    disabled={locked || index === 0 || drag !== null}
                    onClick={() => moveByButton(item, -1)}
                  >
                    <ChevronDownGlyph style={{ transform: 'rotate(180deg)' }} />
                  </button>
                  <button
                    type="button"
                    className="mn-reorderable-list__move"
                    data-move="down"
                    aria-label={messages.moveDown(label)}
                    disabled={locked || index === shown.length - 1 || drag !== null}
                    onClick={() => moveByButton(item, 1)}
                  >
                    <ChevronDownGlyph />
                  </button>
                </span>
              ) : null}
            </li>
          );
        })}
      </ul>
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
 * A list the user can reorder by dragging a handle (mouse, pen or touch) or
 * from the keyboard: Space picks an item up, the arrow keys move it, Space
 * drops it and Escape cancels. Every step is announced. Optional move buttons
 * give a one-click alternative.
 */
export const ReorderableList = forwardRef(ReorderableListInner) as <T>(
  props: ReorderableListProps<T> & { ref?: ForwardedRef<HTMLUListElement> },
) => ReactElement | null;
