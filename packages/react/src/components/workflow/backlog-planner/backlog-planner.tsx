'use client';

import {
  forwardRef,
  useEffect,
  useId,
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
import { ChevronRightGlyph, MoreGlyph } from '../../../internal/icons.js';
import { Progress } from '../../data-display/progress-bar/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';

/** One bucket of the planner: an iteration, sprint, release or the backlog. */
export interface BacklogBucket<T> {
  /** Stable id, passed back in moves. */
  id: string;
  /** Heading, such as "Sprint 14" or "Backlog". */
  title: ReactNode;
  /** Plain-text title for announcements and the move menu when `title` is not a string. */
  titleText?: string;
  /** Quiet text after the title, such as the dates. */
  subtitle?: ReactNode;
  /** Badge after the title, such as "Active". */
  badge?: ReactNode;
  /** Buttons at the end of the header, such as Edit or Start. */
  actions?: ReactNode;
  /** Points the bucket can hold. Shows a capacity meter and an over-capacity note. */
  capacity?: number | null | undefined;
  /** The items, in priority order. */
  items: T[];
  /** Shown when the bucket has no items. */
  empty?: ReactNode;
  /** Content under the list, such as a quick-add form. */
  footer?: ReactNode;
}

/** A finished move. */
export interface BacklogMove<T> {
  item: T;
  itemId: string;
  fromBucketId: string;
  fromIndex: number;
  toBucketId: string;
  toIndex: number;
}

/** Visible and announced strings. Override them to translate. */
export interface BacklogPlannerMessages {
  /** Item count, points and unestimated count in each header. */
  summary: (count: number, points: number | null, unestimated: number) => string;
  /** Capacity meter text, such as "18 of 20 points". */
  capacity: (points: number, capacity: number) => string;
  /** Note when a bucket is over capacity. */
  overCapacity: (by: number) => string;
  /** Points shown on a row. */
  points: (points: number) => string;
  /** Shown on a row with no estimate. */
  unestimated: string;
  /** Hidden text on a row with no estimate. */
  notEstimated: string;
  /** Accessible name of the drag handle. */
  handleLabel: (label: string) => string;
  /** Description of the drag handle. */
  instructions: string;
  /** Accessible name of the row menu. */
  moveMenu: (label: string) => string;
  moveTo: string;
  /** Default empty text. */
  empty: string;
  pickedUp: (label: string, bucket: string, position: number, total: number) => string;
  moved: (label: string, bucket: string, position: number, total: number) => string;
  dropped: (label: string, bucket: string, position: number, total: number) => string;
  cancelled: (label: string) => string;
  failed: (label: string) => string;
}

export const DEFAULT_BACKLOG_PLANNER_MESSAGES: BacklogPlannerMessages = {
  summary: (count, points, unestimated) =>
    [
      `${count} ${count === 1 ? 'item' : 'items'}`,
      points === null ? null : `${points} ${points === 1 ? 'point' : 'points'}`,
      unestimated ? `${unestimated} unestimated` : null,
    ]
      .filter(Boolean)
      .join(' · '),
  capacity: (points, capacity) => `${points} of ${capacity} points`,
  overCapacity: (by) => `Planned work is over capacity by ${by} ${by === 1 ? 'point' : 'points'}.`,
  points: (p) => `${p} pt`,
  unestimated: '–',
  notEstimated: 'Not estimated',
  handleLabel: (label) => `Move ${label}`,
  instructions:
    'Press Space or Enter to pick up. Use the up and down arrows to move, across buckets too. Press Space or Enter to drop, or Escape to cancel.',
  moveMenu: (label) => `Move ${label} to…`,
  moveTo: 'Move to',
  empty: 'Nothing here yet.',
  pickedUp: (l, b, p, n) => `Picked up ${l}. ${b}, position ${p} of ${n}.`,
  moved: (l, b, p, n) => `${l} moved to ${b}, position ${p} of ${n}.`,
  dropped: (l, b, p, n) => `${l} dropped in ${b}, position ${p} of ${n}.`,
  cancelled: (l) => `Move cancelled. ${l} returned to where it was.`,
  failed: (l) => `${l} could not be moved. It is back where it was.`,
};

export interface BacklogPlannerProps<T> extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The buckets, top to bottom: usually the active and planned iterations, then the backlog. */
  buckets: BacklogBucket<T>[];
  /** Stable id of an item. */
  getItemId: (item: T) => string;
  /** Short plain-text name of an item, for announcements and labels. */
  getItemLabel: (item: T) => string;
  /** Draws the row content after the drag handle (key, title, pickers). */
  renderItem: (item: T) => ReactNode;
  /** Points of an item; `null` when it has no estimate. Enables totals and capacity. */
  getItemPoints?: ((item: T) => number | null) | undefined;
  /**
   * Called once per finished move with the move and the next buckets. Return a
   * promise to keep the move on screen while it saves; a rejection puts the
   * item back and announces it. Without it, items cannot be moved.
   */
  onMove?: ((move: BacklogMove<T>, next: BacklogBucket<T>[]) => unknown) | undefined;
  /** Whether an item may go to a bucket. Refused buckets are skipped and disabled in the menu. */
  canMove?: ((item: T, toBucketId: string, fromBucketId: string) => boolean) | undefined;
  /** Adds a "Move to…" menu to each row, a single-pointer alternative to dragging. Default `true`. */
  moveMenu?: boolean;
  /** Ids of collapsed buckets (controlled). */
  collapsed?: string[] | undefined;
  /** Ids collapsed at first (uncontrolled). */
  defaultCollapsed?: string[] | undefined;
  onCollapsedChange?: ((collapsed: string[]) => void) | undefined;
  /** Heading level of bucket titles. Default 3. */
  headingLevel?: 2 | 3 | 4;
  /** Override visible and announced strings. */
  messages?: Partial<BacklogPlannerMessages>;
}

