'use client';
import {
  forwardRef,
  useCallback,
  useId,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import { IconButton } from '../../actions/button/index.js';
import { moveItem } from '../../interaction/reorderable-list/index.js';
import type { WidgetSpan } from '../widget/index.js';

/** One widget placed by a DashboardGrid. */
export interface DashboardGridItem {
  /** Stable key, stored in the layout. */
  id: string;
  /** Plain-text name, used in the arrange controls and announcements, e.g. "Team load". */
  label: string;
  /** Columns spanned on wide screens. Clamped to `columns`. Default `1`. */
  span?: WidgetSpan | undefined;
  /** The widget itself, usually a Widget. */
  content: ReactNode;
}

/** A finished move, passed to `onLayoutChange`. */
export interface DashboardGridMove {
  id: string;
  from: number;
  to: number;
}

/** Labels and announcements. Override them to translate. */
export interface DashboardGridMessages {
  /** Accessible name of the grid when no `aria-label` is given. */
  label: string;
  /** Accessible name of a widget's drag handle. */
  handleLabel: (label: string) => string;
  /** Read once by every handle. */
  instructions: string;
  moveEarlier: (label: string) => string;
  moveLater: (label: string) => string;
  pickedUp: (label: string, position: number, total: number) => string;
  moved: (label: string, position: number, total: number) => string;
  dropped: (label: string, position: number, total: number) => string;
  cancelled: (label: string, position: number, total: number) => string;
}

export const DASHBOARD_GRID_MESSAGES: DashboardGridMessages = {
  label: 'Dashboard',
  handleLabel: (label) => `Move ${label}`,
  instructions:
    'Press Space or Enter to pick up. Use the arrow keys to move, Space or Enter to drop, Escape to cancel.',
  moveEarlier: (label) => `Move ${label} earlier`,
  moveLater: (label) => `Move ${label} later`,
  pickedUp: (label, p, n) => `Picked up ${label}. Position ${p} of ${n}.`,
  moved: (label, p, n) => `${label} moved to position ${p} of ${n}.`,
  dropped: (label, p, n) => `${label} dropped at position ${p} of ${n}.`,
  cancelled: (label, p, n) => `Move cancelled. ${label} returned to position ${p} of ${n}.`,
};

export interface DashboardGridProps extends Omit<HTMLAttributes<HTMLDivElement>, 'defaultValue'> {
  /**
   * The widgets, in their default order. Give `items` to let people rearrange
   * the dashboard; without it the grid lays out `children` as they are, and a
   * child Widget's `span` still applies.
   */
  items?: readonly DashboardGridItem[] | undefined;
  /**
   * The order of item ids (controlled), as saved for this person. Ids that no
   * longer exist are skipped and items missing from it are added at the end, so
   * a stored layout survives widgets being added or removed.
   */
  layout?: readonly string[] | undefined;
  /** The starting order when uncontrolled. Default: the order of `items`. */
  defaultLayout?: readonly string[] | undefined;
  /** Called once per finished move with the full new order: store it to persist the layout. */
  onLayoutChange?: ((layout: string[], move: DashboardGridMove) => void) | undefined;
  /**
   * Arrange mode: each widget gets a drag handle (pointer and keyboard) and
   * Move earlier / Move later buttons. Toggle it from an "Arrange" button.
   */
  editing?: boolean | undefined;
  /** Columns on wide screens (from 64rem of grid width). Medium widths use two, narrow one. Default `3`. */
  columns?: 1 | 2 | 3 | 4 | undefined;
  /** Strings, for localisation. */
  messages?: Partial<DashboardGridMessages> | undefined;
  children?: ReactNode;
}

/** Resolves a stored layout against the current items. */
export function resolveDashboardLayout(
  items: readonly Pick<DashboardGridItem, 'id'>[],
  layout: readonly string[] | undefined,
): string[] {
  const known = new Set(items.map((i) => i.id));
  const seen = new Set<string>();
  const out: string[] = [];
  for (const id of layout ?? []) {
    if (known.has(id) && !seen.has(id)) {
      seen.add(id);
      out.push(id);
    }
  }
  for (const item of items) if (!seen.has(item.id)) out.push(item.id);
  return out;
}

function clampSpan(span: WidgetSpan | undefined, columns: number): string | undefined {
  if (span === undefined || span === 1) return undefined;
  if (span === 'full' || span >= columns) return 'full';
  return String(span);
}

interface Drag {
  id: string;
  /** The order when the move started, restored on cancel. */
  origin: string[];
  /** `keyboard` once picked up with Space/Enter; `pointer` while a drag is under way. */
  mode: 'keyboard' | 'pointer';
  /** Pointer drags start only after the pointer moves a few pixels. */
  started: boolean;
  startX: number;
  startY: number;
}

const DRAG_THRESHOLD = 4;

function GripGlyph() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden>
      <circle cx="6" cy="4" r="1.25" />
      <circle cx="10" cy="4" r="1.25" />
      <circle cx="6" cy="8" r="1.25" />
      <circle cx="10" cy="8" r="1.25" />
      <circle cx="6" cy="12" r="1.25" />
      <circle cx="10" cy="12" r="1.25" />
    </svg>
  );
}

