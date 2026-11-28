package dev.monarchui.components.charts.barchart

import dev.monarchui.components.charts.chartframe.MonarchChartTable
import dev.monarchui.components.charts.chartframe.MonarchChartTableSeries

/**
 * The built-in words and patterns of a bar chart, resolved from resources or the app's labels.
 *
 * @property total "Total".
 * @property noValue the text of a gap: "No value".
 * @property noData the summary of a chart or a series without values: "No data".
 * @property value a name and a value: "%1$s %2$s".
 * @property categoryValues a category and its values: "%1$s: %2$s".
 * @property range the first and last category: "%1$s to %2$s".
 * @property seriesSummary one series in the summary: "%1$s: total %2$s, highest %3$s, %4$s".
 */
internal class MonarchBarChartWords(
    val total: String,
    val noValue: String,
    val noData: String,
    val value: String,
    val categoryValues: String,
    val range: String,
    val seriesSummary: String,
)

/**
 * The total and the highest value of one series.
 *
 * @property total the sum of the values.
 * @property max the highest value.
 * @property maxIndex the category of the highest value.
 */
internal class MonarchBarChartStats(val total: Double, val max: Double, val maxIndex: Int)

/** The text alternatives of MonarchBarChart: the spoken summary, each category, the data table. Pure. */
internal object MonarchBarChartText {
    private const val SEPARATOR = ", "
    private const val SENTENCE = ". "

    /** The total and the highest value of [series] over [count] categories, or `null` without values. */
    fun stats(series: MonarchBarChartSeries, count: Int): MonarchBarChartStats? {
        val values = (0 until count).mapNotNull { index ->
            MonarchBarChartGeometry.valueAt(series, index)?.let { index to it }
        }
        val highest = values.maxByOrNull { it.second } ?: return null
        return MonarchBarChartStats(values.sumOf { it.second }, highest.second, highest.first)
    }

    /** A value as written, or [MonarchBarChartWords.noValue] for a gap. */
    fun valueText(value: Double?, format: (Double) -> String, words: MonarchBarChartWords): String =
        value?.let(format) ?: words.noValue

    /**
     * One category as TalkBack and the inspection row read it: the category, each shown series with its value,
     * and the total of a stack of two or more series. "Jan: Web 1,200, iOS 800, Total 2,000".
     */
    @Suppress("LongParameterList")
    fun describe(
        index: Int,
        categories: List<String>,
        series: List<MonarchBarChartSeries>,
        visible: List<Int>,
        stacks: List<MonarchBarChartStack>,
        stacked: Boolean,
        format: (Double) -> String,
        words: MonarchBarChartWords,
    ): String {
        val parts = visible.map { seriesIndex ->
            val item = series[seriesIndex]
            words.value.format(item.label, valueText(MonarchBarChartGeometry.valueAt(item, index), format, words))
        }
        val total = stacks.getOrNull(index)
            ?.takeIf { stacked && visible.size > 1 }
            ?.let { listOf(words.value.format(words.total, format(it.total))) }
            .orEmpty()
        return words.categoryValues.format(categories.getOrElse(index) { "" }, (parts + total).joinToString(SEPARATOR))
    }

    /**
     * The spoken summary: the number and range of categories ([count] is the localised "3 categories"), each
     * shown series' total and highest category, and each reference line.
     */
    @Suppress("LongParameterList")
    fun summary(
        categories: List<String>,
        series: List<MonarchBarChartSeries>,
        visible: List<Int>,
        references: List<MonarchBarChartReferenceLine>,
        count: String,
        format: (Double) -> String,
        words: MonarchBarChartWords,
    ): String {
        if (categories.isEmpty()) return words.noData
        val span = if (categories.size == 1) {
            categories.first()
        } else {
            words.range.format(categories.first(), categories.last())
        }
        val seriesParts = visible.map { index ->
            val item = series[index]
            val stats = stats(item, categories.size)
            if (stats == null) {
                words.categoryValues.format(item.label, words.noData)
            } else {
                val highest = categories[stats.maxIndex]
                words.seriesSummary.format(item.label, format(stats.total), highest, format(stats.max))
            }
        }
        val referenceParts = references.map { line ->
            line.label?.let { words.value.format(it, format(line.value)) } ?: format(line.value)
        }
        return (listOf(count + SEPARATOR + span) + seriesParts + referenceParts).joinToString(SENTENCE)
    }

    /**
     * The data table: one row per category, one column per series (hidden ones too), and a Total column when
     * two or more series are stacked. Values are written with [format].
     */
    @Suppress("LongParameterList")
    fun table(
        categories: List<String>,
        series: List<MonarchBarChartSeries>,
        stacked: Boolean,
        categoryTitle: String,
        total: String,
        format: (Double) -> String,
    ): MonarchChartTable {
        val columns = series.map { item ->
            val values = categories.indices.map { MonarchBarChartGeometry.valueAt(item, it) }
            MonarchChartTableSeries(item.label, values, values.map { it?.let(format).orEmpty() })
        }
        val totals = if (stacked && series.size > 1) {
            val sums = categories.indices.map { index ->
                series.sumOf { MonarchBarChartGeometry.valueAt(it, index) ?: 0.0 }
            }
            listOf(MonarchChartTableSeries(total, sums, sums.map(format)))
        } else {
            emptyList()
        }
        return MonarchChartTable(categoryTitle, categories, columns + totals)
    }
}
