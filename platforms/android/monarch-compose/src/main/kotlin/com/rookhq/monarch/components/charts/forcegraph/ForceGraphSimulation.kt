package com.rookhq.monarch.components.charts.forcegraph

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/** A position in the layout's own units, centred around (0, 0). */
internal data class ForceGraphPoint(val x: Double, val y: Double)

/**
 * A small force layout in the spirit of d3-force, ported from the web
 * `forceLayout`: link springs, many-body repulsion, collision and a weak pull
 * to the middle so unlinked nodes stay in view. It is deterministic: nodes
 * start on a phyllotaxis spiral and a fixed number of steps runs in one go.
 *
 * Up to [EXACT_LIMIT] nodes repulsion and collision are exact (pairwise).
 * Larger graphs use a Barnes-Hut quadtree for repulsion and a grid of
 * collide-sized cells for collision, and fewer steps by default.
 */
internal object ForceGraphSimulation {
    /** Up to this many nodes, repulsion and collision are exact. */
    const val EXACT_LIMIT: Int = 300

    private const val DEFAULT_TICKS = 300
    private const val MIN_TICKS = 60
    private const val ALPHA_MIN = 0.001
    private const val VELOCITY_DECAY = 0.6
    private const val SPIRAL_RADIUS = 10.0
    private const val SPIRAL_OFFSET = 0.5
    private const val GOLDEN = 5.0
    private const val GOLDEN_TURN = 3.0
    private const val COLLIDE_PUSH = 0.35
    private const val TINY = 1e-6
    private const val ROUND = 100.0

    /** The default number of steps for [n] nodes: 300, tapering to 60 for very large graphs. */
    fun defaultTicks(n: Int): Int =
        if (n <= EXACT_LIMIT) {
            DEFAULT_TICKS
        } else {
            max(MIN_TICKS, (DEFAULT_TICKS.toDouble() * EXACT_LIMIT / n).roundToLong().toInt())
        }

    /** One body of the simulation. */
    private class Body(var x: Double, var y: Double) {
        var vx = 0.0
        var vy = 0.0
    }

    /**
     * Lays out [ids] linked by [links] (pairs of indices into [ids]) and returns each position, in the order of
     * [ids]. Links must already be valid: both ends known and distinct.
     */
    fun layout(
        ids: List<String>,
        links: List<Pair<Int, Int>>,
        options: MonarchForceGraphLayoutOptions,
    ): List<ForceGraphPoint> {
        val n = ids.size
        val ticks = options.ticks ?: defaultTicks(n)
        val exact = n <= EXACT_LIMIT
        val angle = PI * (GOLDEN_TURN - sqrt(GOLDEN))
        val bodies = List(n) { i ->
            val r = SPIRAL_RADIUS * sqrt(SPIRAL_OFFSET + i)
            Body(r * cos(i * angle), r * sin(i * angle))
        }
        val degree = IntArray(n)
        for ((s, t) in links) {
            degree[s]++
            degree[t]++
        }
        val alphaDecay = 1 - ALPHA_MIN.pow(1.0 / max(1, ticks))
        var alpha = 1.0
        repeat(ticks) {
            alpha += (0 - alpha) * alphaDecay
            springs(bodies, links, degree, options.linkDistance, alpha)
            if (exact) repelExact(bodies, options.charge * alpha) else repelBarnesHut(bodies, options.charge * alpha)
            for (b in bodies) {
                b.vx -= b.x * options.gravity * alpha
                b.vy -= b.y * options.gravity * alpha
            }
            if (exact) collideExact(bodies, options.collide) else collideGrid(bodies, options.collide)
            for (b in bodies) {
                b.vx *= VELOCITY_DECAY
                b.x += b.vx
                b.vy *= VELOCITY_DECAY
                b.y += b.vy
            }
        }
        val mx = if (n > 0) bodies.sumOf { it.x } / n else 0.0
        val my = if (n > 0) bodies.sumOf { it.y } / n else 0.0
        return bodies.map { ForceGraphPoint(round(it.x - mx), round(it.y - my)) }
    }

