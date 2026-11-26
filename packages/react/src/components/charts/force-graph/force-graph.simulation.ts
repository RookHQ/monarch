/**
 * A small, dependency-free force layout (in the spirit of d3-force): link
 * springs, many-body repulsion, collision, centring and a weak pull to the
 * middle so unlinked nodes stay in view. It is deterministic — the same input
 * always gives the same positions — because nodes start on a phyllotaxis
 * spiral and the simulation runs a fixed number of ticks synchronously.
 *
 * Small graphs (up to `EXACT_LIMIT` nodes) use exact pairwise repulsion and
 * collision. Larger ones use a Barnes-Hut quadtree for repulsion and a grid
 * of `collide`-sized cells for collision, both O(n log n) per tick, and the
 * default number of ticks shrinks with size (`defaultTicks`).
 */

export interface ForceLayoutNode {
  id: string;
  /** Fixed position: the node is not moved by the forces. */
  fx?: number | undefined;
  fy?: number | undefined;
}

export interface ForceLayoutEdge {
  source: string;
  target: string;
}

export interface ForceLayoutOptions {
  /** Rest length of a link. Default 90. */
  linkDistance?: number | undefined;
  /** Repulsion between every pair of nodes (negative pushes apart). Default -260. */
  charge?: number | undefined;
  /** Minimum distance between node centres. Default 40. */
  collide?: number | undefined;
  /** Pull towards the centre for every node, 0–1. Default 0.04. */
  gravity?: number | undefined;
  /** Simulation steps. Default `defaultTicks(nodes.length)`: 300 up to 300 nodes, fewer above. */
  ticks?: number | undefined;
}

/** A node position, centred around (0, 0). */
export interface ForceLayoutPoint {
  x: number;
  y: number;
}

/** Up to this many nodes, repulsion and collision are exact (pairwise). */
export const EXACT_LIMIT = 300;

/** Default simulation steps for a graph of `n` nodes: 300, tapering to 60 for very large graphs. */
export function defaultTicks(n: number): number {
  if (n <= EXACT_LIMIT) return 300;
  return Math.max(60, Math.round((300 * EXACT_LIMIT) / n));
}

interface Body {
  x: number;
  y: number;
  vx: number;
  vy: number;
  fx: number | undefined;
  fy: number | undefined;
}

/**
 * Lays out a graph and returns each node's position by id. Edges whose ends
 * are not in `nodes` are ignored.
 */
export function forceLayout(
  nodes: readonly ForceLayoutNode[],
  edges: readonly ForceLayoutEdge[],
  {
    linkDistance = 90,
    charge = -260,
    collide = 40,
    gravity = 0.04,
    ticks = defaultTicks(nodes.length),
  }: ForceLayoutOptions = {},
): Map<string, ForceLayoutPoint> {
  const n = nodes.length;
  const exact = n <= EXACT_LIMIT;
  const index = new Map<string, number>();
  nodes.forEach((node, i) => index.set(node.id, i));
  const angle = Math.PI * (3 - Math.sqrt(5));
  const bodies: Body[] = nodes.map((node, i) => {
    const r = 10 * Math.sqrt(0.5 + i);
    const a = i * angle;
    return {
      x: node.fx ?? r * Math.cos(a),
      y: node.fy ?? r * Math.sin(a),
      vx: 0,
      vy: 0,
      fx: node.fx,
      fy: node.fy,
    };
  });

  const links: [number, number][] = [];
  const degree = new Array<number>(n).fill(0);
  for (const e of edges) {
    const s = index.get(e.source);
    const t = index.get(e.target);
    if (s === undefined || t === undefined || s === t) continue;
    links.push([s, t]);
    degree[s] = (degree[s] ?? 0) + 1;
    degree[t] = (degree[t] ?? 0) + 1;
  }

  const alphaMin = 0.001;
  const alphaDecay = 1 - Math.pow(alphaMin, 1 / Math.max(1, ticks));
  const velocityDecay = 0.6;
  let alpha = 1;

  for (let tick = 0; tick < ticks; tick++) {
    alpha += (0 - alpha) * alphaDecay;

    // Links: springs towards the rest length, weaker on busy nodes.
    for (const [si, ti] of links) {
      const s = bodies[si] as Body;
      const t = bodies[ti] as Body;
      let dx = t.x + t.vx - s.x - s.vx || 1e-6;
      let dy = t.y + t.vy - s.y - s.vy || 1e-6;
      const l = Math.sqrt(dx * dx + dy * dy);
      const ds = degree[si] ?? 1;
      const dt = degree[ti] ?? 1;
      const strength = 1 / Math.min(ds, dt);
      const k = ((l - linkDistance) / l) * alpha * strength;
      dx *= k;
      dy *= k;
      const bias = ds / (ds + dt);
      t.vx -= dx * bias;
      t.vy -= dy * bias;
      s.vx += dx * (1 - bias);
      s.vy += dy * (1 - bias);
    }

    // Many-body repulsion.
    if (exact) repelExact(bodies, charge * alpha);
    else repelBarnesHut(bodies, charge * alpha);

    // Weak gravity to the centre.
    for (const b of bodies) {
      b.vx -= b.x * gravity * alpha;
      b.vy -= b.y * gravity * alpha;
    }

    // Collision: push overlapping nodes apart.
    if (exact) collideExact(bodies, collide);
    else collideGrid(bodies, collide);

    // Integrate.
    for (const b of bodies) {
      if (b.fx !== undefined) {
        b.x = b.fx;
        b.vx = 0;
      } else {
        b.vx *= velocityDecay;
        b.x += b.vx;
      }
      if (b.fy !== undefined) {
        b.y = b.fy;
        b.vy = 0;
      } else {
        b.vy *= velocityDecay;
        b.y += b.vy;
      }
    }
  }

  // Centre the free nodes' mean on (0, 0) when nothing is pinned.
  const pinned = bodies.some((b) => b.fx !== undefined || b.fy !== undefined);
  let mx = 0;
  let my = 0;
  if (!pinned && n > 0) {
    for (const b of bodies) {
      mx += b.x;
      my += b.y;
    }
    mx /= n;
    my /= n;
  }
  const out = new Map<string, ForceLayoutPoint>();
  nodes.forEach((node, i) => {
    const b = bodies[i] as Body;
    out.set(node.id, {
      x: Math.round((b.x - mx) * 100) / 100,
      y: Math.round((b.y - my) * 100) / 100,
    });
  });
  return out;
}