interface Location {
  bucketId: string;
  index: number;
}

interface DragState<T> {
  key: string;
  label: string;
  item: T;
  mode: 'keyboard' | 'pointer';
  origin: Location;
  preview: BacklogBucket<T>[];
}

function locate<T>(buckets: BacklogBucket<T>[], key: string, getId: (t: T) => string) {
  for (const b of buckets) {
    const index = b.items.findIndex((i) => getId(i) === key);
    if (index !== -1) return { bucketId: b.id, index, item: b.items[index] as T };
  }
  return null;
}

/** Returns a copy of `buckets` with item `key` at `to`. Unknown items or buckets leave it unchanged. */
export function moveBacklogItem<T>(
  buckets: BacklogBucket<T>[],
  key: string,
  to: { bucketId: string; index: number },
  getItemId: (item: T) => string,
): BacklogBucket<T>[] {
  const from = locate(buckets, key, getItemId);
  if (!from || !buckets.some((b) => b.id === to.bucketId)) return buckets;
  return buckets.map((b) => {
    if (b.id !== from.bucketId && b.id !== to.bucketId) return b;
    const items = b.items.slice();
    if (b.id === from.bucketId) items.splice(from.index, 1);
    if (b.id === to.bucketId)
      items.splice(Math.max(0, Math.min(to.index, items.length)), 0, from.item);
    return { ...b, items };
  });
}

function GripGlyph() {
  return (
    <svg
      viewBox="0 0 16 16"
      width="16"
      height="16"
      fill="currentColor"
      aria-hidden
      focusable={false}
    >
      <circle cx="6" cy="4" r="1.1" />
      <circle cx="10" cy="4" r="1.1" />
      <circle cx="6" cy="8" r="1.1" />
      <circle cx="10" cy="8" r="1.1" />
      <circle cx="6" cy="12" r="1.1" />
      <circle cx="10" cy="12" r="1.1" />
    </svg>
  );
}

