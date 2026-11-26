package com.rookhq.monarch.components.charts.areachart

import com.rookhq.monarch.components.charts.linechart.MonarchLineChartGeometry
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartSeries

/**
 * The areas of a chart worked out from its series.
 *
 * @property totals the total of each x value: the sum of the shown series, a negative value or a gap counting as
 * zero.
 * @property edges the top edge of each shown series, in the order of the shown series, one value per x value and
 * `null` for a gap: the value itself when the areas overlap, the running sum in a stack and the running share
 * (0 to 1) in a 100% stack.
 */
internal class MonarchAreaChartStack(val totals: List<Double>, val edges: List<List<Double?>>)

/** The pure stacking of MonarchAreaChart, as on the web. */
internal object MonarchAreaChartStacks {
    /**
     * Stacks the shown series ([visible], indices into [series]) over [count] x values. A gap breaks only its own
     * series: the series above keep their place. In a stack a negative value counts as zero; an x value whose total
     * is zero gives every series a share of zero.
     */
    fun stack(
        series: List<MonarchLineChartSeries>,
        visible: List<Int>,
        count: Int,
        stacking: MonarchAreaChartStacking,
    ): MonarchAreaChartStack {
        val totals = List(count) { x ->
            visible.sumOf { (MonarchLineChartGeometry.valueAt(series[it], x) ?: 0.0).coerceAtLeast(0.0) }
        }
        val base = DoubleArray(count)
        val edges = visible.map { index ->
            List(count) { x ->
                val value = MonarchLineChartGeometry.valueAt(series[index], x) ?: return@List null
                when (stacking) {
                    MonarchAreaChartStacking.Overlap -> value
                    MonarchAreaChartStacking.Stacked -> (base[x] + value.coerceAtLeast(0.0)).also { base[x] = it }
                    MonarchAreaChartStacking.Expand -> (base[x] + share(value, totals[x])).also { base[x] = it }
                }
            }
        }
        return MonarchAreaChartStack(totals, edges)
    }

    /** The share of [total] that [value] is, from 0 to 1: zero for a negative value or a zero total. */
    fun share(value: Double, total: Double): Double = if (total > 0.0) value.coerceAtLeast(0.0) / total else 0.0

    /** Whether the stack has a total worth writing: a stack of two or more shown series. */
    fun showsTotal(stacking: MonarchAreaChartStacking, shown: Int): Boolean =
        stacking != MonarchAreaChartStacking.Overlap && shown > 1
}
