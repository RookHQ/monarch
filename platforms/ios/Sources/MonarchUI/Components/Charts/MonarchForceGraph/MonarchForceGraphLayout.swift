import Foundation

/// A small force layout in the spirit of d3-force, ported from the web
/// component so both platforms place a graph alike: link springs, many-body
/// repulsion, collision and a weak pull to the centre.
///
/// It is deterministic: nodes start on a phyllotaxis spiral and the
/// simulation runs a fixed number of steps. Up to `exactLimit` nodes,
/// repulsion and collision are pairwise; larger graphs use a Barnes-Hut
/// quadtree and a grid of collision cells.
enum MonarchForceGraphLayout {
    /// Up to this many nodes, repulsion and collision are exact.
    static let exactLimit = 300

    /// The default number of steps: 300, tapering to 60 for very large graphs.
    static func defaultTicks(_ count: Int) -> Int {
        guard count > exactLimit else { return 300 }
        return max(60, Int((300.0 * Double(exactLimit) / Double(count)).rounded()))
    }

    private struct Body {
        var x: Double
        var y: Double
        var vx = 0.0
        var vy = 0.0
    }

    /// Lays out the nodes and returns each one's position, centred on (0, 0).
    /// Links whose ends are not among the nodes, and links to the same node, are ignored.
    static func layout(
        ids: [String],
        links: [(String, String)],
        options: MonarchForceGraphLayoutOptions
    ) -> [String: CGPoint] {
        let count = ids.count
        let exact = count <= exactLimit
        var index: [String: Int] = [:]
        for (i, id) in ids.enumerated() where index[id] == nil {
            index[id] = i
        }
        let angle = Double.pi * (3 - 5.0.squareRoot())
        var bodies = (0..<count).map { i -> Body in
            let radius = 10 * (0.5 + Double(i)).squareRoot()
            let a = Double(i) * angle
            return Body(x: radius * cos(a), y: radius * sin(a))
        }
        var pairs: [(Int, Int)] = []
        var degree = [Int](repeating: 0, count: count)
        for (source, target) in links {
            guard let s = index[source], let t = index[target], s != t else { continue }
            pairs.append((s, t))
            degree[s] += 1
            degree[t] += 1
        }

        let ticks = max(0, options.ticks ?? defaultTicks(count))
        let alphaMin = 0.001
        let alphaDecay = 1 - pow(alphaMin, 1 / Double(max(1, ticks)))
        let velocityDecay = 0.6
        var alpha = 1.0

        for _ in 0..<ticks {
            alpha += (0 - alpha) * alphaDecay
            // Links: springs towards the rest length, weaker on busy nodes.
            for (si, ti) in pairs {
                var dx = bodies[ti].x + bodies[ti].vx - bodies[si].x - bodies[si].vx
                var dy = bodies[ti].y + bodies[ti].vy - bodies[si].y - bodies[si].vy
                if dx == 0 { dx = 1e-6 }
                if dy == 0 { dy = 1e-6 }
                let length = (dx * dx + dy * dy).squareRoot()
                let ds = Double(degree[si])
                let dt = Double(degree[ti])
                let strength = 1 / min(ds, dt)
                let k = (length - options.linkDistance) / length * alpha * strength
                dx *= k
                dy *= k
                let bias = ds / (ds + dt)
                bodies[ti].vx -= dx * bias
                bodies[ti].vy -= dy * bias
                bodies[si].vx += dx * (1 - bias)
                bodies[si].vy += dy * (1 - bias)
            }
            if exact {
                repelExact(&bodies, strength: options.charge * alpha)
            } else {
                repelBarnesHut(&bodies, strength: options.charge * alpha)
            }
            for i in bodies.indices {
                bodies[i].vx -= bodies[i].x * options.gravity * alpha
                bodies[i].vy -= bodies[i].y * options.gravity * alpha
            }
            if exact {
                collideExact(&bodies, distance: options.collide)
            } else {
                collideGrid(&bodies, distance: options.collide)
            }
            for i in bodies.indices {
                bodies[i].vx *= velocityDecay
                bodies[i].x += bodies[i].vx
                bodies[i].vy *= velocityDecay
                bodies[i].y += bodies[i].vy
            }
        }

        var meanX = 0.0
        var meanY = 0.0
        if count > 0 {
            meanX = bodies.reduce(0) { $0 + $1.x } / Double(count)
            meanY = bodies.reduce(0) { $0 + $1.y } / Double(count)
        }
        var out: [String: CGPoint] = [:]
        for (id, i) in index {
            out[id] = CGPoint(
                x: ((bodies[i].x - meanX) * 100).rounded() / 100,
                y: ((bodies[i].y - meanY) * 100).rounded() / 100
            )
        }
        return out
    }

    /// A deterministic nudge for two nodes at the same place.
    private static func jiggle(_ i: Int, _ j: Int) -> (Double, Double) {
        (Double((i * 7 + j * 13) % 10) / 10 - 0.45, Double((i * 11 + j * 5) % 10) / 10 - 0.55)
    }

    /// Pairwise repulsion. `strength` is the charge times alpha.
    private static func repelExact(_ bodies: inout [Body], strength: Double) {
        let count = bodies.count
        for i in 0..<count {
            for j in (i + 1)..<max(i + 1, count) {
                var dx = bodies[j].x - bodies[i].x
                var dy = bodies[j].y - bodies[i].y
                if dx == 0 && dy == 0 { (dx, dy) = jiggle(i, j) }
                let weight = strength / max(1, dx * dx + dy * dy)
                bodies[i].vx += dx * weight
                bodies[i].vy += dy * weight
                bodies[j].vx -= dx * weight
                bodies[j].vy -= dy * weight
            }
        }
    }

