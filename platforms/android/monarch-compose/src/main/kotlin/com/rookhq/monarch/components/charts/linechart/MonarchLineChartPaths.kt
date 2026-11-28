package com.rookhq.monarch.components.charts.linechart

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/** The pure paths of MonarchLineChart: runs between gaps, lone points, monotone curves, lines and areas. */
internal object MonarchLineChartPaths {
    private const val THIRD = 3f
    private const val TANGENT = 3f
    private const val MIN_CURVE_POINTS = 3

    /** Splits [points] into runs at each gap (`null`), so a missing value never draws a line. */
    fun runs(points: List<Offset?>): List<List<Offset>> {
        val out = mutableListOf<List<Offset>>()
        var run = mutableListOf<Offset>()
        points.forEach { point ->
            if (point != null) {
                run += point
            } else if (run.isNotEmpty()) {
                out += run
                run = mutableListOf()
            }
        }
        if (run.isNotEmpty()) out += run
        return out
    }

    /** The points with no neighbour in their run: drawn as dots so a lone value is not lost. */
    fun isolated(points: List<Offset?>): List<Offset> = runs(points).filter { it.size == 1 }.map { it.single() }

    /**
     * The tangents of a monotone cubic through [run] (Fritsch–Carlson), one per point: the curve never overshoots
     * the values. Runs of fewer than three points get straight lines.
     */
    fun tangents(run: List<Offset>): List<Float> {
        val n = run.size
        if (n < 2) return List(n) { 0f }
        val dx = (0 until n - 1).map { run[it + 1].x - run[it].x }
        val slope = (0 until n - 1).map { i -> if (dx[i] == 0f) 0f else (run[i + 1].y - run[i].y) / dx[i] }
        val inner = (1 until n - 1).map { i ->
            val s0 = slope[i - 1]
            val s1 = slope[i]
            val h0 = dx[i - 1]
            val h1 = dx[i]
            if (s0 * s1 <= 0f) 0f else TANGENT * (h0 + h1) / ((2 * h1 + h0) / s0 + (h1 + 2 * h0) / s1)
        }
        return listOf(slope.first()) + inner + slope.last()
    }

    /** Adds [run] to [path] as a line from its first point: straight segments, or a monotone curve. */
    fun addRun(path: Path, run: List<Offset>, curve: MonarchLineChartCurve, moveFirst: Boolean = true) {
        if (run.isEmpty()) return
        if (moveFirst) path.moveTo(run[0].x, run[0].y) else path.lineTo(run[0].x, run[0].y)
        if (curve == MonarchLineChartCurve.Linear || run.size < MIN_CURVE_POINTS) {
            run.drop(1).forEach { path.lineTo(it.x, it.y) }
            return
        }
        val t = tangents(run)
        for (i in 0 until run.size - 1) {
            val a = run[i]
            val b = run[i + 1]
            val h = (b.x - a.x) / THIRD
            path.cubicTo(a.x + h, a.y + h * t[i], b.x - h, b.y - h * t[i + 1], b.x, b.y)
        }
    }

    /** The line of every run of two or more points. */
    fun linePath(points: List<Offset?>, curve: MonarchLineChartCurve): Path = Path().apply {
        runs(points).filter { it.size > 1 }.forEach { addRun(this, it, curve) }
    }

    /** The closed area between the line of each run of two or more points and [baseline] (a y pixel). */
    fun areaPath(points: List<Offset?>, baseline: Float, curve: MonarchLineChartCurve): Path = Path().apply {
        runs(points).filter { it.size > 1 }.forEach { run ->
            moveTo(run.first().x, baseline)
            addRun(this, run, curve, moveFirst = false)
            lineTo(run.last().x, baseline)
            close()
        }
    }
}