    private fun round(value: Double): Double = (value * ROUND).roundToLong() / ROUND

    /** Links pull towards their rest length, more weakly on busy nodes. */
    private fun springs(
        bodies: List<Body>,
        links: List<Pair<Int, Int>>,
        degree: IntArray,
        distance: Double,
        alpha: Double,
    ) {
        for ((si, ti) in links) {
            val s = bodies[si]
            val t = bodies[ti]
            var dx = (t.x + t.vx - s.x - s.vx).let { if (it == 0.0) TINY else it }
            var dy = (t.y + t.vy - s.y - s.vy).let { if (it == 0.0) TINY else it }
            val l = sqrt(dx * dx + dy * dy)
            val ds = degree[si]
            val dt = degree[ti]
            val k = (l - distance) / l * alpha / minOf(ds, dt)
            dx *= k
            dy *= k
            val bias = ds.toDouble() / (ds + dt)
            t.vx -= dx * bias
            t.vy -= dy * bias
            s.vx += dx * (1 - bias)
            s.vy += dy * (1 - bias)
        }
    }

    /** A fixed nudge for two nodes at one place, so the result stays deterministic. */
    @Suppress("MagicNumber")
    private fun jiggle(i: Int, j: Int): Pair<Double, Double> =
        ((i * 7 + j * 13) % 10) / 10.0 - 0.45 to ((i * 11 + j * 5) % 10) / 10.0 - 0.55

    /** Exact pairwise repulsion. [strength] is the charge times alpha. */
    private fun repelExact(bodies: List<Body>, strength: Double) {
        for (i in bodies.indices) {
            val a = bodies[i]
            for (j in i + 1 until bodies.size) {
                val b = bodies[j]
                var dx = b.x - a.x
                var dy = b.y - a.y
                if (dx == 0.0 && dy == 0.0) {
                    val (jx, jy) = jiggle(i, j)
                    dx = jx
                    dy = jy
                }
                val w = strength / max(1.0, dx * dx + dy * dy)
                a.vx += dx * w
                a.vy += dy * w
                b.vx -= dx * w
                b.vy -= dy * w
            }
        }
    }

    /** A quadtree cell: its bounds, the count and centre of mass of its bodies, and children or leaf bodies. */
    private class Quad(val x0: Double, val y0: Double, val size: Double) {
        var count = 0
        var cx = 0.0
        var cy = 0.0
        var children: Array<Quad?>? = null
        val bodies = mutableListOf<Int>()
    }

    /** The Barnes-Hut opening criterion: theta 0.9 squared, as in d3-force. */
    private const val THETA2 = 0.81
    private const val MAX_DEPTH = 24
    private const val EPSILON2 = 1e-9
    private const val QUADRANTS = 4
    private const val LOW_BITS = 0xffffffffL

    private fun insert(node: Quad, i: Int, all: List<Body>, depth: Int) {
        val b = all[i]
        node.cx = (node.cx * node.count + b.x) / (node.count + 1)
        node.cy = (node.cy * node.count + b.y) / (node.count + 1)
        node.count += 1
        if (node.children == null) {
            if (node.bodies.isEmpty() || depth >= MAX_DEPTH) {
                node.bodies += i
                return
            }
            val held = node.bodies.toList()
            node.bodies.clear()
            node.children = arrayOfNulls(QUADRANTS)
            for (h in held) place(node, h, all, depth)
        }
        place(node, i, all, depth)
    }

    private fun place(node: Quad, i: Int, all: List<Body>, depth: Int) {
        val b = all[i]
        val half = node.size / 2
        val right = if (b.x >= node.x0 + half) 1 else 0
        val bottom = if (b.y >= node.y0 + half) 1 else 0
        val children = node.children!!
        val k = right + bottom * 2
        val child = children[k] ?: Quad(node.x0 + right * half, node.y0 + bottom * half, half).also { children[k] = it }
        insert(child, i, all, depth + 1)
    }

