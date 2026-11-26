package com.rookhq.monarch.components.charts.barchart

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * A linear scale from values to pixels: [domain] maps onto [start]..[end]. [end] may be smaller than [start]
 * (a vertical value axis runs from the bottom up). Shared chart core; to be promoted with the other charts.
 */
internal class MonarchBarChartScale(
    val domain: ClosedFloatingPointRange<Double>,
    val start: Float,
    val end: Float,
) {
    /** The pixel of [value]. Values outside the domain map outside the range. */
    fun map(value: Double): Float {
        val span = domain.endInclusive - domain.start
        if (span == 0.0) return start
        return (start + (value - domain.start) / span * (end - start)).toFloat()
    }
}

/**
 * The value axis of a chart.
 *
 * @property domain the values from the start to the end of the axis.
 * @property ticks the values that get a gridline and a label.
 */
internal class MonarchBarChartValueAxis(val domain: ClosedFloatingPointRange<Double>, val ticks: List<Double>)

/** The value axis of a chart: its domain and round ticks. Pure. Shared chart core. */
internal object MonarchBarChartTicks {
    private const val MIN_TICKS = 2
    private const val MAX_TICKS = 6
    private const val STEP_TWO = 2.0
    private const val STEP_FIVE = 5.0
    private const val STEP_TEN = 10.0
    private const val STEP_TWO_FIVE = 2.5

    /** The number of ticks for a plot [length] when ticks should be at least [spacing] apart, from 2 to 6. */
    fun count(length: Float, spacing: Float): Int =
        if (spacing <= 0f) MIN_TICKS else (length / spacing).toInt().coerceIn(MIN_TICKS, MAX_TICKS)

    /** A round step (1, 2 or 5 times a power of ten) that splits [span] into about [count] parts. */
    fun step(span: Double, count: Int): Double {
        if (span <= 0.0 || !span.isFinite()) return 1.0
        val rough = span / count.coerceAtLeast(1)
        val magnitude = STEP_TEN.pow(floor(log10(rough)))
        val residual = rough / magnitude
        // The web's steps: 1, 2, 2.5, 5 and 10 times a power of ten, so the axes read the same.
        val nice = when {
            residual <= 1.0 -> 1.0
            residual <= STEP_TWO -> STEP_TWO
            residual <= STEP_TWO_FIVE -> STEP_TWO_FIVE
            residual <= STEP_FIVE -> STEP_FIVE
            else -> STEP_TEN
        }
        return nice * magnitude
    }

    /**
     * The value axis: [fixed] when given, otherwise the [values] and zero widened to round ticks of about
     * [count] steps. With no extent at all, 0 to 1.
     */
    fun axis(values: List<Double>, count: Int, fixed: ClosedFloatingPointRange<Double>?): MonarchBarChartValueAxis {
        if (fixed != null && fixed.endInclusive > fixed.start) {
            return MonarchBarChartValueAxis(fixed, ticks(fixed, step(fixed.endInclusive - fixed.start, count)))
        }
        val finite = values.filter { it.isFinite() }
        val low = minOf(0.0, finite.minOrNull() ?: 0.0)
        val high = maxOf(0.0, finite.maxOrNull() ?: 0.0)
        val step = step(if (high > low) high - low else 1.0, count)
        val domain = if (high > low) floor(low / step) * step..ceil(high / step) * step else 0.0..1.0
        return MonarchBarChartValueAxis(domain, ticks(domain, step))
    }

    /** The ticks of [domain]: every multiple of [step] inside it, rounded to remove float noise. */
    fun ticks(domain: ClosedFloatingPointRange<Double>, step: Double): List<Double> {
        if (step <= 0.0) return listOf(domain.start, domain.endInclusive)
        val first = ceil(domain.start / step - EPSILON)
        val last = floor(domain.endInclusive / step + EPSILON)
        if (last < first) return emptyList()
        return (first.toLong()..last.toLong()).map { clean(it * step) }
    }

    /** Removes the binary noise of a product such as 0.1 * 3. */
    private fun clean(value: Double): Double {
        if (value == 0.0) return 0.0
        val digits = (DIGITS - ceil(log10(abs(value)))).toInt().coerceAtLeast(0)
        val factor = STEP_TEN.pow(digits)
        return (value * factor).roundToLong() / factor
    }

    private const val EPSILON = 1e-9
    private const val DIGITS = 12.0
}
