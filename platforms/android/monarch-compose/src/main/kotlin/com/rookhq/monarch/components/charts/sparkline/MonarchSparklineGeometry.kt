package com.rookhq.monarch.components.charts.sparkline

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The latest, lowest and highest of the values that are not gaps.
 *
 * @property last the latest value.
 * @property min the lowest value.
 * @property max the highest value.
 * @property count how many values are not gaps.
 */
internal data class MonarchSparklineStats(val last: Double, val min: Double, val max: Double, val count: Int)

/**
 * Where a sparkline is drawn, in pixels.
 *
 * @property width the width of the canvas.
 * @property height the height of the canvas.
 * @property pad the space kept free at every edge, so dots and strokes are not clipped.
 * @property domain the value range, low to high.
 * @property inverted whether smaller values are drawn higher.
 */
internal data class MonarchSparklineFrame(
    val width: Float,
    val height: Float,
    val pad: Float,
    val domain: Pair<Double, Double>,
    val inverted: Boolean,
) {
    /** The vertical position of [value] in this frame. */
    fun y(value: Double): Float = MonarchSparklineGeometry.y(value, domain, pad, height - pad, inverted)

    /** The baseline an area is filled down to. */
    val baseline: Float get() = if (inverted) pad else height - pad
}

/** The pure geometry of MonarchSparkline, in pixels, left to right. Mirroring is applied when drawing. */
internal object MonarchSparklineGeometry {
    /** A value that is `null` or not finite is a gap. */
    fun clean(values: List<Double?>): List<Double?> = values.map { value -> value?.takeIf { it.isFinite() } }

    fun stats(clean: List<Double?>): MonarchSparklineStats? {
        val present = clean.filterNotNull()
        if (present.isEmpty()) return null
        return MonarchSparklineStats(present.last(), present.min(), present.max(), present.size)
    }

    /** Lines and areas need two values; bars need one. */
    fun hasEnough(stats: MonarchSparklineStats?, variant: MonarchSparklineVariant): Boolean {
        val needed = if (variant == MonarchSparklineVariant.Bar) 1 else 2
        return (stats?.count ?: 0) >= needed
    }

    /** The value range: [range] or the values' own. Bars always include zero; an empty range is widened by one. */
    fun domain(
        stats: MonarchSparklineStats,
        range: ClosedFloatingPointRange<Double>?,
        variant: MonarchSparklineVariant,
    ): Pair<Double, Double> {
        var low = range?.start ?: stats.min
        var high = range?.endInclusive ?: stats.max
        if (variant == MonarchSparklineVariant.Bar) {
            low = min(low, 0.0)
            high = max(high, 0.0)
        }
        if (low == high) {
            low -= 1.0
            high += 1.0
        }
        return low to high
    }

    /** The vertical position of [value], limited to the range, between [top] and [bottom]. */
    fun y(value: Double, domain: Pair<Double, Double>, top: Float, bottom: Float, inverted: Boolean): Float {
        val (low, high) = domain
        val share = ((value.coerceIn(low, high) - low) / (high - low)).toFloat()
        return if (inverted) top + share * (bottom - top) else bottom - share * (bottom - top)
    }

    /** One point per value, `null` for a gap, spread evenly between [pad] and `width - pad`. */
    fun points(clean: List<Double?>, frame: MonarchSparklineFrame): List<Offset?> {
        val step = if (clean.size > 1) (frame.width - frame.pad * 2) / (clean.size - 1) else 0f
        return clean.mapIndexed { index, value -> value?.let { Offset(frame.pad + index * step, frame.y(it)) } }
    }

    /** The runs of points between gaps. */
    fun segments(points: List<Offset?>): List<List<Offset>> {
        val runs = mutableListOf<List<Offset>>()
        var run = mutableListOf<Offset>()
        points.forEach { point ->
            if (point == null) {
                if (run.isNotEmpty()) runs += run
                run = mutableListOf()
            } else {
                run += point
            }
        }
        if (run.isNotEmpty()) runs += run
        return runs
    }

    /** Points that get a dot: every point that stands alone between gaps, and the latest one when asked. */
    fun dots(points: List<Offset?>, showLastPoint: Boolean): List<Offset> {
        val alone = segments(points).filter { it.size == 1 }.map { it.single() }
        val last = points.lastOrNull { it != null }
        return if (showLastPoint && last != null && last !in alone) alone + last else alone
    }

    /** One bar per value, `null` for a gap; a bar is at least one pixel high and grows from zero. */
    fun bars(clean: List<Double?>, frame: MonarchSparklineFrame): List<Rect?> {
        if (clean.isEmpty()) return emptyList()
        val band = frame.width / clean.size
        val gap = if (band > BAR_GAP_FROM) 1f else 0f
        val zero = frame.y(0.0)
        return clean.mapIndexed { index, value ->
            value?.let {
                val point = frame.y(it)
                val top = min(point, zero)
                val size = max(1f, abs(zero - point))
                Rect(index * band + gap / 2, top, index * band + gap / 2 + max(1f, band - gap), top + size)
            }
        }
    }

    /** Bars wider than this many pixels are separated by a one-pixel gap. */
    private const val BAR_GAP_FROM = 4f
}