    /// A quadtree cell: its bounds, count, centre of mass, and children or leaf bodies.
    private final class Quad {
        let x0: Double
        let y0: Double
        let size: Double
        var count = 0
        var cx = 0.0
        var cy = 0.0
        var children: [Quad?]?
        var bodies: [Int] = []

        init(_ x0: Double, _ y0: Double, _ size: Double) {
            self.x0 = x0
            self.y0 = y0
            self.size = size
        }
    }

    /// The Barnes-Hut opening criterion squared, θ = 0.9 as in d3-force.
    private static let theta2 = 0.81
    /// The deepest a quadtree goes; deeper bodies share a leaf.
    private static let maxDepth = 24

    private static func insert(_ node: Quad, _ i: Int, _ all: [Body], _ depth: Int) {
        let body = all[i]
        node.cx = (node.cx * Double(node.count) + body.x) / Double(node.count + 1)
        node.cy = (node.cy * Double(node.count) + body.y) / Double(node.count + 1)
        node.count += 1
        if node.children == nil {
            if node.bodies.isEmpty || depth >= maxDepth {
                node.bodies.append(i)
                return
            }
            let held = node.bodies
            node.bodies = []
            node.children = [nil, nil, nil, nil]
            for h in held { place(node, h, all, depth) }
        }
        place(node, i, all, depth)
    }

    private static func place(_ node: Quad, _ i: Int, _ all: [Body], _ depth: Int) {
        let body = all[i]
        let half = node.size / 2
        let right = body.x >= node.x0 + half ? 1 : 0
        let bottom = body.y >= node.y0 + half ? 1 : 0
        let k = right + bottom * 2
        let child: Quad
        if let existing = node.children?[k] ?? nil {
            child = existing
        } else {
            child = Quad(node.x0 + Double(right) * half, node.y0 + Double(bottom) * half, half)
            node.children?[k] = child
        }
        insert(child, i, all, depth + 1)
    }

    /// Barnes-Hut repulsion: far cells act as one body at their centre of mass.
    private static func repelBarnesHut(_ bodies: inout [Body], strength: Double) {
        guard let first = bodies.first else { return }
        var x0 = first.x
        var y0 = first.y
        var x1 = first.x
        var y1 = first.y
        for body in bodies {
            x0 = min(x0, body.x)
            y0 = min(y0, body.y)
            x1 = max(x1, body.x)
            y1 = max(y1, body.y)
        }
        let root = Quad(x0, y0, max(1, x1 - x0, y1 - y0) + 1)
        let snapshot = bodies
        for i in snapshot.indices { insert(root, i, snapshot, 0) }
        var stack: [Quad] = []
        for i in bodies.indices {
            stack.removeAll(keepingCapacity: true)
            stack.append(root)
            while let node = stack.popLast() {
                guard node.count > 0 else { continue }
                var dx = node.cx - bodies[i].x
                var dy = node.cy - bodies[i].y
                let l2 = dx * dx + dy * dy
                if let children = node.children {
                    if node.size * node.size / max(1e-9, l2) < theta2 {
                        let weight = strength * Double(node.count) / max(1, l2)
                        bodies[i].vx += dx * weight
                        bodies[i].vy += dy * weight
                    } else {
                        stack.append(contentsOf: children.compactMap { $0 })
                    }
                    continue
                }
                for j in node.bodies where j != i {
                    dx = snapshot[j].x - bodies[i].x
                    dy = snapshot[j].y - bodies[i].y
                    if dx == 0 && dy == 0 { (dx, dy) = jiggle(i, j) }
                    let weight = strength / max(1, dx * dx + dy * dy)
                    bodies[i].vx += dx * weight
                    bodies[i].vy += dy * weight
                }
            }
        }
    }

    private static func pushApart(_ bodies: inout [Body], _ i: Int, _ j: Int, distance: Double) {
        let dx = bodies[j].x + bodies[j].vx - bodies[i].x - bodies[i].vx
        let dy = bodies[j].y + bodies[j].vy - bodies[i].y - bodies[i].vy
        let l2 = dx * dx + dy * dy
        guard l2 < distance * distance else { return }
        let length = l2.squareRoot() == 0 ? 1e-6 : l2.squareRoot()
        let push = (distance - length) / length * 0.35
        bodies[i].vx -= dx * push
        bodies[i].vy -= dy * push
        bodies[j].vx += dx * push
        bodies[j].vy += dy * push
    }

    /// Pairwise collision.
    private static func collideExact(_ bodies: inout [Body], distance: Double) {
        let count = bodies.count
        for i in 0..<count {
            for j in (i + 1)..<max(i + 1, count) {
                pushApart(&bodies, i, j, distance: distance)
            }
        }
    }

    /// Collision through a grid of cells as large as the collision distance:
    /// only neighbouring cells can overlap.
    private static func collideGrid(_ bodies: inout [Body], distance: Double) {
        let cell = max(1, distance)
        struct Key: Hashable {
            let x: Int
            let y: Int
        }
        var grid: [Key: [Int]] = [:]
        for (i, body) in bodies.enumerated() {
            grid[Key(x: Int((body.x / cell).rounded(.down)), y: Int((body.y / cell).rounded(.down))), default: []]
                .append(i)
        }
        for i in bodies.indices {
            let gx = Int((bodies[i].x / cell).rounded(.down))
            let gy = Int((bodies[i].y / cell).rounded(.down))
            for ox in -1...1 {
                for oy in -1...1 {
                    for j in grid[Key(x: gx + ox, y: gy + oy)] ?? [] where j > i {
                        pushApart(&bodies, i, j, distance: distance)
                    }
                }
            }
        }
    }
}
