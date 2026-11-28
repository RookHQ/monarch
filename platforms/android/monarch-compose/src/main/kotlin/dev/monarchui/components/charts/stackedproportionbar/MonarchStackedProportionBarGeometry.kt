package dev.monarchui.components.charts.stackedproportionbar

import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.components.charts.chartlegend.MonarchChartLegendTokens
import dev.monarchui.foundation.MonarchTone

/**
 * One drawn part: a segment, or the remainder of a capacity.
 *
 * @property key the segment's key; [MonarchStackedProportionBarGeometry.REMAINDER_KEY] for the remainder.
 * @property label the name.
 * @property value the amount, 0 or more.
 * @property slot the palette slot, from the segment's position before zeros are hidden.
 * @property tone the status tone in status mode, otherwise `null`.
 * @property remainder whether this is the empty rest of the whole.
 */
internal class MonarchStackedProportionBarPart(
    val key: String,
    val label: String,
    val value: Double,
    val slot: MonarchChartColor,
    val tone: MonarchTone?,
    val remainder: Boolean = false,
)

/**
 * The parts of a bar and the whole they are shares of.
 *
 * @property parts the segments to draw, in order, without the remainder.
 * @property whole the size of the whole: the sum, or a larger total.
 * @property remainder the rest of the whole, 0 when the parts fill it.
 */
internal class MonarchStackedProportionBarModel(
    val parts: List<MonarchStackedProportionBarPart>,
    val whole: Double,
    val remainder: Double,
) {
    /** The sum of the parts. */
    val sum: Double get() = whole - remainder

    /** Whether there is nothing to show: no part has a value. */
    val empty: Boolean get() = sum <= 0.0
}

/** The pure rules of MonarchStackedProportionBar: cleaning, colours, the whole and the drawn spans. */
internal object MonarchStackedProportionBarGeometry {
    /** The key of the remainder part. */
    const val REMAINDER_KEY: String = "__remainder"

    /** [value], or 0 when it is negative or not finite. */
    fun clean(value: Double): Double = value.takeIf { it.isFinite() && it > 0 } ?: 0.0

    /** Status mode: every segment has a tone. A partial set of tones is ignored. */
    fun statusMode(segments: List<MonarchStackedProportionBarSegment>): Boolean =
        segments.isNotEmpty() && segments.all { it.tone != null }

    /**
     * The parts of [segments]: values cleaned, slots taken from the position before zeros are hidden (so hiding a
     * zero never repaints the others), parts of 0 left out with [hideZero]. The whole is [total] when it is larger
     * than the sum; the rest is the remainder.
     */
    fun model(
        segments: List<MonarchStackedProportionBarSegment>,
        total: Double?,
        hideZero: Boolean,
    ): MonarchStackedProportionBarModel {
        val status = statusMode(segments)
        val parts = segments.mapIndexed { index, segment ->
            MonarchStackedProportionBarPart(
                key = segment.key,
                label = segment.label,
                value = clean(segment.value),
                slot = MonarchChartLegendTokens.slot(segment.color, index),
                tone = segment.tone.takeIf { status },
            )
        }.filter { !hideZero || it.value > 0 }
        val sum = parts.sumOf { it.value }
        val whole = total?.let(::clean)?.takeIf { it > sum } ?: sum
        return MonarchStackedProportionBarModel(parts, whole, whole - sum)
    }

    /**
     * Where each value of [values] is drawn along a bar of [width] pixels, as (start, length) from the start edge,
     * with [gap] between neighbours and the rest of the [whole] left as track. A part with a value is at least
     * [minPart] long; the others shrink to make room. Parts of 0 get no length.
     */
    fun spans(values: List<Double>, whole: Double, width: Float, minPart: Float, gap: Float): List<Pair<Float, Float>> {
        if (whole <= 0.0 || values.isEmpty()) return values.map { 0f to 0f }
        val drawn = values.count { it > 0 }
        val rest = whole - values.sum()
        val gaps = (drawn - 1).coerceAtLeast(0) + if (rest > 0 && drawn > 0) 1 else 0
        val available = (width - gap * gaps).coerceAtLeast(0f)
        val clamped = BooleanArray(values.size)
        var lengths = DoubleArray(values.size)
        repeat(values.size + 1) {
            val fixed = values.indices.count { clamped[it] } * minPart
            val freeWhole = rest + values.indices.filter { !clamped[it] }.sumOf { values[it] }
            val space = (available - fixed).coerceAtLeast(0f)
            lengths = DoubleArray(values.size) { index ->
                when {
                    values[index] <= 0 -> 0.0
                    clamped[index] -> minPart.toDouble()
                    freeWhole <= 0 -> 0.0
                    else -> values[index] / freeWhole * space
                }
            }
            val tooShort = values.indices.filter { !clamped[it] && values[it] > 0 && lengths[it] < minPart }
            if (tooShort.isEmpty()) return place(lengths, values, gap)
            tooShort.forEach { clamped[it] = true }
        }
        return place(lengths, values, gap)
    }

    private fun place(lengths: DoubleArray, values: List<Double>, gap: Float): List<Pair<Float, Float>> {
        var at = 0f
        return values.indices.map { index ->
            if (values[index] <= 0) return@map at to 0f
            val span = at to lengths[index].toFloat()
            at += lengths[index].toFloat() + gap
            span
        }
    }
}