    /** Barnes-Hut repulsion: far cells act as one body at their centre of mass. */
    private fun repelBarnesHut(bodies: List<Body>, strength: Double) {
        val x0 = bodies.minOf { it.x }
        val y0 = bodies.minOf { it.y }
        val size = maxOf(1.0, bodies.maxOf { it.x } - x0, bodies.maxOf { it.y } - y0) + 1
        val root = Quad(x0, y0, size)
        for (i in bodies.indices) insert(root, i, bodies, 0)
        val stack = ArrayDeque<Quad>()
        for (i in bodies.indices) repelFromTree(bodies, i, root, stack, strength)
    }

    /** Walks the tree for body [i]: far cells push as one body, near leaves body by body. */
    private fun repelFromTree(bodies: List<Body>, i: Int, root: Quad, stack: ArrayDeque<Quad>, strength: Double) {
        val a = bodies[i]
        stack.clear()
        stack.addLast(root)
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            val children = node.children
            val dx = node.cx - a.x
            val dy = node.cy - a.y
            val l2 = dx * dx + dy * dy
            when {
                node.count == 0 -> Unit
                children != null && node.size * node.size / max(EPSILON2, l2) < THETA2 -> {
                    val w = strength * node.count / max(1.0, l2)
                    a.vx += dx * w
                    a.vy += dy * w
                }
                children != null -> stack.addAll(children.filterNotNull())
                else -> repelLeaf(bodies, i, node, strength)
            }
        }
    }

    private fun repelLeaf(bodies: List<Body>, i: Int, node: Quad, strength: Double) {
        val a = bodies[i]
        for (j in node.bodies) {
            if (j == i) continue
            val b = bodies[j]
            var dx = b.x - a.x
            var dy = b.y - a.y
            if (dx == 0.0 && dy == 0.0) {
                val (jx, jy) = jiggle(i, j)
                dx = jx
                dy = jy
            }
            val w = strength / max(1.0, dx * dx + dy * dy)
            a.vx += dx * w
            a.vy += dy * w
        }
    }

    private fun pushApart(a: Body, b: Body, collide: Double) {
        val dx = b.x + b.vx - a.x - a.vx
        val dy = b.y + b.vy - a.y - a.vy
        val l2 = dx * dx + dy * dy
        if (l2 >= collide * collide) return
        val l = sqrt(l2).let { if (it == 0.0) TINY else it }
        val push = (collide - l) / l * COLLIDE_PUSH
        a.vx -= dx * push
        a.vy -= dy * push
        b.vx += dx * push
        b.vy += dy * push
    }

    private fun collideExact(bodies: List<Body>, collide: Double) {
        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) pushApart(bodies[i], bodies[j], collide)
        }
    }

    /** Pushes body [i] apart from the later bodies of one grid cell. */
    private fun collideCell(bodies: List<Body>, i: Int, cell: List<Int>?, collide: Double) {
        cell?.forEach { j -> if (j > i) pushApart(bodies[i], bodies[j], collide) }
    }

    /** Collision through a grid of collide-sized cells: only neighbouring cells can overlap. */
    private fun collideGrid(bodies: List<Body>, collide: Double) {
        val cell = max(1.0, collide)
        val grid = HashMap<Long, MutableList<Int>>()
        fun key(gx: Int, gy: Int): Long = (gx.toLong() shl Int.SIZE_BITS) or (gy.toLong() and LOW_BITS)
        bodies.forEachIndexed { i, b ->
            grid.getOrPut(key(floor(b.x / cell).toInt(), floor(b.y / cell).toInt())) { mutableListOf() } += i
        }
        for (i in bodies.indices) {
            val gx = floor(bodies[i].x / cell).toInt()
            val gy = floor(bodies[i].y / cell).toInt()
            for (ox in -1..1) {
                for (oy in -1..1) collideCell(bodies, i, grid[key(gx + ox, gy + oy)], collide)
            }
        }
    }
}
