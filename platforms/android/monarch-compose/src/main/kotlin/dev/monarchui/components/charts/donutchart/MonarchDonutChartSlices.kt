package dev.monarchui.components.charts.donutchart

import dev.monarchui.components.charts.chartframe.MonarchChartTable
import dev.monarchui.components.charts.chartframe.MonarchChartTableSeries
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.components.charts.chartlegend.MonarchChartColor.Neutral
import dev.monarchui.components.charts.chartlegend.MonarchChartLegendTokens
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin

/**
 * One drawn slice: a segment, or the Other slice the smallest segments fold into.
 *
 * @property key the segment's key; [MonarchDonutChartSlices.OTHER_KEY] for Other.
 * @property label the name.
 * @property value the amount, 0 or more.
 * @property slot the palette slot.
 */
internal class MonarchDonutChartSlice(
    val key: String,
    val label: String,
    val value: Double,
    val slot: MonarchChartColor,
)

/**
 * Where a slice is drawn, in degrees clockwise from the top.
 *
 * @property slice the slice.
 * @property start the start angle, after half the gap.
 * @property sweep the length of the arc.
 */
internal class MonarchDonutChartArc(val slice: MonarchDonutChartSlice, val start: Float, val sweep: Float) {
    /** The angle of the middle of the arc. */
    val middle: Float get() = start + sweep / 2
}

/**
 * The words and patterns that write shares and entries.
 *
 * @property entry "%1$s %2$s (%3$s)": a name, its value and its share.
 * @property namedValue "%1$s %2$s": a name and a value.
 * @property valueAndShare "%1$s · %2$s": a value and a share.
 * @property below "<%1$s": a share under the smallest step.
 * @property above ">%1$s": a share just under the whole.
 * @property total "Total".
 */
internal class MonarchDonutChartWords(
    val entry: String,
    val namedValue: String,
    val valueAndShare: String,
    val below: String,
    val above: String,
    val total: String,
)

/** The pure rules of MonarchDonutChart: cleaning, order, folding, arcs, shares and text. */
internal object MonarchDonutChartSlices {
    /** The key of the Other slice. */
    const val OTHER_KEY: String = "__other"

    /** The fewest slices: Other never stands alone. */
    const val MIN_SEGMENTS: Int = 2

    private const val FULL_TURN = 360f
    private const val PERCENT = 100.0
    private const val TEN = 10.0
    private const val QUARTER_TURN = 90.0

    /** [segments] with negative and non-finite values set to 0, in the order of [sort]. */
    fun ordered(segments: List<MonarchDonutChartSegment>, sort: MonarchDonutChartSort): List<MonarchDonutChartSegment> {
        val clean = segments.map { segment ->
            val value = segment.value.takeIf { it.isFinite() && it > 0 } ?: 0.0
            MonarchDonutChartSegment(segment.key, segment.label, value, segment.color)
        }
        return if (sort == MonarchDonutChartSort.Descending) clean.sortedByDescending { it.value } else clean
    }

    /** The sum of the values. */
    fun total(ordered: List<MonarchDonutChartSegment>): Double = ordered.sumOf { it.value }

    /**
     * The slices to draw: every segment with a value while there are at most [maxSegments] (at least 2);
     * otherwise the first `maxSegments - 1` and one neutral Other slice named [other] with the rest.
     */
    fun slices(ordered: List<MonarchDonutChartSegment>, maxSegments: Int, other: String): List<MonarchDonutChartSlice> {
        val limit = max(MIN_SEGMENTS, maxSegments)
        val colored = ordered.mapIndexed { index, segment ->
            val slot = MonarchChartLegendTokens.slot(segment.color, index)
            MonarchDonutChartSlice(segment.key, segment.label, segment.value, slot)
        }
        val folded = if (colored.size > limit) {
            colored.take(limit - 1) +
                MonarchDonutChartSlice(OTHER_KEY, other, colored.drop(limit - 1).sumOf { it.value }, Neutral)
        } else {
            colored
        }
        return folded.filter { it.value > 0 }
    }