/* ------------------------------------------------------------ repulsion */

/** Exact pairwise repulsion, O(n²). `strength` is charge × alpha. */
function repelExact(bodies: Body[], strength: number) {
  const n = bodies.length;
  for (let i = 0; i < n; i++) {
    const a = bodies[i] as Body;
    for (let j = i + 1; j < n; j++) {
      const b = bodies[j] as Body;
      let dx = b.x - a.x;
      let dy = b.y - a.y;
      if (dx === 0 && dy === 0) {
        // Deterministic jiggle for coincident nodes.
        dx = ((i * 7 + j * 13) % 10) / 10 - 0.45;
        dy = ((i * 11 + j * 5) % 10) / 10 - 0.55;
      }
      const l2 = Math.max(1, dx * dx + dy * dy);
      const w = strength / l2;
      a.vx += dx * w;
      a.vy += dy * w;
      b.vx -= dx * w;
      b.vy -= dy * w;
    }
  }
}

/** A quadtree cell: its bounds, the bodies' count and centre of mass, and children or a leaf body. */
interface Quad {
  x0: number;
  y0: number;
  size: number;
  count: number;
  cx: number;
  cy: number;
  children: (Quad | null)[] | null;
  /** Leaf bodies (more than one only when they coincide or the cell is tiny). */
  bodies: number[];
}

const THETA2 = 0.81; // Barnes-Hut opening criterion θ = 0.9, as in d3-force.
const MAX_DEPTH = 24;

function quad(x0: number, y0: number, size: number): Quad {
  return { x0, y0, size, count: 0, cx: 0, cy: 0, children: null, bodies: [] };
}

function insert(node: Quad, i: number, all: Body[], depth: number) {
  const b = all[i] as Body;
  node.cx = (node.cx * node.count + b.x) / (node.count + 1);
  node.cy = (node.cy * node.count + b.y) / (node.count + 1);
  node.count += 1;
  if (!node.children) {
    if (node.bodies.length === 0 || depth >= MAX_DEPTH) {
      node.bodies.push(i);
      return;
    }
    // Split the leaf and push its bodies down.
    const held = node.bodies;
    node.bodies = [];
    node.children = [null, null, null, null];
    for (const h of held) place(node, h, all, depth);
  }
  place(node, i, all, depth);
}

function place(node: Quad, i: number, all: Body[], depth: number) {
  const b = all[i] as Body;
  const half = node.size / 2;
  const right = b.x >= node.x0 + half ? 1 : 0;
  const bottom = b.y >= node.y0 + half ? 1 : 0;
  const k = right + bottom * 2;
  const children = node.children as (Quad | null)[];
  let child = children[k];
  if (!child) {
    child = quad(node.x0 + right * half, node.y0 + bottom * half, half);
    children[k] = child;
  }
  insert(child, i, all, depth + 1);
}

