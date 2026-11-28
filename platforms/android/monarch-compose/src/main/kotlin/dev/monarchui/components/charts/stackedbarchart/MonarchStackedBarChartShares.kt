package dev.monarchui.components.charts.stackedbarchart

import dev.monarchui.components.charts.barchart.MonarchBarChartSeries

/**
 * The words of the spoken summary with the 100% offset.
 *
 * @property count the number of categories: "3 categories".
 * @property range the first and last category: "%1$s to %2$s".
 * @property shareSeries one series: "%1$s: average share %2$s, largest %3$s, %4$s".
 * @property categoryValues a name and its text: "%1$s: %2$s".
 * @property noData a series without shares: "No data".
 */
internal class MonarchStackedBarChartWords(
    val count: String,
    val range: String,
    val shareSeries: String,
    val categoryValues: String,
    val noData: String,
)

/** The pure rules of the 100% offset of MonarchStackedBarChart: shares and their summary. */
internal object MonarchStackedBarChartShares {
    private const val SEPARATOR = ", "
    private const val SENTENCE = ". "

    /**
     * Each series with its values replaced by shares of its category's total, from 0 to 1. Negative values count
     * as zero; a gap stays a gap; a category whose total is zero has zero shares. Shares are taken over every
     * series, hidden ones included, so hiding a part leaves a gap instead of rescaling the others.
     */
    fun shares(series: List<MonarchBarChartSeries>, categoryCount: Int): List<MonarchBarChartSeries> {
        val totals = (0 until categoryCount).map { index ->
            series.sumOf { item -> valueAt(item, index)?.coerceAtLeast(0.0) ?: 0.0 }
        }
        return series.map { item ->
            MonarchBarChartSeries(
                key = item.key,
                label = item.label,
                values = (0 until categoryCount).map { index ->
                    valueAt(item, index)?.let { value ->
                        val total = totals[index]
                        if (total > 0) value.coerceAtLeast(0.0) / total else 0.0
                    }
                },
                color = item.color,
            )
        }
    }

    /** The finite value of [series] at [index], or `null` for a gap. */
    fun valueAt(series: MonarchBarChartSeries, index: Int): Double? =
        series.values.getOrNull(index)?.takeIf { it.isFinite() }

    /**
     * The spoken summary with the 100% offset: the categories, then each shown series' average share and its
     * largest share. "3 categories, Jan to Mar. Resolved: average share 80%, largest Feb, 84.6%".
     */
    fun summary(
        categories: List<String>,
        shares: List<MonarchBarChartSeries>,
        visible: List<Int>,
        format: (Double) -> String,
        words: MonarchStackedBarChartWords,
    ): String {
        if (categories.isEmpty()) return words.noData
        val span = if (categories.size == 1) {
            categories.first()
        } else {
            words.range.format(categories.first(), categories.last())
        }
        val parts = visible.map { index ->
            val item = shares[index]
            val values = categories.indices.mapNotNull { category -> valueAt(item, category)?.let { category to it } }
            val largest = values.maxByOrNull { it.second }
            if (largest == null) {
                words.categoryValues.format(item.label, words.noData)
            } else {
                val average = values.sumOf { it.second } / values.size
                words.shareSeries.format(item.label, format(average), categories[largest.first], format(largest.second))
            }
        }
        return (listOf(words.count + SEPARATOR + span) + parts).joinToString(SENTENCE)
    }
}
