'use client';

import {
  forwardRef,
  useId,
  useMemo,
  useRef,
  useState,
  type FocusEvent,
  type KeyboardEvent,
  type MouseEvent,
  type PointerEvent,
} from 'react';
import { DashGlyph, PlusGlyph } from '../../../internal/icons.js';
import {
  ChartLegend,
  ChartLiveRegion,
  ChartTooltip,
  DEFAULT_INTERACTION_LABELS,
  colorStyle,
  defaultChartColor,
  truncateLabel,
  useControllable,
  useElementWidth,
  type ChartColor,
  type ChartInteractionLabels,
  type ChartTableData,
} from '../../../internal/charts/index.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { ChartFrame, type ChartFrameLabels, type ChartFrameProps } from '../chart-frame/index.js';
import {
  forceLayout,
  type ForceLayoutOptions,
  type ForceLayoutPoint,
} from './force-graph.simulation.js';

export {
  forceLayout,
  type ForceLayoutEdge,
  type ForceLayoutNode,
  type ForceLayoutOptions,
  type ForceLayoutPoint,
} from './force-graph.simulation.js';

/** A node of the graph. */
export interface ForceGraphNode {
  /** Stable id, referenced by edges. */
  id: string;
  /** Name drawn under the node and used in the tooltip, table and announcements. */
  label: string;
  /** Key of the node's group (`groups`), which sets its colour. */
  group?: string | undefined;
  /** One line shown in the tooltip, e.g. the space or type. */
  description?: string | undefined;
}

/** A link between two nodes. */
export interface ForceGraphEdge {
  /** Id of one end. */
  source: string;
  /** Id of the other end. */
  target: string;
  /** Key of the link's kind (`edgeKinds`), which sets its colour and dash. */
  kind?: string | undefined;
}

/** A group of nodes, drawn in one palette colour and listed in the legend. */
export interface ForceGraphGroup {
  key: string;
  label: string;
  /** Palette slot. Default: the group's position (1, 2, …; the sixth and later are neutral). */
  color?: ChartColor | undefined;
}

/** A kind of link, listed in the legend with its line style. */
export interface ForceGraphEdgeKind {
  key: string;
  label: string;
  /** Palette slot or `neutral` (default). */
  color?: ChartColor | undefined;
  /** Dashed line as a second encoding besides colour. */
  dashed?: boolean | undefined;
}

/** Words the graph adds to ChartFrame's; pass translations here. */
export interface ForceGraphLabels extends ChartFrameLabels, Partial<ChartInteractionLabels> {
  /** Zoom-in button. Default "Zoom in". */
  zoomIn?: string | undefined;
  /** Zoom-out button. Default "Zoom out". */
  zoomOut?: string | undefined;
  /** Reset button. Default "Reset view". */
  resetView?: string | undefined;
  /** Table column for nodes. Default "Node". */
  node?: string | undefined;
  /** Table column and tooltip row for groups. Default "Group". */
  group?: string | undefined;
  /** Table column for linked nodes. Default "Connections". */
  connections?: string | undefined;
  /** Group name of nodes without a group. Default "No group". */
  noGroup?: string | undefined;
  /** Name of the legend of link kinds. Default "Link types". */
  edgeLegend?: string | undefined;
  /** "{count} connections" in announcements. */
  connectionCount?: ((count: number) => string) | undefined;
  /** Generated summary. */
  summary?: ((nodes: number, edges: number, groups: number) => string) | undefined;
}

export interface ForceGraphProps extends Omit<
  ChartFrameProps,
  'children' | 'legend' | 'legendMarker' | 'onLegendToggle' | 'table' | 'labels' | 'height'