/**
 * The responsive grid a dashboard's widgets sit in. One column on narrow
 * screens, two at medium widths and `columns` on wide ones. With `items` and
 * `editing`, people can rearrange widgets by dragging or with the keyboard;
 * the new order is handed to `onLayoutChange` for the app to store.
 */
export const DashboardGrid = forwardRef<HTMLDivElement, DashboardGridProps>(function DashboardGrid(
  {
    items,
    layout: layoutProp,
    defaultLayout,
    onLayoutChange,
    editing = false,
    columns = 3,
    messages: messagesProp,
    className,
    style,
    children,
    'aria-label': ariaLabel,
    ...rest
  },
  ref,
) {
  const messages = useMemo(() => ({ ...DASHBOARD_GRID_MESSAGES, ...messagesProp }), [messagesProp]);
  const id = useId();
  const instructionsId = `${id}-instructions`;
  const [internal, setInternal] = useState<readonly string[] | undefined>(defaultLayout);
  const committed = useMemo(
    () => resolveDashboardLayout(items ?? [], layoutProp ?? internal),
    [items, layoutProp, internal],
  );
  const [drag, setDrag] = useState<Drag | null>(null);
  const dragRef = useRef<Drag | null>(null);
  dragRef.current = drag;
  const [preview, setPreview] = useState<string[] | null>(null);
  const previewRef = useRef<string[] | null>(null);
  previewRef.current = preview;
  const order = preview ?? committed;
  const [announcement, setAnnouncement] = useState('');
  const cells = useRef(new Map<string, HTMLDivElement>());
  const handles = useRef(new Map<string, HTMLButtonElement>());
  const focusAfter = useRef<HTMLElement | null>(null);

  const byId = useMemo(() => new Map((items ?? []).map((i) => [i.id, i])), [items]);
  const labelOf = useCallback((itemId: string) => byId.get(itemId)?.label ?? itemId, [byId]);

  useLayoutEffect(() => {
    if (focusAfter.current) {
      focusAfter.current.focus();
      focusAfter.current = null;
    }
  });

  const commit = useCallback(
    (next: string[], movedId: string, from: number) => {
      const to = next.indexOf(movedId);
      if (to === from) return;
      if (layoutProp === undefined) setInternal(next);
      onLayoutChange?.(next, { id: movedId, from, to });
    },
    [layoutProp, onLayoutChange],
  );

  const end = useCallback(
    (outcome: 'drop' | 'cancel') => {
      const current = dragRef.current;
      if (!current) return;
      const shown = previewRef.current ?? current.origin;
      const label = labelOf(current.id);
      const total = current.origin.length;
      if (outcome === 'cancel' || !current.started) {
        if (current.started) {
          setAnnouncement(messages.cancelled(label, current.origin.indexOf(current.id) + 1, total));
        }
      } else {
        setAnnouncement(messages.dropped(label, shown.indexOf(current.id) + 1, total));
        commit(shown, current.id, current.origin.indexOf(current.id));
      }
      setDrag(null);
      setPreview(null);
    },
    [commit, labelOf, messages],
  );

  const shift = (itemId: string, to: number) => {
    const current = previewRef.current ?? committed;
    const from = current.indexOf(itemId);
    const target = Math.max(0, Math.min(to, current.length - 1));
    if (from === target) return;
    const next = moveItem(current, from, target);
    setPreview(next);
    setAnnouncement(messages.moved(labelOf(itemId), target + 1, next.length));
    const handle = handles.current.get(itemId);
    if (handle) focusAfter.current = handle;
  };

  const onHandleKeyDown = (event: ReactKeyboardEvent<HTMLButtonElement>, itemId: string) => {
    const current = dragRef.current;
    const active = current?.id === itemId && current.mode === 'keyboard';
    const key = event.key;
    if (key === ' ' || key === 'Enter') {
      event.preventDefault();
      if (active) {
        end('drop');
      } else {
        const origin = committed.slice();
        setDrag({ id: itemId, origin, mode: 'keyboard', started: true, startX: 0, startY: 0 });
        setPreview(origin);
        setAnnouncement(
          messages.pickedUp(labelOf(itemId), origin.indexOf(itemId) + 1, origin.length),
        );
      }
      return;
    }
    if (!active) return;
    const shown = previewRef.current ?? committed;
    const index = shown.indexOf(itemId);
    if (key === 'Escape') {
      event.preventDefault();
      end('cancel');
    } else if (key === 'ArrowLeft' || key === 'ArrowUp') {
      event.preventDefault();
      shift(itemId, index - 1);
    } else if (key === 'ArrowRight' || key === 'ArrowDown') {
      event.preventDefault();
      shift(itemId, index + 1);
    } else if (key === 'Home') {
      event.preventDefault();
      shift(itemId, 0);
    } else if (key === 'End') {
      event.preventDefault();
      shift(itemId, shown.length - 1);
    }
  };

  const onHandlePointerDown = (event: ReactPointerEvent<HTMLButtonElement>, itemId: string) => {
    if (event.button !== 0 || dragRef.current) return;
    event.currentTarget.setPointerCapture?.(event.pointerId);
    setDrag({
      id: itemId,
      origin: committed.slice(),
      mode: 'pointer',
      started: false,
      startX: event.clientX,
      startY: event.clientY,
    });
  };

  const onHandlePointerMove = (event: ReactPointerEvent<HTMLButtonElement>) => {
    const current = dragRef.current;
    if (!current || current.mode !== 'pointer') return;
    if (!current.started) {
      if (
        Math.abs(event.clientX - current.startX) < DRAG_THRESHOLD &&
        Math.abs(event.clientY - current.startY) < DRAG_THRESHOLD
      )
        return;
      const started = { ...current, started: true };
      dragRef.current = started;
      setDrag(started);
      setPreview(current.origin);
      setAnnouncement(
        messages.pickedUp(
          labelOf(current.id),
          current.origin.indexOf(current.id) + 1,
          current.origin.length,
        ),
      );
    }
    const shown = previewRef.current ?? current.origin;
    for (const [cellId, node] of cells.current) {
      if (cellId === current.id) continue;
      const rect = node.getBoundingClientRect();
      if (
        event.clientX >= rect.left &&
        event.clientX <= rect.right &&
        event.clientY >= rect.top &&
        event.clientY <= rect.bottom
      ) {
        const to = shown.indexOf(cellId);
        const from = shown.indexOf(current.id);
        if (to !== from && to >= 0) {
          const next = moveItem(shown, from, to);
          previewRef.current = next;
          setPreview(next);
          setAnnouncement(messages.moved(labelOf(current.id), to + 1, next.length));
        }
        break;
      }
    }
  };

  const onHandlePointerUp = () => {
    if (dragRef.current?.mode === 'pointer') end('drop');
  };
  const onHandlePointerCancel = () => {
    if (dragRef.current?.mode === 'pointer') end('cancel');
  };

  const moveByButton = (itemId: string, delta: -1 | 1, button: HTMLButtonElement) => {
    const from = committed.indexOf(itemId);
    const to = from + delta;
    if (to < 0 || to >= committed.length) return;
    const next = moveItem(committed, from, to);
    setAnnouncement(messages.moved(labelOf(itemId), to + 1, next.length));
    commit(next, itemId, from);
    // Keep focus on the pressed button; at an end it is disabled, so use the handle.
    const atEnd = to === 0 || to === next.length - 1;
    focusAfter.current = atEnd ? (handles.current.get(itemId) ?? button) : button;
  };

  const vars = { '--mn-dashboard-grid-columns': columns } as CSSProperties;
  const reorderable = items !== undefined;

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-dashboard-grid', className)}
      style={{ ...vars, ...style }}
      data-columns={columns}
      data-editing={editing && reorderable ? '' : undefined}
      data-dragging={drag?.started ? '' : undefined}
    >
      {reorderable ? (
        <>
          <div
            className="mn-dashboard-grid__grid"
            role="list"
            aria-label={ariaLabel ?? messages.label}
          >
            {order.map((itemId, index) => {
              const item = byId.get(itemId);
              if (!item) return null;
              const active = drag?.id === itemId && drag.started;
              return (
                <div
                  key={itemId}
                  role="listitem"
                  className="mn-dashboard-grid__cell"
                  data-span={clampSpan(item.span, columns)}
                  data-active={active ? '' : undefined}
                  ref={(node) => {
                    if (node) cells.current.set(itemId, node);
                    else cells.current.delete(itemId);
                  }}
                >
                  {editing ? (
                    <div className="mn-dashboard-grid__arrange">
                      <button
                        type="button"
                        className="mn-dashboard-grid__handle"
                        aria-label={messages.handleLabel(item.label)}
                        aria-describedby={instructionsId}
                        aria-pressed={active}
                        ref={(node) => {
                          if (node) handles.current.set(itemId, node);
                          else handles.current.delete(itemId);
                        }}
                        onKeyDown={(event) => onHandleKeyDown(event, itemId)}
                        onPointerDown={(event) => onHandlePointerDown(event, itemId)}
                        onPointerMove={onHandlePointerMove}
                        onPointerUp={onHandlePointerUp}
                        onPointerCancel={onHandlePointerCancel}
                        onBlur={(event) => {
                          if (
                            dragRef.current?.id === itemId &&
                            dragRef.current.mode === 'keyboard' &&
                            event.relatedTarget !== null
                          )
                            end('drop');
                        }}
                      >
                        <GripGlyph />
                        <span className="mn-dashboard-grid__handle-text" aria-hidden>
                          {item.label}
                        </span>
                      </button>
                      <span className="mn-dashboard-grid__move">
                        <IconButton
                          size="sm"
                          variant="ghost"
                          className="mn-dashboard-grid__move-button"
                          data-direction="earlier"
                          label={messages.moveEarlier(item.label)}
                          icon={<ChevronDownGlyph />}
                          disabled={index === 0 || drag !== null}
                          onClick={(event) => moveByButton(itemId, -1, event.currentTarget)}
                        />
                        <IconButton
                          size="sm"
                          variant="ghost"
                          className="mn-dashboard-grid__move-button"
                          data-direction="later"
                          label={messages.moveLater(item.label)}
                          icon={<ChevronDownGlyph />}
                          disabled={index === order.length - 1 || drag !== null}
                          onClick={(event) => moveByButton(itemId, 1, event.currentTarget)}
                        />
                      </span>
                    </div>
                  ) : null}
                  {item.content}
                </div>
              );
            })}
          </div>
          {editing ? (
            <span id={instructionsId} className="mn-visually-hidden">
              {messages.instructions}
            </span>
          ) : null}
          <span className="mn-visually-hidden" role="status" aria-live="polite">
            {announcement}
          </span>
        </>
      ) : (
        <div
          className="mn-dashboard-grid__grid"
          role={ariaLabel ? 'group' : undefined}
          aria-label={ariaLabel}
        >
          {children}
        </div>
      )}
    </div>
  );
});