function BacklogPlannerInner<T>(
  {
    buckets,
    getItemId,
    getItemLabel,
    renderItem,
    getItemPoints,
    onMove,
    canMove,
    moveMenu = true,
    collapsed: collapsedProp,
    defaultCollapsed = [],
    onCollapsedChange,
    headingLevel = 3,
    messages: messagesProp,
    className,
    ...rest
  }: BacklogPlannerProps<T>,
  ref: ForwardedRef<HTMLDivElement>,
) {
  const messages = { ...DEFAULT_BACKLOG_PLANNER_MESSAGES, ...messagesProp };
  const instructionsId = useId();
  const baseId = useId();
  const Heading = `h${headingLevel}` as const;
  const rootRef = useRef<HTMLDivElement | null>(null);

  const [collapsedState, setCollapsedState] = useState<string[]>(defaultCollapsed);
  const collapsed = new Set(collapsedProp ?? collapsedState);
  const toggleBucket = (id: string) => {
    const next = new Set(collapsed);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    if (collapsedProp === undefined) setCollapsedState([...next]);
    onCollapsedChange?.([...next]);
  };

  // A saved move stays on screen until new buckets arrive or the save fails.
  const [optimistic, setOptimistic] = useState<{
    for: BacklogBucket<T>[];
    next: BacklogBucket<T>[];
  } | null>(null);
  const [pendingKey, setPendingKey] = useState<string | null>(null);
  const [drag, setDrag] = useState<DragState<T> | null>(null);
  const [announcement, setAnnouncement] = useState('');
  const dragRef = useRef<DragState<T> | null>(null);
  dragRef.current = drag;

  const base = optimistic && optimistic.for === buckets ? optimistic.next : buckets;
  const shown = drag?.preview ?? base;
  const editable = onMove !== undefined;

  const titleOf = (b: BacklogBucket<T>) =>
    b.titleText ?? (typeof b.title === 'string' ? b.title : b.id);
  const bucketTitle = (id: string) => {
    const b = shown.find((x) => x.id === id);
    return b ? titleOf(b) : id;
  };
  const allowed = (item: T, to: string, from: string) =>
    to === from || !canMove || canMove(item, to, from);

  async function commit(state: DragState<T>, next: BacklogBucket<T>[]) {
    const to = locate(next, state.key, getItemId);
    if (!to || !onMove) return;
    if (to.bucketId === state.origin.bucketId && to.index === state.origin.index) return;
    const move: BacklogMove<T> = {
      item: state.item,
      itemId: state.key,
      fromBucketId: state.origin.bucketId,
      fromIndex: state.origin.index,
      toBucketId: to.bucketId,
      toIndex: to.index,
    };
    const snapshot = buckets;
    setOptimistic({ for: snapshot, next });
    setPendingKey(state.key);
    try {
      await onMove(move, next);
    } catch {
      setOptimistic(null);
      setAnnouncement(messages.failed(state.label));
    } finally {
      setPendingKey((k) => (k === state.key ? null : k));
    }
  }

  function start(item: T, mode: 'keyboard' | 'pointer') {
    const key = getItemId(item);
    const origin = locate(base, key, getItemId);
    if (!origin) return null;
    const state: DragState<T> = {
      key,
      label: getItemLabel(item),
      item,
      mode,
      origin: { bucketId: origin.bucketId, index: origin.index },
      preview: base,
    };
    setDrag(state);
    const bucket = base.find((b) => b.id === origin.bucketId);
    setAnnouncement(
      messages.pickedUp(
        state.label,
        bucketTitle(origin.bucketId),
        origin.index + 1,
        bucket?.items.length ?? 0,
      ),
    );
    return state;
  }

  function moveTo(state: DragState<T>, to: Location, announce = true) {
    const next = moveBacklogItem(state.preview, state.key, to, getItemId);
    if (next === state.preview) return state;
    const updated = { ...state, preview: next };
    setDrag(updated);
    if (announce) {
      const at = locate(next, state.key, getItemId);
      const total = next.find((b) => b.id === at?.bucketId)?.items.length ?? 0;
      if (at)
        setAnnouncement(messages.moved(state.label, bucketTitle(at.bucketId), at.index + 1, total));
    }
    return updated;
  }

  function drop(state: DragState<T>) {
    setDrag(null);
    const at = locate(state.preview, state.key, getItemId);
    const total = state.preview.find((b) => b.id === at?.bucketId)?.items.length ?? 0;
    if (at)
      setAnnouncement(messages.dropped(state.label, bucketTitle(at.bucketId), at.index + 1, total));
    void commit(state, state.preview);
  }

  function cancel(state: DragState<T>) {
    setDrag(null);
    setAnnouncement(messages.cancelled(state.label));
  }

  /** The next position up or down that accepts the item, crossing bucket edges. */
  function step(state: DragState<T>, dir: 1 | -1): Location | null {
    const at = locate(state.preview, state.key, getItemId);
    if (!at) return null;
    const bucket = state.preview.find((b) => b.id === at.bucketId);
    if (!bucket) return null;
    const within = at.index + dir;
    if (within >= 0 && within < bucket.items.length) return { bucketId: bucket.id, index: within };
    const i = state.preview.findIndex((b) => b.id === bucket.id);
    for (let j = i + dir; j >= 0 && j < state.preview.length; j += dir) {
      const b = state.preview[j];
      if (!b || collapsed.has(b.id) || !allowed(state.item, b.id, state.origin.bucketId)) continue;
      return { bucketId: b.id, index: dir === 1 ? 0 : b.items.length };
    }
    return null;
  }

  function onHandleKeyDown(event: KeyboardEvent<HTMLButtonElement>, item: T) {
    const state = dragRef.current;
    const key = event.key;
    if (!state || state.mode !== 'keyboard') {
      if (key === ' ' || key === 'Enter') {
        event.preventDefault();
        start(item, 'keyboard');
      }
      return;
    }
    if (key === ' ' || key === 'Enter') {
      event.preventDefault();
      drop(state);
    } else if (key === 'Escape') {
      event.preventDefault();
      cancel(state);
    } else if (key === 'Tab') {
      cancel(state);
    } else if (key === 'ArrowDown' || key === 'ArrowUp') {
      event.preventDefault();
      const to = step(state, key === 'ArrowDown' ? 1 : -1);
      if (to) moveTo(state, to);
    } else if (key === 'Home' || key === 'End') {
      event.preventDefault();
      const at = locate(state.preview, state.key, getItemId);
      const len = state.preview.find((b) => b.id === at?.bucketId)?.items.length ?? 1;
      if (at) moveTo(state, { bucketId: at.bucketId, index: key === 'Home' ? 0 : len - 1 });
    }
  }

  // Keep focus on the moving item's handle when it changes bucket (it remounts).
  useEffect(() => {
    if (drag?.mode !== 'keyboard') return;
    const el = [
      ...(rootRef.current?.querySelectorAll<HTMLElement>('[data-handle-for]') ?? []),
    ].find((h) => h.dataset.handleFor === drag.key);
    if (el && document.activeElement !== el) el.focus();
  }, [drag]);

  // Pointer dragging from the handle.
  const pointer = useRef<{
    id: number;
    x: number;
    y: number;
    item: T;
    started: boolean;
    cancelled?: boolean;
  } | null>(null);
  // Moves are followed on the window: the row remounts when it changes bucket,
  // which would drop pointer capture on the original handle.
  function onHandlePointerDown(event: ReactPointerEvent<HTMLButtonElement>, item: T) {
    if (event.button !== 0 || dragRef.current || pointer.current) return;
    pointer.current = {
      id: event.pointerId,
      x: event.clientX,
      y: event.clientY,
      item,
      started: false,
    };
    const move = (e: PointerEvent) => onPointerMove(e);
    const end = (e: PointerEvent) => {
      if (pointer.current && e.pointerId !== pointer.current.id) return;
      window.removeEventListener('pointermove', move);
      window.removeEventListener('pointerup', end);
      window.removeEventListener('pointercancel', end);
      onPointerEnd(e.type === 'pointercancel');
    };
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', end);
    window.addEventListener('pointercancel', end);
  }
  function targetAt(clientY: number, state: DragState<T>): Location | null {
    const root = rootRef.current;
    if (!root) return null;
    const sections = [...root.querySelectorAll<HTMLElement>('[data-bucket-id]')];
    let chosen: HTMLElement | null = null;
    let best = Infinity;
    for (const s of sections) {
      const r = s.getBoundingClientRect();
      const d = clientY < r.top ? r.top - clientY : clientY > r.bottom ? clientY - r.bottom : 0;
      if (d < best) {
        best = d;
        chosen = s;
      }
    }
    const bucketId = chosen?.dataset.bucketId;
    if (!chosen || !bucketId || !allowed(state.item, bucketId, state.origin.bucketId)) return null;
    const rows = [...chosen.querySelectorAll<HTMLElement>('[data-item-id]')].filter(
      (r) => r.dataset.itemId !== state.key,
    );
    let index = 0;
    for (const r of rows) {
      const rect = r.getBoundingClientRect();
      if (clientY > rect.top + rect.height / 2) index += 1;
    }
    return { bucketId, index };
  }
  function onPointerMove(event: PointerEvent) {
    const p = pointer.current;
    if (!p || p.cancelled || p.id !== event.pointerId) return;
    let state = dragRef.current;
    if (!p.started) {
      if (Math.hypot(event.clientX - p.x, event.clientY - p.y) < 4) return;
      p.started = true;
      state = start(p.item, 'pointer');
    }
    if (!state) return;
    const to = targetAt(event.clientY, state);
    const at = locate(state.preview, state.key, getItemId);
    if (to && at && (to.bucketId !== at.bucketId || to.index !== at.index)) {
      dragRef.current = moveTo(state, to, false);
    }
  }
  function onPointerEnd(cancelled: boolean) {
    const p = pointer.current;
    pointer.current = null;
    const state = dragRef.current;
    if (!p?.started || p.cancelled || !state || state.mode !== 'pointer') return;
    if (cancelled) cancel(state);
    else drop(state);
  }
  useEffect(() => {
    if (drag?.mode !== 'pointer') return;
    const onKey = (e: globalThis.KeyboardEvent) => {
      if (e.key !== 'Escape' || !dragRef.current) return;
      // The window listeners end on the next pointerup; a cancelled drag drops nothing.
      if (pointer.current) pointer.current.cancelled = true;
      cancel(dragRef.current);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [drag?.mode]); // eslint-disable-line react-hooks/exhaustive-deps

  function menuMove(item: T, toBucketId: string) {
    const key = getItemId(item);
    const from = locate(base, key, getItemId);
    if (!from) return;
    const target = base.find((b) => b.id === toBucketId);
    const state: DragState<T> = {
      key,
      label: getItemLabel(item),
      item,
      mode: 'keyboard',
      origin: { bucketId: from.bucketId, index: from.index },
      preview: base,
    };
    const next = moveBacklogItem(
      base,
      key,
      { bucketId: toBucketId, index: target?.items.length ?? 0 },
      getItemId,
    );
    const at = locate(next, key, getItemId);
    const total = next.find((b) => b.id === toBucketId)?.items.length ?? 0;
    if (at)
      setAnnouncement(messages.dropped(state.label, bucketTitle(toBucketId), at.index + 1, total));
    void commit(state, next);
  }

  return (
    <div
      ref={(node) => {
        rootRef.current = node;
        if (typeof ref === 'function') ref(node);
        else if (ref) ref.current = node;
      }}
      {...rest}
      className={cx('mn-backlog-planner', className)}
      data-dragging={drag ? drag.mode : undefined}
    >
      {shown.map((bucket) => {
        const isCollapsed = collapsed.has(bucket.id);
        const pointsList = getItemPoints ? bucket.items.map(getItemPoints) : [];
        const points = getItemPoints ? pointsList.reduce<number>((s, p) => s + (p ?? 0), 0) : null;
        const unestimated = pointsList.filter((p) => p === null).length;
        const capacity = bucket.capacity ?? null;
        const over = capacity !== null && points !== null && points > capacity;
        const listId = `${baseId}-${bucket.id}`;
        const title = titleOf(bucket);
        const refused =
          drag !== null && !allowed(drag.item, bucket.id, drag.origin.bucketId) ? '' : undefined;
        return (
          <section
            key={bucket.id}
            className="mn-backlog-planner__bucket"
            data-bucket-id={bucket.id}
            data-collapsed={isCollapsed || undefined}
            data-over-capacity={over || undefined}
            data-refused={refused}
            aria-labelledby={`${listId}-title`}
          >
            <header className="mn-backlog-planner__header">
              <Heading id={`${listId}-title`} className="mn-backlog-planner__title">
                <button
                  type="button"
                  className="mn-backlog-planner__toggle"
                  aria-expanded={!isCollapsed}
                  aria-controls={isCollapsed ? undefined : listId}
                  onClick={() => toggleBucket(bucket.id)}
                >
                  <ChevronRightGlyph aria-hidden />
                  {bucket.title}
                </button>
              </Heading>
              {bucket.subtitle ? (
                <span className="mn-backlog-planner__subtitle">{bucket.subtitle}</span>
              ) : null}
              {bucket.badge}
              <span className="mn-backlog-planner__summary">
                {messages.summary(bucket.items.length, points, unestimated)}
              </span>
              {capacity !== null && points !== null ? (
                <span className="mn-backlog-planner__capacity">
                  <Progress
                    size="sm"
                    semantics="meter"
                    value={points}
                    max={Math.max(capacity, 1)}
                    label={`${title}: ${messages.capacity(points, capacity)}`}
                    valueText={messages.capacity(points, capacity)}
                    className="mn-backlog-planner__meter"
                  />
                  <span aria-hidden>{messages.capacity(points, capacity)}</span>
                </span>
              ) : null}
              {bucket.actions ? (
                <span className="mn-backlog-planner__actions">{bucket.actions}</span>
              ) : null}
            </header>
            {over && capacity !== null && points !== null ? (
              <p className="mn-backlog-planner__warning">
                {messages.overCapacity(points - capacity)}
              </p>
            ) : null}
            {isCollapsed ? null : (
              <>
                <ul
                  id={listId}
                  className="mn-backlog-planner__list"
                  aria-labelledby={`${listId}-title`}
                >
                  {bucket.items.map((item) => {
                    const key = getItemId(item);
                    const label = getItemLabel(item);
                    const p = getItemPoints ? getItemPoints(item) : undefined;
                    const moving = drag?.key === key;
                    return (
                      <li
                        key={key}
                        className="mn-backlog-planner__item"
                        data-item-id={key}
                        data-dragging={moving ? drag.mode : undefined}
                        data-pending={pendingKey === key || undefined}
                        aria-busy={pendingKey === key || undefined}
                      >
                        {editable ? (
                          <button
                            type="button"
                            className="mn-backlog-planner__handle"
                            data-handle-for={key}
                            aria-label={messages.handleLabel(label)}
                            aria-describedby={instructionsId}
                            aria-pressed={moving && drag.mode === 'keyboard'}
                            onKeyDown={(e) => onHandleKeyDown(e, item)}
                            onBlur={(e) => {
                              // Focus moving to another control cancels a keyboard move.
                              const s = dragRef.current;
                              const next = e.relatedTarget as HTMLElement | null;
                              if (
                                s?.mode === 'keyboard' &&
                                s.key === key &&
                                next &&
                                next.dataset.handleFor !== key
                              )
                                cancel(s);
                            }}
                            onPointerDown={(e) => onHandlePointerDown(e, item)}
                          >
                            <GripGlyph />
                          </button>
                        ) : null}
                        <div className="mn-backlog-planner__content">{renderItem(item)}</div>
                        {getItemPoints ? (
                          <span className="mn-backlog-planner__points">
                            {p === null || p === undefined ? (
                              <>
                                <span aria-hidden>{messages.unestimated}</span>
                                <span className="mn-visually-hidden">{messages.notEstimated}</span>
                              </>
                            ) : (
                              messages.points(p)
                            )}
                          </span>
                        ) : null}
                        {editable && moveMenu ? (
                          <DropdownMenu>
                            <DropdownMenuTrigger
                              className="mn-backlog-planner__menu"
                              aria-label={messages.moveMenu(label)}
                            >
                              <MoreGlyph aria-hidden />
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuLabel>{messages.moveTo}</DropdownMenuLabel>
                              {shown
                                .filter((b) => b.id !== bucket.id)
                                .map((b) => (
                                  <DropdownMenuItem
                                    key={b.id}
                                    disabled={!allowed(item, b.id, bucket.id)}
                                    onSelect={() => menuMove(item, b.id)}
                                  >
                                    {titleOf(b)}
                                  </DropdownMenuItem>
                                ))}
                            </DropdownMenuContent>
                          </DropdownMenu>
                        ) : null}
                      </li>
                    );
                  })}
                  {bucket.items.length === 0 ? (
                    <li className="mn-backlog-planner__empty">{bucket.empty ?? messages.empty}</li>
                  ) : null}
                </ul>
                {bucket.footer ? (
                  <div className="mn-backlog-planner__footer">{bucket.footer}</div>
                ) : null}
              </>
            )}
          </section>
        );
      })}
      <p id={instructionsId} className="mn-visually-hidden">
        {messages.instructions}
      </p>
      <p className="mn-visually-hidden" role="status" aria-live="assertive">
        {announcement}
      </p>
    </div>
  );
}

/**
 * Backlog and iteration planning on one page: buckets stacked top to bottom
 * (sprints, releases, the backlog) with items moved between and within them
 * by keyboard, pointer or a "Move to…" menu, with points against capacity.
 */
export const BacklogPlanner = forwardRef(BacklogPlannerInner) as <T>(
  props: BacklogPlannerProps<T> & { ref?: ForwardedRef<HTMLDivElement> },
) => ReactElement;