> {
  nodes: readonly ForceGraphNode[];
  edges: readonly ForceGraphEdge[];
  /** Node groups, in legend order. Default: the distinct `group` keys of the nodes, labelled by key. */
  groups?: readonly ForceGraphGroup[] | undefined;
  /** Link kinds, listed in a second legend. */
  edgeKinds?: readonly ForceGraphEdgeKind[] | undefined;
  /** Height of the canvas in px. Default 420. */
  height?: number | undefined;
  /** Tunes the force layout (link distance, repulsion, collision, gravity, ticks). */
  layoutOptions?: ForceLayoutOptions | undefined;
  /** Draw node labels. Default `true`. */
  showLabels?: boolean | undefined;
  /** Node marked as selected (for example the open document). */
  selectedId?: string | null | undefined;
  /** Controlled inspected node (`null` for none). */
  activeId?: string | null | undefined;
  onActiveIdChange?: ((id: string | null) => void) | undefined;
  /** Click, Enter or Space on a node, e.g. to open it. */
  onNodeSelect?: ((node: ForceGraphNode) => void) | undefined;
  /** Replaces the generated text summary (the canvas's accessible name). */
  summary?: string | undefined;
  labels?: ForceGraphLabels | undefined;
}

const FALLBACK_WIDTH = 640;
const PAD = 36;
const R = 9;
const ZOOM_MIN = 0.35;
const ZOOM_MAX = 3;

interface View {
  k: number;
  x: number;
  y: number;
}

type Drag =
  | { type: 'node'; id: string; pointer: number; moved: boolean; sx: number; sy: number }
  | { type: 'pan'; pointer: number; sx: number; sy: number; ox: number; oy: number };

/**
 * A network of linked nodes laid out by a small built-in force simulation
 * (no dependency), inside ChartFrame: groups in palette colours, link kinds
 * with dashes, drag to move nodes and pan, zoom buttons, arrow-key
 * navigation between nodes, drill-in and a table listing every node's links.
 */
