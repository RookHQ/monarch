'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type DragEvent,
  type HTMLAttributes,
  type KeyboardEvent,
  type PointerEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
import { IconButton } from '../../actions/button/index.js';
import { Avatar } from '../../identity/avatar/index.js';

/** One person or open position in the chart. */
export interface OrgChartNode {
  /** Stable id. */
  id: string;
  /** Manager's id; `null` or a manager not in `nodes` makes it a top-level node. */
  parentId?: string | null | undefined;
  /** Person's name, or the title of an open position. */
  name: string;
  /** Job title, shown under the name. */
  title?: string | undefined;
  /** Photo URL. Initials are shown without one. */
  avatarSrc?: string | null | undefined;
  /** Chips or text under the name, such as department and team. Display only. */
  meta?: ReactNode;
  /** `vacancy` draws an open position with a dashed border. Default `person`. */
  kind?: 'person' | 'vacancy';
  /** Dashed border for a temporary or acting reporting line. */
  dashed?: boolean;
  /**
   * Total direct reports, including ones not loaded yet. When it is larger
   * than the loaded children, expanding calls `onLoadChildren` first.
   */
  childCount?: number | undefined;
}

/** A secondary (dotted) reporting line. */
export interface OrgChartDottedLine {
  /** The person who also reports. */
  from: string;
  /** The secondary manager. */
  to: string;
}

/** Visible and announced strings. Override them to translate. */
export interface OrgChartMessages {
  /** Accessible name of the tree. */
  label: string;
  /** Hidden text for an open position. */
  vacant: string;
  /** Hidden count of direct reports. */
  reports: (count: number) => string;
  /** Hidden note for a dashed (temporary) reporting line. */
  dashed: string;
  /** Accessible name of the zoom controls. */
  zoom: string;
  zoomIn: string;
  zoomOut: string;
  zoomReset: string;
  /** Zoom level, shown and announced. */
  zoomLevel: (percent: number) => string;
  /** Visible hint when moving is enabled. */
  moveHint: string;
  /** Announced after branches load or fail. */
  loadFailed: (name: string) => string;
  /** Visible hint for keyboard use (hidden; describes the tree). */
  instructions: string;
}

export const DEFAULT_ORG_CHART_MESSAGES: OrgChartMessages = {
  label: 'Organisation chart',
  vacant: 'Open position',
  reports: (n) => `${n} direct ${n === 1 ? 'report' : 'reports'}`,
  dashed: 'temporary reporting line',
  zoom: 'Zoom',
  zoomIn: 'Zoom in',
  zoomOut: 'Zoom out',
  zoomReset: 'Reset zoom',
  zoomLevel: (p) => `${p}%`,
  moveHint: 'Drag a person onto their new manager, or select them and press M.',
  loadFailed: (name) => `Could not load ${name}’s team.`,
  instructions:
    'Use the arrow keys to move between people, Right to open a team, Left to close it or go to the manager, and Enter to open a person.',
};

export interface OrgChartProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'onSelect'
> {
  /** Every person and open position, flat; reporting lines come from `parentId`. */
  nodes: OrgChartNode[];
  /** Ids of expanded nodes (controlled). */
  expanded?: string[] | undefined;
  /** Ids expanded at first (uncontrolled). Default: the top two levels when there are three or fewer top-level people. */
  defaultExpanded?: string[] | undefined;
  /** Called with the new expanded ids. */
  onExpandedChange?: ((expanded: string[]) => void) | undefined;
  /** Called when a node is chosen with a click or Enter, for example to open a side panel. */
  onSelect?: ((node: OrgChartNode) => void) | undefined;
  /** Loads a branch whose `childCount` is larger than the children in `nodes`. Return a promise; the product then adds the nodes. */
  onLoadChildren?: ((node: OrgChartNode) => unknown) | undefined;
  /**
   * Enables moving people. Called when a card is dropped on another card
   * (`newParentId` is that id), on the empty background (`null`), or when M is
   * pressed on a card (`undefined`: ask where). Moves into one's own team are refused.
   */
  onMoveRequest?:
    ((node: OrgChartNode, newParentId: string | null | undefined) => void) | undefined;
  /** Highlights a node, opens the path to it and scrolls it into view. */
  focusId?: string | null | undefined;
  /** Draw a node's card content yourself (inside the focusable card). Do not put buttons or links in it. */
  renderNode?: ((node: OrgChartNode) => ReactNode) | undefined;
  /** Secondary reporting lines, drawn as dashed curves in the chart layout. */
  dottedLines?: OrgChartDottedLine[] | undefined;
  /**
   * `chart`: top-down boxes and lines. `list`: an indented list. `auto`
   * (default): the chart, switching to the list when the component is narrower than 40rem.
   */
  layout?: 'auto' | 'chart' | 'list';
  /** Show zoom buttons (chart layout). Default `true`. */
  zoomable?: boolean;
  /** Extra controls next to the zoom buttons, such as export. */
  toolbar?: ReactNode;
  /** Override visible and announced strings. */
  messages?: Partial<OrgChartMessages>;
}

