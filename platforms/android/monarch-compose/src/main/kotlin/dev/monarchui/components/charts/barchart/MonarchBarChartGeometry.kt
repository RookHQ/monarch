package dev.monarchui.components.charts.barchart

import androidx.compose.ui.geometry.Rect
import kotlin.math.ceil
import kotlin.math.floor

/**
 * One drawn bar, or one part of a stack, in value units.
 *
 * @property category the category index.
 * @property series the index of the series in the chart's series list.
 * @property value the value of the bar.
 * @property from where the bar starts on the value axis: zero, or the end of the part below it in a stack.
 * @property to where the bar ends on the value axis.
 * @property slot the position of the bar in its group: 0 for a stack.
 * @property roundedEnd whether the far end is rounded: every grouped bar, and the outermost part of a stack.
 */
internal class MonarchBarChartSegment(
    val category: Int,
    val series: Int,
    val value: Double,
    val from: Double,
    val to: Double,
    val slot: Int,
    val roundedEnd: Boolean,
)

/**
 * The bars of one category.
 *
 * @property segments the bars, in series order.
 * @property positive the sum of the positive values.
 * @property negative the sum of the negative values.
 */
internal class MonarchBarChartStack(
    val segments: List<MonarchBarChartSegment>,
    val positive: Double,
    val negative: Double,
) {
    /** The net total of the category. */
    val total: Double get() = positive + negative
}

/**
 * The plot area of a bar chart in pixels, measured from the start edge: [x0]..[x1] across, [y0]..[y1] down.
 * Drawing mirrors x in right to left. Shared chart core.
 *
 * @property horizontal whether the bars grow across (categories run down).
 * @property value the value scale: along x for horizontal bars, along y (bottom up) for columns.
 * @property bandStart where the first category band starts on the category axis.
 * @property band the length of one category band.
 * @property ticks the values of the value axis that get a gridline and a label.
 */
@Suppress("LongParameterList")
internal class MonarchBarChartPlot(
    val width: Float,
    val height: Float,
    val x0: Float,
    val x1: Float,
    val y0: Float,
    val y1: Float,
    val horizontal: Boolean,
    val value: MonarchBarChartScale,
    val bandStart: Float,
    val band: Float,
    val ticks: List<Double>,
) {
    /** The start of category [index] on the category axis. */
    fun bandAt(index: Int): Float = bandStart + index * band

    /** The band of category [index] across the plot, measured from the start edge. */
    fun categoryRect(index: Int): Rect = if (horizontal) {
        Rect(x0, bandAt(index), x1, bandAt(index) + band)
    } else {
        Rect(bandAt(index), y0, bandAt(index) + band, y1)
    }

    /** The category at [position] on the category axis (y for horizontal bars, x from the start for columns). */
    fun indexAt(position: Float, count: Int): Int? = MonarchBarChartGeometry.indexAt(position, bandStart, band, count)
}

/** The pure geometry of MonarchBarChart: stacks, extents, bands. Shared chart core. */
internal object MonarchBarChartGeometry {
    /** The share of a band the bars of a category fill, as on the web. */
    const val GROUP_FILL: Float = 0.72f

    /** The value of [series] at [index], or `null` for a gap or a value that is not finite. */
    fun valueAt(series: MonarchBarChartSeries, index: Int): Double? =
        series.values.getOrNull(index)?.takeIf { it.isFinite() }

    /**
     * The bars of every category. [visible] lists the indices of the shown series, in order. Stacked bars sum
     * the positive and the negative values separately from zero.
     */
    fun stacks(
        series: List<MonarchBarChartSeries>,
        visible: List<Int>,
        categoryCount: Int,
        stacked: Boolean,
    ): List<MonarchBarChartStack> = (0 until categoryCount).map { category ->
        var positive = 0.0
        var negative = 0.0
        val segments = mutableListOf<MonarchBarChartSegment>()
        visible.forEachIndexed { slot, seriesIndex ->
            val value = valueAt(series[seriesIndex], category) ?: return@forEachIndexed
            val base = if (!stacked) 0.0 else if (value >= 0) positive else negative
            if (value >= 0) positive += value else negative += value
            segments += MonarchBarChartSegment(
                category = category,
                series = seriesIndex,
                value = value,
                from = base,
                to = base + value,
                slot = if (stacked) 0 else slot,
                roundedEnd = !stacked,
            )
        }
        MonarchBarChartStack(if (stacked) roundOutermost(segments) else segments, positive, negative)
    }

    /** In a stack only the last positive and the last negative part have a rounded end. */
    private fun roundOutermost(segments: List<MonarchBarChartSegment>): List<MonarchBarChartSegment> {
        val lastPositive = segments.lastOrNull { it.value > 0 }
        val lastNegative = segments.lastOrNull { it.value < 0 }
        return segments.map { segment ->
            if (segment === lastPositive || segment === lastNegative) {
                MonarchBarChartSegment(
                    segment.category,
                    segment.series,
                    segment.value,
                    segment.from,
                    segment.to,
                    segment.slot,
                    roundedEnd = true,
                )
            } else {
                segment
            }
        }
    }

    /** The values the value axis must include: every bar end (stack sums when stacked) and every reference line. */
    fun extent(stacks: List<MonarchBarChartStack>, stacked: Boolean, references: List<Double>): List<Double> {
        val ends = stacks.flatMap { stack ->
            if (stacked) listOf(stack.positive, stack.negative) else stack.segments.map { it.to }
        }
        return ends + references
    }

    /** The category at [position], or `null` outside the bands. */
    fun indexAt(position: Float, bandStart: Float, band: Float, count: Int): Int? {
        if (count <= 0 || band <= 0f) return null
        val index = floor((position - bandStart) / band).toInt()
        return index.takeIf { it in 0 until count }
    }

    /** The length of the bars of one category across the band: at most [GROUP_FILL] of it and [maxBar] per bar. */
    fun groupSize(band: Float, maxBar: Float, bars: Int, stacked: Boolean): Float {
        val count = if (stacked) 1 else bars.coerceAtLeast(1)
        return minOf(band * GROUP_FILL, maxBar * count)
    }

    /**
     * Where a bar sits across its band: the offset from the band's start and the thickness. Grouped bars leave
     * [gap] between neighbours.
     */
    @Suppress("LongParameterList")
    fun cross(slot: Int, band: Float, group: Float, bars: Int, stacked: Boolean, gap: Float): Pair<Float, Float> {
        val count = if (stacked) 1 else bars.coerceAtLeast(1)
        val per = group / count
        val inset = if (count > 1) gap / 2 else 0f
        val offset = (band - group) / 2 + slot * per + inset
        return offset to (per - 2 * inset).coerceAtLeast(1f)
    }

    /** The categories that get a label when labels need [labelLength] each along a band of [band]: every n-th. */
    fun labelStep(band: Float, labelLength: Float): Int =
        if (band <= 0f) 1 else ceil(labelLength / band).toInt().coerceAtLeast(1)
}