    /**
     * The arcs of [slices] around a full turn. With two or more slices each loses [gap] degrees, but keeps at
     * least [minSweep] degrees (or its own size, if smaller), so a tiny share stays visible.
     */
    fun arcs(slices: List<MonarchDonutChartSlice>, gap: Float, minSweep: Float): List<MonarchDonutChartArc> {
        val total = slices.sumOf { it.value }
        if (total <= 0.0) return emptyList()
        val spacing = if (slices.size > 1) gap else 0f
        var at = 0f
        return slices.map { slice ->
            val length = (slice.value / total * FULL_TURN).toFloat()
            val sweep = max(length - spacing, min(length, minSweep))
            val arc = MonarchDonutChartArc(slice, at + (length - sweep) / 2, sweep)
            at += length
            arc
        }
    }

    /**
     * The point at [angle] degrees clockwise from the top on a circle of [radius] around ([center], [center]),
     * measured from the start edge: x grows towards the end.
     */
    fun pointAt(angle: Float, radius: Float, center: Float): Pair<Float, Float> {
        val radians = Math.toRadians(angle.toDouble() - QUARTER_TURN)
        return (center + radius * cos(radians)).toFloat() to (center + radius * sin(radians)).toFloat()
    }

    /**
     * The share of [value] in [total], written by [percent] (which takes a fraction), with [digits] decimals.
     * A share that rounds to 0 is "<1%", one that rounds to the whole without being it ">99%".
     */
    fun share(
        value: Double,
        total: Double,
        digits: Int,
        percent: (Double) -> String,
        words: MonarchDonutChartWords,
    ): String {
        if (total <= 0.0) return percent(0.0)
        val fraction = value / total
        val scale = TEN.pow(digits.coerceAtLeast(0))
        val rounded = round(fraction * PERCENT * scale) / scale
        val step = 1 / scale / PERCENT
        return when {
            value > 0 && rounded == 0.0 -> words.below.format(percent(step))
            value < total && rounded >= PERCENT -> words.above.format(percent(1 - step))
            else -> percent(fraction)
        }
    }

    /** What a legend entry shows after its label, or `null` for [MonarchDonutChartLegendContent.None]. */
    fun legendValue(
        content: MonarchDonutChartLegendContent,
        value: String,
        share: String,
        words: MonarchDonutChartWords,
    ): String? =
        when (content) {
            MonarchDonutChartLegendContent.Both -> words.valueAndShare.format(value, share)
            MonarchDonutChartLegendContent.Value -> value
            MonarchDonutChartLegendContent.Percent -> share
            MonarchDonutChartLegendContent.None -> null
        }

    /** The spoken summary: "Total 874. Laptops 412 (47%), Phones 268 (31%)". Segments of 0 are left out. */
    fun summary(
        ordered: List<MonarchDonutChartSegment>,
        format: (Double) -> String,
        share: (Double) -> String,
        words: MonarchDonutChartWords,
    ): String {
        val total = words.namedValue.format(words.total, format(total(ordered)))
        val parts = ordered.filter { it.value > 0 }.map {
            words.entry.format(it.label, format(it.value), share(it.value))
        }
        return if (parts.isEmpty()) total else "$total. ${parts.joinToString(", ")}"
    }

    /** The data table: every segment, then the total, with value and share columns. */
    @Suppress("LongParameterList")
    fun table(
        ordered: List<MonarchDonutChartSegment>,
        segmentTitle: String,
        valueTitle: String,
        shareTitle: String,
        format: (Double) -> String,
        share: (Double) -> String,
        words: MonarchDonutChartWords,
    ): MonarchChartTable {
        val total = total(ordered)
        val values = ordered.map { it.value } + total
        val shares = values.map { if (total > 0) it / total * PERCENT else 0.0 }
        return MonarchChartTable(
            categoryTitle = segmentTitle,
            categories = ordered.map { it.label } + words.total,
            series = listOf(
                MonarchChartTableSeries(valueTitle, values, values.map(format)),
                MonarchChartTableSeries(shareTitle, shares, values.map(share)),
            ),
        )
    }
}