const ZOOM_MIN = 0.4;
const ZOOM_MAX = 1.6;
const clampZoom = (z: number) => Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, Math.round(z * 10) / 10));

/**
 * An organisation chart: people as cards under their managers, with
 * expandable teams, lazy-loaded branches, zoom and pan, dotted reporting
 * lines and drag-to-move. A full ARIA tree from the keyboard, and an indented
 * list on narrow screens.
 */
export const OrgChart = forwardRef<HTMLDivElement, OrgChartProps>(function OrgChart(
  {
    nodes,
    expanded: expandedProp,
    defaultExpanded,
    onExpandedChange,
    onSelect,
    onLoadChildren,
    onMoveRequest,
    focusId,
    renderNode,
    dottedLines = [],
    layout = 'auto',
    zoomable = true,
    toolbar,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_ORG_CHART_MESSAGES, ...messagesProp };
  const instructionsId = useId();

  const { byId, roots, children } = useMemo(() => {
    const map = new Map(nodes.map((n) => [n.id, n]));
    const kids = new Map<string, OrgChartNode[]>();
    const top: OrgChartNode[] = [];
    for (const n of nodes) {
      const p = n.parentId && n.parentId !== n.id && map.has(n.parentId) ? n.parentId : null;
      if (p === null) top.push(n);
      else kids.set(p, [...(kids.get(p) ?? []), n]);
    }
    return { byId: map, roots: top, children: kids };
  }, [nodes]);

  const ancestors = useCallback(
    (id: string): string[] => {
      const out: string[] = [];
      const seen = new Set<string>([id]);
      let cur = byId.get(id)?.parentId ?? null;
      while (cur && byId.has(cur) && !seen.has(cur)) {
        out.push(cur);
        seen.add(cur);
        cur = byId.get(cur)?.parentId ?? null;
      }
      return out;
    },
    [byId],
  );

  const [expandedState, setExpandedState] = useState<string[]>(() => {
    const open: string[] = [];
    if (defaultExpanded) open.push(...defaultExpanded);
    else {
      open.push(...roots.map((r) => r.id));
      if (roots.length <= 3)
        for (const r of roots) for (const c of children.get(r.id) ?? []) open.push(c.id);
    }
    if (focusId) open.push(...ancestors(focusId));
    return [...new Set(open)];
  });
  const expanded = useMemo(
    () => new Set(expandedProp ?? expandedState),
    [expandedProp, expandedState],
  );
  const setExpanded = useCallback(
    (next: Set<string>) => {
      const list = [...next];
      if (expandedProp === undefined) setExpandedState(list);
      onExpandedChange?.(list);
    },
    [expandedProp, onExpandedChange],
  );

  // Open the path to a newly focused node.
  const lastFocusId = useRef(focusId);
  useEffect(() => {
    if (!focusId || lastFocusId.current === focusId) return;
    lastFocusId.current = focusId;
    const path = ancestors(focusId).filter((id) => !expanded.has(id));
    if (path.length) setExpanded(new Set([...expanded, ...path]));
  }, [focusId, ancestors, expanded, setExpanded]);

  const hasChildren = (n: OrgChartNode) =>
    (children.get(n.id)?.length ?? 0) > 0 || (n.childCount ?? 0) > 0;

  // Visible nodes in tree order, for the keyboard.
  const visible = useMemo(() => {
    const out: OrgChartNode[] = [];
    const walk = (list: OrgChartNode[], seen: Set<string>) => {
      for (const n of list) {
        if (seen.has(n.id)) continue;
        out.push(n);
        if (expanded.has(n.id)) walk(children.get(n.id) ?? [], new Set(seen).add(n.id));
      }
    };
    walk(roots, new Set());
    return out;
  }, [roots, children, expanded]);

  const [active, setActive] = useState<string | null>(null);
  const activeId =
    active && visible.some((n) => n.id === active)
      ? active
      : focusId && visible.some((n) => n.id === focusId)
        ? focusId
        : (visible[0]?.id ?? null);

  const [loading, setLoading] = useState<string | null>(null);
  const [announcement, setAnnouncement] = useState('');
  const treeRef = useRef<HTMLUListElement>(null);

  // Keyboard moves set the active node, then focus it after the render that
  // gives it tabindex 0 (and, after expanding, after it exists).
  const pendingFocus = useRef<string | null>(null);
  const focusNode = (id: string) => {
    pendingFocus.current = id;
    setActive(id);
  };
  useEffect(() => {
    const id = pendingFocus.current;
    if (!id) return;
    const el = findNode(treeRef.current, id);
    if (el) {
      pendingFocus.current = null;
      el.focus();
    }
  });

  async function toggle(n: OrgChartNode, open?: boolean) {
    const isOpen = expanded.has(n.id);
    const want = open ?? !isOpen;
    if (want === isOpen) return;
    if (!want) {
      const next = new Set(expanded);
      next.delete(n.id);
      setExpanded(next);
      return;
    }
    const loaded = children.get(n.id)?.length ?? 0;
    if (onLoadChildren && (n.childCount ?? 0) > loaded) {
      setLoading(n.id);
      try {
        await onLoadChildren(n);
      } catch {
        setAnnouncement(messages.loadFailed(n.name));
        return;
      } finally {
        setLoading(null);
      }
    }
    setExpanded(new Set([...expanded, n.id]));
  }

  function onKeyDown(event: KeyboardEvent<HTMLElement>, n: OrgChartNode) {
    const i = visible.findIndex((v) => v.id === n.id);
    const key = event.key;
    let handled = true;
    if (key === 'ArrowDown') {
      const next = visible[i + 1];
      if (next) focusNode(next.id);
    } else if (key === 'ArrowUp') {
      const prev = visible[i - 1];
      if (prev) focusNode(prev.id);
    } else if (key === 'Home') {
      if (visible[0]) focusNode(visible[0].id);
    } else if (key === 'End') {
      const last = visible[visible.length - 1];
      if (last) focusNode(last.id);
    } else if (key === 'ArrowRight') {
      if (hasChildren(n) && !expanded.has(n.id)) void toggle(n, true);
      else {
        const first = children.get(n.id)?.[0];
        if (first && expanded.has(n.id)) focusNode(first.id);
      }
    } else if (key === 'ArrowLeft') {
      if (expanded.has(n.id) && hasChildren(n)) void toggle(n, false);
      else if (n.parentId && byId.has(n.parentId)) focusNode(n.parentId);
    } else if (key === 'Enter' || key === ' ') {
      onSelect?.(n);
    } else if ((key === 'm' || key === 'M') && onMoveRequest && n.kind !== 'vacancy') {
      onMoveRequest(n, undefined);
    } else handled = false;
    if (handled) event.preventDefault();
  }

  // Zoom and pan.
  const [zoom, setZoom] = useState(1);
  const scroller = useRef<HTMLDivElement>(null);
  const inner = useRef<HTMLDivElement>(null);
  const pan = useRef<{ x: number; y: number; left: number; top: number; id: number } | null>(null);

  useEffect(() => {
    if (!focusId) return;
    const el = findNode(treeRef.current, focusId);
    if (el && typeof el.scrollIntoView === 'function') {
      const reduce =
        typeof window.matchMedia === 'function' &&
        window.matchMedia('(prefers-reduced-motion: reduce)').matches;
      el.scrollIntoView({
        block: 'center',
        inline: 'center',
        behavior: reduce ? 'auto' : 'smooth',
      });
    }
  }, [focusId, expanded]);

  // Dotted lines are measured from the real card positions.
  const [lines, setLines] = useState<Array<{ key: string; d: string; x: number; y: number }>>([]);
  const [size, setSize] = useState({ w: 0, h: 0 });
  const measure = useCallback(() => {
    const root = inner.current;
    if (!root || dottedLines.length === 0) {
      setLines((l) => (l.length ? [] : l));
      return;
    }
    const base = root.getBoundingClientRect();
    const rect = (id: string) => {
      const el = findNode(root, id);
      if (!el) return null;
      const r = el.getBoundingClientRect();
      return {
        x: (r.left - base.left) / zoom,
        y: (r.top - base.top) / zoom,
        w: r.width / zoom,
        h: r.height / zoom,
      };
    };
    const out: Array<{ key: string; d: string; x: number; y: number }> = [];
    for (const l of dottedLines) {
      const m = rect(l.to);
      const e = rect(l.from);
      if (!m || !e || (m.w === 0 && e.w === 0)) continue;
      const x1 = m.x + m.w / 2;
      const y1 = m.y + m.h;
      const x2 = e.x + e.w / 2;
      const y2 = e.y;
      const dy = Math.max(30, Math.abs(y2 - y1) / 2);
      out.push({
        key: `${l.from}-${l.to}`,
        d: `M${x1} ${y1} C${x1} ${y1 + dy} ${x2} ${y2 - dy} ${x2} ${y2}`,
        x: x2,
        y: y2,
      });
    }
    setLines(out);
    setSize({ w: root.scrollWidth, h: root.scrollHeight });
  }, [dottedLines, zoom]);
  useLayoutEffect(() => {
    measure();
  }, [measure, expanded, nodes]);
  useEffect(() => {
    const root = inner.current;
    if (!root || typeof ResizeObserver === 'undefined') return;
    const ro = new ResizeObserver(() => measure());
    ro.observe(root);
    return () => ro.disconnect();
  }, [measure]);

  // Drag a card onto a new manager.
  const [dragging, setDragging] = useState<string | null>(null);
  const [dropTarget, setDropTarget] = useState<string | null>(null);
  const isInside = (maybeAncestor: string, of: string) =>
    of === maybeAncestor || ancestors(of).includes(maybeAncestor);
  const canDropOn = (target: string) =>
    dragging !== null &&
    !isInside(dragging, target) &&
    byId.get(dragging)?.parentId !== target &&
    byId.get(target)?.kind !== 'vacancy';

  const renderItem = (n: OrgChartNode, level: number, seen: Set<string>): ReactNode => {
    const kids = seen.has(n.id) ? [] : (children.get(n.id) ?? []);
    const branch = hasChildren(n);
    const open = branch && expanded.has(n.id);
    const count = Math.max(kids.length, n.childCount ?? 0);
    const vacancy = n.kind === 'vacancy';
    const nextSeen = new Set(seen).add(n.id);
    return (
      <li key={n.id} role="none" className="mn-org-chart__branch">
        <div
          // No selection model: the tree only navigates, so aria-selected is not set (APG tree view).
          // eslint-disable-next-line jsx-a11y/role-has-required-aria-props
          role="treeitem"
          aria-level={level}
          aria-expanded={branch ? open : undefined}
          aria-busy={loading === n.id || undefined}
          aria-current={n.id === focusId ? 'true' : undefined}
          tabIndex={n.id === activeId ? 0 : -1}
          data-node-id={n.id}
          className="mn-org-chart__node"
          data-kind={vacancy ? 'vacancy' : 'person'}
          data-dashed={n.dashed || undefined}
          data-focus={n.id === focusId || undefined}
          data-dragging={dragging === n.id || undefined}
          data-drop={dropTarget === n.id || undefined}
          draggable={onMoveRequest !== undefined && !vacancy ? true : undefined}
          onFocus={() => setActive(n.id)}
          onClick={() => onSelect?.(n)}
          onKeyDown={(e) => onKeyDown(e, n)}
          onDragStart={(e: DragEvent) => {
            e.dataTransfer.setData('text/plain', n.id);
            e.dataTransfer.effectAllowed = 'move';
            setDragging(n.id);
          }}
          onDragEnd={() => {
            setDragging(null);
            setDropTarget(null);
          }}
          onDragOver={(e: DragEvent) => {
            if (!canDropOn(n.id)) return;
            e.preventDefault();
            e.stopPropagation();
            setDropTarget(n.id);
          }}
          onDragLeave={() => setDropTarget((t) => (t === n.id ? null : t))}
          onDrop={(e: DragEvent) => {
            e.preventDefault();
            e.stopPropagation();
            const moved = byId.get(dragging ?? e.dataTransfer.getData('text/plain'));
            if (moved && canDropOn(n.id)) onMoveRequest?.(moved, n.id);
            setDragging(null);
            setDropTarget(null);
          }}
        >
          {renderNode ? (
            renderNode(n)
          ) : (
            <>
              <span className="mn-org-chart__person">
                {vacancy ? (
                  <span className="mn-org-chart__vacancy-icon" aria-hidden>
                    <BriefcaseGlyph />
                  </span>
                ) : (
                  <Avatar name={n.name} src={n.avatarSrc ?? null} size="md" decorative />
                )}
                <span className="mn-org-chart__text">
                  <span className="mn-org-chart__name">{n.name}</span>
                  {n.title ? <span className="mn-org-chart__title">{n.title}</span> : null}
                </span>
              </span>
              {n.meta ? <span className="mn-org-chart__meta">{n.meta}</span> : null}
            </>
          )}
          {vacancy ? <span className="mn-visually-hidden">, {messages.vacant}</span> : null}
          {n.dashed ? <span className="mn-visually-hidden">, {messages.dashed}</span> : null}
          {count > 0 ? (
            <span className="mn-visually-hidden">, {messages.reports(count)}</span>
          ) : null}
        </div>
        {branch ? (
          // Mouse shortcut; keyboard users expand with the arrow keys on the card.
          <button
            type="button"
            className="mn-org-chart__toggle"
            tabIndex={-1}
            aria-hidden
            data-open={open || undefined}
            disabled={loading === n.id}
            onClick={() => void toggle(n)}
          >
            <ChevronRightGlyph />
            {count}
          </button>
        ) : null}
        {open && kids.length > 0 ? (
          <ul role="group" className="mn-org-chart__group">
            {kids.map((k) => renderItem(k, level + 1, nextSeen))}
          </ul>
        ) : null}
      </li>
    );
  };

  const percent = Math.round(zoom * 100);

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-org-chart', className)}
      data-layout={layout}
      data-dragging={dragging ? '' : undefined}
    >
      {zoomable || toolbar || onMoveRequest ? (
        <div className="mn-org-chart__toolbar">
          {onMoveRequest ? <p className="mn-org-chart__hint">{messages.moveHint}</p> : null}
          {toolbar}
          {zoomable ? (
            <div className="mn-org-chart__zoom" role="group" aria-label={messages.zoom}>
              <IconButton
                size="sm"
                label={messages.zoomOut}
                icon={<MinusGlyph />}
                disabled={zoom <= ZOOM_MIN}
                onClick={() => setZoom((z) => clampZoom(z - 0.1))}
              />
              <span className="mn-org-chart__zoom-level" aria-live="polite">
                {messages.zoomLevel(percent)}
              </span>
              <IconButton
                size="sm"
                label={messages.zoomIn}
                icon={<PlusMarkGlyph />}
                disabled={zoom >= ZOOM_MAX}
                onClick={() => setZoom((z) => clampZoom(z + 0.1))}
              />
              <IconButton
                size="sm"
                label={messages.zoomReset}
                icon={<ResetGlyph />}
                disabled={zoom === 1}
                onClick={() => setZoom(1)}
              />
            </div>
          ) : null}
        </div>
      ) : null}
      {/* Pointer panning and wheel zoom only; the tree inside is the keyboard path. */}
      {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions */}
      <div
        ref={scroller}
        className="mn-org-chart__viewport"
        onPointerDown={(e: PointerEvent<HTMLDivElement>) => {
          if (e.pointerType === 'touch' || e.button !== 0) return;
          if ((e.target as HTMLElement).closest('[role="treeitem"],button')) return;
          const el = scroller.current;
          if (!el) return;
          pan.current = {
            x: e.clientX,
            y: e.clientY,
            left: el.scrollLeft,
            top: el.scrollTop,
            id: e.pointerId,
          };
          el.setPointerCapture?.(e.pointerId);
        }}
        onPointerMove={(e: PointerEvent<HTMLDivElement>) => {
          const p = pan.current;
          const el = scroller.current;
          if (!p || !el || p.id !== e.pointerId) return;
          el.scrollLeft = p.left - (e.clientX - p.x);
          el.scrollTop = p.top - (e.clientY - p.y);
        }}
        onPointerUp={() => (pan.current = null)}
        onPointerCancel={() => (pan.current = null)}
        onWheel={(e) => {
          if (!zoomable || (!e.ctrlKey && !e.metaKey)) return;
          e.preventDefault();
          setZoom((z) => clampZoom(z - Math.sign(e.deltaY) * 0.1));
        }}
        onDragOver={(e: DragEvent) => {
          if (dragging && e.target === e.currentTarget && byId.get(dragging)?.parentId)
            e.preventDefault();
        }}
        onDrop={(e: DragEvent) => {
          if (e.target !== e.currentTarget) return;
          e.preventDefault();
          const moved = dragging ? byId.get(dragging) : undefined;
          if (moved?.parentId) onMoveRequest?.(moved, null);
          setDragging(null);
          setDropTarget(null);
        }}
      >
        <div
          ref={inner}
          className="mn-org-chart__canvas"
          style={zoom === 1 ? undefined : ({ '--mn-org-chart-zoom': zoom } as CSSProperties)}
        >
          {lines.length > 0 ? (
            <svg
              className="mn-org-chart__dotted"
              width={size.w}
              height={size.h}
              aria-hidden
              focusable={false}
            >
              {lines.map((l) => (
                <g key={l.key}>
                  <path d={l.d} />
                  <circle cx={l.x} cy={l.y} r={3} />
                </g>
              ))}
            </svg>
          ) : null}
          <ul
            ref={treeRef}
            // eslint-disable-next-line jsx-a11y/no-noninteractive-element-to-interactive-role
            role="tree"
            aria-label={messages.label}
            aria-describedby={instructionsId}
            className="mn-org-chart__tree"
          >
            {roots.map((r) => renderItem(r, 1, new Set()))}
          </ul>
        </div>
      </div>
      <p id={instructionsId} className="mn-visually-hidden">
        {messages.instructions}
      </p>
      <span className="mn-visually-hidden" role="status" aria-live="polite">
        {announcement}
      </span>
    </div>
  );
});

