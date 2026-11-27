package com.rookhq.monarch.components.charts.linechart

import com.rookhq.monarch.components.charts.chartframe.MonarchChartTable
import com.rookhq.monarch.components.charts.chartframe.MonarchChartTableSeries

/**
 * The built-in words and patterns of a line chart, resolved from resources or the app's labels.
 *
 * @property noValue the text of a gap: "No value".
 * @property noData the summary of a chart or a series without values: "No data".
 * @property value a name and a value: "%1$s %2$s".
 * @property pointValues an x value and its values: "%1$s: %2$s".
 * @property range the first and last x value: "%1$s to %2$s".
 * @property seriesSummary one series in the summary: "%1$s: low %2$s, high %3$s, latest %4$s".
 */
internal class MonarchLineChartWords(
    val noValue: String,
    val noData: String,
    val value: String,
    val pointValues: String,
    val range: String,
    val seriesSummary: String,
)

/**
 * The lowest, highest and latest value of one series.
 *
 * @property min the lowest value.
 * @property max the highest value.
 * @property last the value of the last x value that has one.
 */
internal class MonarchLineChartStats(val min: Double, val max: Double, val last: Double)

/** The text alternatives of MonarchLineChart: the spoken summary, each point, the data table. Pure. */
internal object MonarchLineChartText {
    private const val SEPARATOR = ", "
    private const val SENTENCE = ". "

    /** The lowest, highest and latest value of [series] over [count] x values, or `null` without values. */
    fun stats(series: MonarchLineChartSeries, count: Int): MonarchLineChartStats? {
        val values = (0 until count).mapNotNull { MonarchLineChartGeometry.valueAt(series, it) }
        if (values.isEmpty()) return null
        return MonarchLineChartStats(values.min(), values.max(), values.last())
    }

    /**
     * One x value as TalkBack and the inspection row read it: the x value, then each shown series with its value,
     * [MonarchLineChartWords.noValue] for a gap. "Mon: This week 820, Last week 700".
     */
    @Suppress("LongParameterList")
    fun describe(
        index: Int,
        xLabels: List<String>,
        series: List<MonarchLineChartSeries>,
        visible: List<Int>,
        format: (Double) -> String,
        words: MonarchLineChartWords,
    ): String {
        val parts = visible.map { seriesIndex ->
            val item = series[seriesIndex]
            val value = MonarchLineChartGeometry.valueAt(item, index)?.let(format) ?: words.noValue
            words.value.format(item.label, value)
        }
        return words.pointValues.format(xLabels.getOrElse(index) { "" }, parts.joinToString(SEPARATOR))
    }

    /**
     * The spoken summary: the number and range of x values ([count] is the localised "5 points"), each shown
     * series' low, high and latest value, and each reference line.
     */
    @Suppress("LongParameterList")
    fun summary(
        xLabels: List<String>,
        series: List<MonarchLineChartSeries>,
        visible: List<Int>,
        references: List<MonarchLineChartReferenceLine>,
        count: String,
        format: (Double) -> String,
        words: MonarchLineChartWords,
    ): String {
        if (xLabels.isEmpty()) return words.noData
        val span = if (xLabels.size == 1) xLabels.first() else words.range.format(xLabels.first(), xLabels.last())
        val seriesParts = visible.map { index ->
            val item = series[index]
            val stats = stats(item, xLabels.size)
            if (stats == null) {
                words.pointValues.format(item.label, words.noData)
            } else {
                words.seriesSummary.format(item.label, format(stats.min), format(stats.max), format(stats.last))
            }
        }
        val referenceParts = references.map { line ->
            line.label?.let { words.value.format(it, format(line.value)) } ?: format(line.value)
        }
        return (listOf(count + SEPARATOR + span) + seriesParts + referenceParts).joinToString(SENTENCE)
    }

    /** The data table: one row per x value, one column per series (hidden ones too), values written with [format]. */
    fun table(
        xLabels: List<String>,
        series: List<MonarchLineChartSeries>,
        xTitle: String,
        format: (Double) -> String,
    ): MonarchChartTable {
        val columns = series.map { item ->
            val values = xLabels.indices.map { MonarchLineChartGeometry.valueAt(item, it) }
            MonarchChartTableSeries(item.label, values, values.map { it?.let(format).orEmpty() })
        }
        return MonarchChartTable(xTitle, xLabels, columns)
    }
}