/** Barnes-Hut repulsion, O(n log n): far cells act as one body at their centre of mass. */
function repelBarnesHut(bodies: Body[], strength: number) {
  let x0 = Infinity;
  let y0 = Infinity;
  let x1 = -Infinity;
  let y1 = -Infinity;
  for (const b of bodies) {
    if (b.x < x0) x0 = b.x;
    if (b.y < y0) y0 = b.y;
    if (b.x > x1) x1 = b.x;
    if (b.y > y1) y1 = b.y;
  }
  const size = Math.max(1, x1 - x0, y1 - y0) + 1;
  const root = quad(x0, y0, size);
  for (let i = 0; i < bodies.length; i++) insert(root, i, bodies, 0);

  const stack: Quad[] = [];
  for (let i = 0; i < bodies.length; i++) {
    const a = bodies[i] as Body;
    stack.length = 0;
    stack.push(root);
    while (stack.length) {
      const node = stack.pop() as Quad;
      if (node.count === 0) continue;
      let dx = node.cx - a.x;
      let dy = node.cy - a.y;
      const l2 = dx * dx + dy * dy;
      const far = node.children && (node.size * node.size) / Math.max(1e-9, l2) < THETA2;
      if (far) {
        const w = (strength * node.count) / Math.max(1, l2);
        a.vx += dx * w;
        a.vy += dy * w;
        continue;
      }
      if (node.children) {
        for (const c of node.children) if (c) stack.push(c);
        continue;
      }
      for (const j of node.bodies) {
        if (j === i) continue;
        const b = bodies[j] as Body;
        dx = b.x - a.x;
        dy = b.y - a.y;
        if (dx === 0 && dy === 0) {
          dx = ((i * 7 + j * 13) % 10) / 10 - 0.45;
          dy = ((i * 11 + j * 5) % 10) / 10 - 0.55;
        }
        const w = strength / Math.max(1, dx * dx + dy * dy);
        a.vx += dx * w;
        a.vy += dy * w;
      }
    }
  }
}

/* ------------------------------------------------------------ collision */

function pushApart(a: Body, b: Body, collide: number) {
  const dx = b.x + b.vx - a.x - a.vx;
  const dy = b.y + b.vy - a.y - a.vy;
  const l2 = dx * dx + dy * dy;
  if (l2 >= collide * collide) return;
  const l = Math.sqrt(l2) || 1e-6;
  const push = ((collide - l) / l) * 0.35;
  a.vx -= dx * push;
  a.vy -= dy * push;
  b.vx += dx * push;
  b.vy += dy * push;
}

/** Exact pairwise collision, O(n²). */
function collideExact(bodies: Body[], collide: number) {
  const n = bodies.length;
  for (let i = 0; i < n; i++) {
    const a = bodies[i] as Body;
    for (let j = i + 1; j < n; j++) pushApart(a, bodies[j] as Body, collide);
  }
}

/** Collision through a grid of `collide`-sized cells: only neighbouring cells can overlap. */
function collideGrid(bodies: Body[], collide: number) {
  const cell = Math.max(1, collide);
  const grid = new Map<string, number[]>();
  const key = (gx: number, gy: number) => `${gx},${gy}`;
  bodies.forEach((b, i) => {
    const k = key(Math.floor(b.x / cell), Math.floor(b.y / cell));
    const list = grid.get(k);
    if (list) list.push(i);
    else grid.set(k, [i]);
  });
  for (let i = 0; i < bodies.length; i++) {
    const a = bodies[i] as Body;
    const gx = Math.floor(a.x / cell);
    const gy = Math.floor(a.y / cell);
    for (let ox = -1; ox <= 1; ox++) {
      for (let oy = -1; oy <= 1; oy++) {
        for (const j of grid.get(key(gx + ox, gy + oy)) ?? []) {
          if (j > i) pushApart(a, bodies[j] as Body, collide);
        }
      }
    }
  }
}

/**
 * A stable content key for a layout input: equal for graphs with the same
 * nodes (ids and pins), edges and options, whatever their object identity.
 */
export function layoutKey(
  nodes: readonly ForceLayoutNode[],
  edges: readonly ForceLayoutEdge[],
  options: ForceLayoutOptions | undefined,
): string {
  let key = '';
  for (const n of nodes) key += `${n.id}\u0001${n.fx ?? ''}\u0001${n.fy ?? ''}\u0002`;
  key += '\u0003';
  for (const e of edges) key += `${e.source}\u0001${e.target}\u0002`;
  return `${key}\u0003${JSON.stringify(options ?? {})}`;
}