function findNode(root: HTMLElement | null, id: string): HTMLElement | null {
  if (!root) return null;
  for (const el of root.querySelectorAll<HTMLElement>('[data-node-id]')) {
    if (el.dataset.nodeId === id) return el;
  }
  return null;
}

function BriefcaseGlyph() {
  return (
    <svg
      viewBox="0 0 16 16"
      width="16"
      height="16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinejoin="round"
      aria-hidden
      focusable={false}
    >
      <rect x="2" y="5" width="12" height="8.5" rx="1.5" />
      <path d="M6 5V3.5h4V5M2 9h12" />
    </svg>
  );
}
function MinusGlyph() {
  return (
    <svg
      viewBox="0 0 16 16"
      width="16"
      height="16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      aria-hidden
      focusable={false}
    >
      <path d="M4 8h8" />
    </svg>
  );
}
function PlusMarkGlyph() {
  return (
    <svg
      viewBox="0 0 16 16"
      width="16"
      height="16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      aria-hidden
      focusable={false}
    >
      <path d="M4 8h8M8 4v8" />
    </svg>
  );
}
function ResetGlyph() {
  return (
    <svg
      viewBox="0 0 16 16"
      width="16"
      height="16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable={false}
    >
      <path d="M3 8a5 5 0 1 0 1.5-3.5M3 2.5v2.5h2.5" />
    </svg>
  );
}
