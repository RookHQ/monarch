package com.rookhq.monarch.components.charts.areachart

import com.rookhq.monarch.components.charts.chartframe.MonarchChartTable
import com.rookhq.monarch.components.charts.chartframe.MonarchChartTableSeries
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartGeometry
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartReferenceLine
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartSeries
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartText
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartWords

/**
 * The words and writers an area chart's text needs.
 *
 * @property line the words shared with the line chart.
 * @property total the name of a stack's total: "Total".
 * @property share a written value with its written share: "820 (45%)".
 * @property format writes a value.
 * @property percent writes a share from 0 to 1 as a percentage.
 */
internal class MonarchAreaChartWords(
    val line: MonarchLineChartWords,
    val total: String,
    val share: (String, String) -> String,
    val format: (Double) -> String,
    val percent: (Double) -> String,
)

/** The text alternatives of MonarchAreaChart: each x value, the spoken summary, the data table. Pure. */
internal object MonarchAreaChartText {
    private const val SEPARATOR = ", "
    private const val SENTENCE = ". "

    /**
     * One x value as TalkBack and the inspection row read it: the x value, each shown series with its real value
     * (with its share in a 100% stack), then the total of a stack of two or more. "Mon: Web 420, iOS 210, Total
     * 630".
     */
    @Suppress("LongParameterList")
    fun describe(
        index: Int,
        xLabels: List<String>,
        series: List<MonarchLineChartSeries>,
        visible: List<Int>,
        stack: MonarchAreaChartStack,
        stacking: MonarchAreaChartStacking,
        words: MonarchAreaChartWords,
    ): String {
        val total = stack.totals.getOrElse(index) { 0.0 }
        val parts = visible.map { seriesIndex ->
            val item = series[seriesIndex]
            val value = MonarchLineChartGeometry.valueAt(item, index)
            val text = when {
                value == null -> words.line.noValue
                stacking == MonarchAreaChartStacking.Expand ->
                    words.share(words.format(value), words.percent(MonarchAreaChartStacks.share(value, total)))
                else -> words.format(value)
            }
            words.line.value.format(item.label, text)
        }.toMutableList()
        if (MonarchAreaChartStacks.showsTotal(stacking, visible.size)) {
            parts += words.line.value.format(words.total, words.format(total))
        }
        return words.line.pointValues.format(xLabels.getOrElse(index) { "" }, parts.joinToString(SEPARATOR))
    }

    /**
     * The spoken summary: the number and range of x values ([count] is the localised "5 points"), each shown
     * series' low, high and latest real value, the total of a stack of two or more, then each reference line.
     */
    @Suppress("LongParameterList")
    fun summary(
        xLabels: List<String>,
        series: List<MonarchLineChartSeries>,
        visible: List<Int>,
        stack: MonarchAreaChartStack,
        stacking: MonarchAreaChartStacking,
        references: List<MonarchLineChartReferenceLine>,
        count: String,
        words: MonarchAreaChartWords,
    ): String {
        val head = MonarchLineChartText.summary(xLabels, series, visible, emptyList(), count, words.format, words.line)
        if (xLabels.isEmpty()) return head
        val totalPart = if (MonarchAreaChartStacks.showsTotal(stacking, visible.size)) {
            val totals = stack.totals
            listOf(
                words.line.seriesSummary.format(
                    words.total,
                    words.format(totals.min()),
                    words.format(totals.max()),
                    words.format(totals.last()),
                ),
            )
        } else {
            emptyList()
        }
        val referenceParts = references.map { line ->
            line.label?.let { words.line.value.format(it, words.format(line.value)) } ?: words.format(line.value)
        }
        return (listOf(head) + totalPart + referenceParts).joinToString(SENTENCE)
    }

    /**
     * The data table: one row per x value, one column per series (hidden ones too) with the real values, and in a
     * stack of two or more series a total column: the sum of every series, a negative value or a gap counting as
     * zero.
     */
    @Suppress("LongParameterList")
    fun table(
        xLabels: List<String>,
        series: List<MonarchLineChartSeries>,
        xTitle: String,
        stacking: MonarchAreaChartStacking,
        words: MonarchAreaChartWords,
    ): MonarchChartTable {
        val columns = series.map { item ->
            val values = xLabels.indices.map { MonarchLineChartGeometry.valueAt(item, it) }
            MonarchChartTableSeries(item.label, values, values.map { it?.let(words.format).orEmpty() })
        }
        if (!MonarchAreaChartStacks.showsTotal(stacking, series.size)) {
            return MonarchChartTable(xTitle, xLabels, columns)
        }
        val totals = MonarchAreaChartStacks.stack(series, series.indices.toList(), xLabels.size, stacking).totals
        val total = MonarchChartTableSeries(words.total, totals, totals.map(words.format))
        return MonarchChartTable(xTitle, xLabels, columns + total)
    }
}
