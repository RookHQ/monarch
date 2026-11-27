package com.rookhq.monarch.components.charts.linechart

import com.rookhq.monarch.components.charts.barchart.MonarchBarChartTicks
import com.rookhq.monarch.components.charts.barchart.MonarchBarChartValueAxis
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/**
 * The pure geometry of MonarchLineChart: x positions, the value axis without zero, label spacing and the nearest
 * point. Ticks and scales come from the bar chart core; lines and areas are in [MonarchLineChartPaths].
 */
internal object MonarchLineChartGeometry {
    private const val FLAT_PAD = 0.1

    /** The value of [series] at [index], or `null` for a gap or a value that is not finite. */
    fun valueAt(series: MonarchLineChartSeries, index: Int): Double? =
        series.values.getOrNull(index)?.takeIf { it.isFinite() }

    /** Evenly spaced positions of [count] categories from 0 to 1. One category sits in the middle. */
    fun evenFractions(count: Int): List<Float> = when {
        count <= 0 -> emptyList()
        count == 1 -> listOf(HALF)
        else -> List(count) { it.toFloat() / (count - 1) }
    }

    /**
     * Positions of [times] from 0 to 1, spaced by time, so uneven dates show. Equal times, or a single one, sit in
     * the middle.
     */
    fun timeFractions(times: List<Long>): List<Float> {
        if (times.isEmpty()) return emptyList()
        val low = times.min()
        val high = times.max()
        if (high == low) return List(times.size) { HALF }
        return times.map { ((it - low).toDouble() / (high - low)).toFloat() }
    }

    /**
     * The value axis. With [includeZero], or a valid [fixed] range, the bar chart's axis (which keeps zero).
     * Without zero, the [values] widened to round ticks of about [count] steps; a flat series is padded by a tenth
     * of its value (or 1 at zero) so it does not sit on an edge.
     */
    fun axis(
        values: List<Double>,
        count: Int,
        fixed: ClosedFloatingPointRange<Double>?,
        includeZero: Boolean,
    ): MonarchBarChartValueAxis {
        val finite = values.filter { it.isFinite() }
        val fixedValid = fixed != null && fixed.endInclusive > fixed.start
        if (includeZero || finite.isEmpty() || fixedValid) {
            return MonarchBarChartTicks.axis(finite, count, fixed)
        }
        var low = finite.min()
        var high = finite.max()
        if (high == low) {
            val pad = if (low == 0.0) 1.0 else abs(low) * FLAT_PAD
            low -= pad
            high += pad
        }
        val step = MonarchBarChartTicks.step(high - low, count)
        val domain = floor(low / step) * step..ceil(high / step) * step
        return MonarchBarChartValueAxis(domain, MonarchBarChartTicks.ticks(domain, step))
    }

    /** The value the area fills down to: zero when the axis holds it, otherwise the nearest end of the axis. */
    fun baseline(domain: ClosedFloatingPointRange<Double>): Double = 0.0.coerceIn(domain.start, domain.endInclusive)

    /**
     * The indices of the x labels to write so that labels are at least [minGap] pixels apart along [length]. The
     * first and the last point are labelled when there is room, as on the web.
     */
    fun labelIndices(count: Int, length: Float, minGap: Float): List<Int> {
        val fit = maxOf(2, if (minGap <= 0f) count else floor(length / minGap).toInt() + 1)
        if (count <= fit) return List(count.coerceAtLeast(0)) { it }
        val every = ceil((count - 1).toDouble() / (fit - 1)).toInt()
        val out = (0 until count step every).toMutableList()
        val last = count - 1
        val previous = out.last()
        when {
            previous == last -> Unit
            last - previous >= every -> out += last
            out.size > 1 -> out[out.size - 1] = last
        }
        return out
    }

    /** The index of the position in [positions] nearest to [at], or `null` when there are none. */
    fun nearestIndex(positions: List<Float>, at: Float): Int? =
        positions.indices.minByOrNull { abs(positions[it] - at) }

    /**
     * The strip of the plot that belongs to point [index]: from halfway to its neighbour before to halfway to its
     * neighbour after, the plot's ends for the first and last point. Measured from the start edge.
     */
    fun strip(positions: List<Float>, index: Int, start: Float, end: Float): Pair<Float, Float> {
        val at = positions[index]
        val from = positions.getOrNull(index - 1)?.let { (it + at) / 2 } ?: start
        val to = positions.getOrNull(index + 1)?.let { (it + at) / 2 } ?: end
        return from to maxOf(to, from + 1f)
    }

    private const val HALF = 0.5f
}
