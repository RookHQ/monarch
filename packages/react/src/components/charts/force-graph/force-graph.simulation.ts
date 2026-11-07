/**
 * A small, dependency-free force layout (in the spirit of d3-force): link
 * springs, many-body repulsion, collision, centring and a weak pull to the
 * middle so unlinked nodes stay in view. It is deterministic — the same input
 * always gives the same positions — because nodes start on a phyllotaxis
 * spiral and the simulation runs a fixed number of ticks synchronously.
 * Repulsion is O(n²) per tick, comfortable up to a few hundred nodes.
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
  /** Simulation steps. Default 300. */
  ticks?: number | undefined;
}

/** A node position, centred around (0, 0). */
export interface ForceLayoutPoint {
  x: number;
  y: number;
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
    ticks = 300,
  }: ForceLayoutOptions = {},
): Map<string, ForceLayoutPoint> {
  const n = nodes.length;
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
        const w = (charge * alpha) / l2;
        a.vx += dx * w;
        a.vy += dy * w;
        b.vx -= dx * w;
        b.vy -= dy * w;
      }
    }

    // Weak gravity to the centre.
    for (const b of bodies) {
      b.vx -= b.x * gravity * alpha;
      b.vy -= b.y * gravity * alpha;
    }

    // Collision: push overlapping nodes apart.
    for (let i = 0; i < n; i++) {
      const a = bodies[i] as Body;
      for (let j = i + 1; j < n; j++) {
        const b = bodies[j] as Body;
        const dx = b.x + b.vx - a.x - a.vx;
        const dy = b.y + b.vy - a.y - a.vy;
        const l2 = dx * dx + dy * dy;
        if (l2 >= collide * collide) continue;
        const l = Math.sqrt(l2) || 1e-6;
        const push = ((collide - l) / l) * 0.35;
        a.vx -= dx * push;
        a.vy -= dy * push;
        b.vx += dx * push;
        b.vy += dy * push;
      }
    }

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