export const ForceGraph = forwardRef<HTMLElement, ForceGraphProps>(function ForceGraph(
  {
    nodes,
    edges,
    groups: groupsProp,
    edgeKinds = [],
    height = 420,
    layoutOptions,
    showLabels = true,
    selectedId,
    activeId,
    onActiveIdChange,
    onNodeSelect,
    summary,
    labels,
    status = 'ready',
    footer,
    ...frame
  },
  ref,
) {
  const hintId = useId();
  const il = {
    ...DEFAULT_INTERACTION_LABELS,
    chart: 'graph',
    keyboardHint:
      'Use the arrow keys to move between nodes, Enter to select one, plus and minus to zoom, 0 to reset.',
    ...labels,
  };
  const words = {
    zoomIn: labels?.zoomIn ?? 'Zoom in',
    zoomOut: labels?.zoomOut ?? 'Zoom out',
    resetView: labels?.resetView ?? 'Reset view',
    node: labels?.node ?? 'Node',
    group: labels?.group ?? 'Group',
    connections: labels?.connections ?? 'Connections',
    noGroup: labels?.noGroup ?? 'No group',
    edgeLegend: labels?.edgeLegend ?? 'Link types',
    count:
      labels?.connectionCount ?? ((c: number) => `${c} ${c === 1 ? 'connection' : 'connections'}`),
  };
  const [measureRef, measured] = useElementWidth<HTMLDivElement>();
  const width = measured ?? FALLBACK_WIDTH;

  // Groups and colours.
  const groups = useMemo<ForceGraphGroup[]>(() => {
    if (groupsProp) return [...groupsProp];
    const keys: string[] = [];
    for (const n of nodes) if (n.group !== undefined && !keys.includes(n.group)) keys.push(n.group);
    return keys.map((key) => ({ key, label: key }));
  }, [groupsProp, nodes]);
  const groupColor = new Map<string, ChartColor>(
    groups.map((g, i) => [g.key, g.color ?? defaultChartColor(i)]),
  );
  const groupLabel = new Map(groups.map((g) => [g.key, g.label]));
  const kinds = new Map(edgeKinds.map((k) => [k.key, k]));

  // Valid edges and adjacency.
  const ids = useMemo(() => new Set(nodes.map((n) => n.id)), [nodes]);
  const links = useMemo(
    () => edges.filter((e) => ids.has(e.source) && ids.has(e.target) && e.source !== e.target),
    [edges, ids],
  );
  const neighbours = useMemo(() => {
    const m = new Map<string, { id: string; kind: string | undefined }[]>();
    for (const n of nodes) m.set(n.id, []);
    for (const e of links) {
      m.get(e.source)?.push({ id: e.target, kind: e.kind });
      m.get(e.target)?.push({ id: e.source, kind: e.kind });
    }
    return m;
  }, [nodes, links]);
  const byId = useMemo(() => new Map(nodes.map((n) => [n.id, n])), [nodes]);
  const order = useMemo(
    () => [...nodes].sort((a, b) => a.label.localeCompare(b.label)).map((n) => n.id),
    [nodes],
  );

  // Layout (abstract coordinates) and the fit into the canvas.
  const layout = useMemo(
    () => forceLayout(nodes, links, layoutOptions),
    // Options are usually an inline object: compare by value.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [nodes, links, JSON.stringify(layoutOptions ?? {})],
  );
  const [moved, setMoved] = useState<ReadonlyMap<string, ForceLayoutPoint>>(new Map());
  const point = (id: string): ForceLayoutPoint => moved.get(id) ?? layout.get(id) ?? { x: 0, y: 0 };
  let minX = Infinity;
  let maxX = -Infinity;
  let minY = Infinity;
  let maxY = -Infinity;
  for (const p of layout.values()) {
    minX = Math.min(minX, p.x);
    maxX = Math.max(maxX, p.x);
    minY = Math.min(minY, p.y);
    maxY = Math.max(maxY, p.y);
  }
  if (!Number.isFinite(minX)) minX = maxX = minY = maxY = 0;
  const fit = Math.min(
    (width - 2 * PAD) / Math.max(1, maxX - minX),
    (height - 2 * PAD - (showLabels ? 14 : 0)) / Math.max(1, maxY - minY),
    1.5,
  );
  const cx0 = (minX + maxX) / 2;
  const cy0 = (minY + maxY) / 2;
  const [view, setView] = useState<View>({ k: 1, x: 0, y: 0 });
  const toScreen = (p: ForceLayoutPoint) => ({
    x: width / 2 + (p.x - cx0) * fit * view.k + view.x,
    y: height / 2 + (p.y - cy0) * fit * view.k + view.y,
  });
  const fromScreen = (x: number, y: number): ForceLayoutPoint => ({
    x: (x - width / 2 - view.x) / (fit * view.k) + cx0,
    y: (y - height / 2 - view.y) / (fit * view.k) + cy0,
  });
  const zoom = (factor: number) =>
    setView((v) => {
      const k = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, v.k * factor));
      const r = k / v.k;
      return { k, x: v.x * r, y: v.y * r };
    });
  const reset = () => {
    setView({ k: 1, x: 0, y: 0 });
    setMoved(new Map());
  };

  // Inspection.
  const [active, setActive] = useControllable<string | null>(activeId, null, onActiveIdChange);
  const [source, setSource] = useState<'pointer' | 'keyboard'>('pointer');
  const current = active !== null && byId.has(active) ? active : null;
  const interactive = status === 'ready' && nodes.length > 0;
  const activate = (id: string | null, from: 'pointer' | 'keyboard') => {
    setSource(from);
    setActive(id);
    if (id === null || from !== 'keyboard') return;
    // Keep the node in view when the keyboard moves to it.
    const s = toScreen(point(id));
    const dx = s.x < PAD ? PAD - s.x : s.x > width - PAD ? width - PAD - s.x : 0;
    const dy = s.y < PAD ? PAD - s.y : s.y > height - PAD ? height - PAD - s.y : 0;
    if (dx || dy) setView((v) => ({ ...v, x: v.x + dx, y: v.y + dy }));
  };

  const nearest = (from: string, dir: 'left' | 'right' | 'up' | 'down') => {
    const a = point(from);
    let best: string | null = null;
    let score = Infinity;
    for (const n of nodes) {
      if (n.id === from) continue;
      const b = point(n.id);
      const dx = b.x - a.x;
      const dy = b.y - a.y;
      const primary = dir === 'right' ? dx : dir === 'left' ? -dx : dir === 'down' ? dy : -dy;
      const cross = Math.abs(dir === 'left' || dir === 'right' ? dy : dx);
      if (primary <= 0 || cross > primary * 2) continue;
      const s = primary + cross * 2;
      if (s < score) {
        score = s;
        best = n.id;
      }
    }
    return best;
  };

  const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    if (!interactive || e.target !== e.currentTarget) return;
    const dirs: Record<string, 'left' | 'right' | 'up' | 'down'> = {
      ArrowLeft: 'left',
      ArrowRight: 'right',
      ArrowUp: 'up',
      ArrowDown: 'down',
    };
    const dir = dirs[e.key];
    if (dir) {
      const next = current === null ? (order[0] ?? null) : nearest(current, dir);
      if (next !== null) activate(next, 'keyboard');
    } else if (e.key === 'Home') {
      activate(order[0] ?? null, 'keyboard');
    } else if (e.key === 'End') {
      activate(order[order.length - 1] ?? null, 'keyboard');
    } else if (e.key === 'Escape') {
      if (current === null) return;
      setActive(null);
    } else if (e.key === 'Enter' || e.key === ' ') {
      if (current === null || !onNodeSelect) return;
      const node = byId.get(current);
      if (node) onNodeSelect(node);
    } else if (e.key === '+' || e.key === '=') {
      zoom(1.25);
    } else if (e.key === '-' || e.key === '_') {
      zoom(0.8);
    } else if (e.key === '0') {
      reset();
    } else return;
    e.preventDefault();
  };

  // Pointer: drag a node to move it, drag the background to pan.
  const drag = useRef<Drag | null>(null);
  const suppressClick = useRef(false);
  const local = (e: PointerEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const scale = rect.width > 0 ? width / rect.width : 1;
    return { x: (e.clientX - rect.left) * scale, y: (e.clientY - rect.top) * scale };
  };
  const nodeOf = (e: { target: EventTarget }) =>
    (e.target as Element).closest?.('[data-node]')?.getAttribute('data-node') ?? null;
  const onPointerDown = (e: PointerEvent<HTMLDivElement>) => {
    if (!interactive || e.button > 0) return;
    const id = nodeOf(e);
    const p = local(e);
    drag.current = id
      ? { type: 'node', id, pointer: e.pointerId, moved: false, sx: p.x, sy: p.y }
      : { type: 'pan', pointer: e.pointerId, sx: p.x, sy: p.y, ox: view.x, oy: view.y };
    if (id) activate(id, 'pointer');
    e.currentTarget.setPointerCapture?.(e.pointerId);
  };
  const onPointerMove = (e: PointerEvent<HTMLDivElement>) => {
    const d = drag.current;
    const p = local(e);
    if (d && d.pointer === e.pointerId) {
      if (d.type === 'node') {
        if (!d.moved && Math.hypot(p.x - d.sx, p.y - d.sy) < 3) return;
        d.moved = true;
        const at = fromScreen(p.x, p.y);
        setMoved((m) => new Map(m).set(d.id, at));
      } else {
        setView((v) => ({ ...v, x: d.ox + p.x - d.sx, y: d.oy + p.y - d.sy }));
      }
      return;
    }
    const id = nodeOf(e);
    if (id) activate(id, 'pointer');
    else if (e.pointerType === 'mouse' && source === 'pointer' && current !== null) setActive(null);
  };
  const endDrag = (e: PointerEvent<HTMLDivElement>) => {
    const d = drag.current;
    if (!d || d.pointer !== e.pointerId) return;
    drag.current = null;
    e.currentTarget.releasePointerCapture?.(e.pointerId);
    if (d.type === 'node' && d.moved) suppressClick.current = true;
  };

  // Text.
  const nodeGroup = (n: ForceGraphNode) =>
    n.group !== undefined ? (groupLabel.get(n.group) ?? n.group) : words.noGroup;
  const linkText = (id: string) =>
    (neighbours.get(id) ?? []).map((l) => {
      const name = byId.get(l.id)?.label ?? l.id;
      const kind = l.kind !== undefined ? kinds.get(l.kind)?.label : undefined;
      return kind ? `${name} (${kind})` : name;
    });
  const describe = (id: string) => {
    const n = byId.get(id);
    if (!n) return '';
    const list = linkText(id);
    return `${n.label}, ${nodeGroup(n)}. ${words.count(list.length)}${
      list.length ? `: ${list.join(', ')}` : ''
    }`;
  };
  let hub: string | null = null;
  let hubDegree = 0;
  for (const n of nodes) {
    const d = neighbours.get(n.id)?.length ?? 0;
    if (d > hubDegree) {
      hub = n.id;
      hubDegree = d;
    }
  }
  const usedGroups = new Set(nodes.map((n) => n.group).filter((g) => g !== undefined)).size;
  const generated = labels?.summary
    ? labels.summary(nodes.length, links.length, usedGroups)
    : `${nodes.length} ${nodes.length === 1 ? 'node' : 'nodes'} and ${links.length} ${
        links.length === 1 ? 'link' : 'links'
      }${usedGroups ? ` in ${usedGroups} ${usedGroups === 1 ? 'group' : 'groups'}` : ''}.${
        hub ? ` Most connected: ${byId.get(hub)?.label} (${hubDegree}).` : ''
      }`;
  const table: ChartTableData = {
    columns: [
      { key: 'node', label: words.node },
      { key: 'group', label: words.group, align: 'start' },
      { key: 'links', label: words.connections, align: 'start' },
    ],
    rows: order.flatMap((id) => {
      const n = byId.get(id);
      return n ? [{ node: n.label, group: nodeGroup(n), links: linkText(id).join(', ') }] : [];
    }),
  };

  const lit = new Set<string>();
  if (current !== null) {
    lit.add(current);
    for (const l of neighbours.get(current) ?? []) lit.add(l.id);
  }
  const legend = groups
    .filter((g) => nodes.some((n) => n.group === g.key))
    .map((g) => ({ key: g.key, label: g.label, color: groupColor.get(g.key) ?? 'neutral' }));
  const usedKinds = edgeKinds.filter((k) => links.some((e) => e.kind === k.key));
  const resolvedStatus = status === 'ready' && nodes.length === 0 ? 'empty' : status;
  const activeNode = current !== null ? byId.get(current) : undefined;
  const activePos = current !== null ? toScreen(point(current)) : null;

  const plotProps = {
    tabIndex: interactive ? 0 : -1,
    onKeyDown: onKeyDown,
    onPointerDown: onPointerDown,
    onPointerMove: onPointerMove,
    onPointerUp: endDrag,
    onPointerCancel: endDrag,
    onPointerLeave: (e: PointerEvent<HTMLDivElement>) => {
      if (!drag.current && e.pointerType === 'mouse' && source === 'pointer') {
        setActive(null);
      }
    },
    onFocus: (e: FocusEvent<HTMLDivElement>) => {
      if (e.target !== e.currentTarget || current !== null || !interactive) return;
      if (e.currentTarget.matches(':focus-visible')) activate(order[0] ?? null, 'keyboard');
    },
    onBlur: (e: FocusEvent<HTMLDivElement>) => {
      if (e.currentTarget.contains(e.relatedTarget as Node | null)) return;
      setActive(null);
    },
    onClick: (e: MouseEvent<HTMLDivElement>) => {
      if (suppressClick.current) {
        suppressClick.current = false;
        return;
      }
      const node = byId.get(nodeOf(e) ?? '');
      if (node && onNodeSelect) onNodeSelect(node);
    },
  };

  return (
    <ChartFrame
      ref={ref}
      {...frame}
      status={resolvedStatus}
      height={height}
      legend={legend.length > 1 ? legend : undefined}
      legendMarker="square"
      table={table}
      labels={labels}
      footer={
        usedKinds.length > 0 && resolvedStatus === 'ready' ? (
          <>
            <ChartLegend
              items={usedKinds.map((k) => ({
                key: k.key,
                label: k.label,
                color: k.color ?? 'neutral',
                dashed: k.dashed,
              }))}
              marker="line"
              label={words.edgeLegend}
            />
            {footer}
          </>
        ) : (
          footer
        )
      }
      data-chart="force-graph"
    >
      <div className="mn-force-graph">
        <div className="mn-force-graph__controls">
          <IconButton
            size="sm"
            variant="secondary"
            label={words.zoomIn}
            icon={<PlusGlyph />}
            disabled={view.k >= ZOOM_MAX}
            onClick={() => zoom(1.25)}
          />
          <IconButton
            size="sm"
            variant="secondary"
            label={words.zoomOut}
            icon={<DashGlyph />}
            disabled={view.k <= ZOOM_MIN}
            onClick={() => zoom(0.8)}
          />
          <Button size="sm" variant="secondary" onClick={reset}>
            {words.resetView}
          </Button>
        </div>
        <div
          ref={measureRef}
          className="mn-chart__plot mn-force-graph__canvas"
          role="group"
          aria-roledescription={il.chart}
          aria-label={summary ?? generated}
          aria-describedby={hintId}
          data-selectable={onNodeSelect ? '' : undefined}
          style={{ height }}
          {...plotProps}
        >
          <svg
            className="mn-chart__svg mn-force-graph__svg"
            viewBox={`0 0 ${width} ${height}`}
            height={height}
            aria-hidden="true"
            focusable="false"
          >
            <g className="mn-force-graph__edges">
              {links.map((e, i) => {
                const a = toScreen(point(e.source));
                const b = toScreen(point(e.target));
                const kind = e.kind !== undefined ? kinds.get(e.kind) : undefined;
                const on = current !== null && (e.source === current || e.target === current);
                return (
                  <line
                    key={i}
                    className="mn-force-graph__edge"
                    data-kind={e.kind}
                    data-dashed={kind?.dashed || undefined}
                    data-highlight={on ? '' : undefined}
                    data-dimmed={current !== null && !on ? '' : undefined}
                    style={colorStyle(kind?.color ?? 'neutral')}
                    x1={a.x}
                    y1={a.y}
                    x2={b.x}
                    y2={b.y}
                  />
                );
              })}
            </g>
            <g className="mn-force-graph__nodes">
              {nodes.map((n) => {
                const p = toScreen(point(n.id));
                const orphan = (neighbours.get(n.id)?.length ?? 0) === 0;
                const color: ChartColor = orphan
                  ? 'neutral'
                  : n.group !== undefined
                    ? (groupColor.get(n.group) ?? 'neutral')
                    : 'neutral';
                return (
                  <g
                    key={n.id}
                    className="mn-force-graph__node"
                    data-node={n.id}
                    data-group={n.group}
                    data-orphan={orphan ? '' : undefined}
                    data-active={n.id === current ? '' : undefined}
                    data-selected={n.id === selectedId ? '' : undefined}
                    data-dimmed={current !== null && !lit.has(n.id) ? '' : undefined}
                    transform={`translate(${Math.round(p.x * 10) / 10} ${Math.round(p.y * 10) / 10})`}
                    style={colorStyle(color)}
                  >
                    <circle className="mn-force-graph__hit" r={R + 8} />
                    <circle className="mn-force-graph__dot" r={R} />
                    {showLabels ? (
                      <text className="mn-force-graph__label" y={R + 13} textAnchor="middle">
                        {truncateLabel(n.label, 150)}
                      </text>
                    ) : null}
                  </g>
                );
              })}
            </g>
          </svg>
          {activeNode && activePos && (
            <ChartTooltip
              x={activePos.x + R}
              y={activePos.y - R}
              width={width}
              title={activeNode.label}
              rows={[
                {
                  key: 'group',
                  label: words.group,
                  value: nodeGroup(activeNode),
                  color:
                    activeNode.group !== undefined
                      ? (groupColor.get(activeNode.group) ?? 'neutral')
                      : 'neutral',
                },
                {
                  key: 'links',
                  label: words.connections,
                  value: String(neighbours.get(activeNode.id)?.length ?? 0),
                  color: 'neutral',
                },
              ]}
              footer={activeNode.description}
            />
          )}
          <span id={hintId} className="mn-visually-hidden">
            {il.keyboardHint}
          </span>
          <ChartLiveRegion>
            {current !== null && source === 'keyboard' ? describe(current) : ''}
          </ChartLiveRegion>
        </div>
      </div>
    </ChartFrame>
  );
});
