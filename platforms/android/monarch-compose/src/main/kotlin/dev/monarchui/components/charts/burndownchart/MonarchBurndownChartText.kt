package dev.monarchui.components.charts.burndownchart

import dev.monarchui.components.charts.chartframe.MonarchChartTable
import dev.monarchui.components.charts.chartframe.MonarchChartTableSeries
import kotlin.math.abs

/**
 * The built-in words of a burndown, resolved from resources or the app's labels, and the value writer.
 *
 * @property remaining "Remaining".
 * @property ideal "Ideal".
 * @property scope "Scope".
 * @property day "Day".
 * @property scopeChange "Scope change".
 * @property noValue the text of a missing value: "No value".
 * @property value a name and a value: "%1$s %2$s".
 * @property pointValues a day and its values: "%1$s: %2$s".
 * @property change a signed scope change.
 * @property status the status sentence.
 * @property statusTrend the status followed by the comparison: "%1$s, %2$s".
 * @property ahead ahead of the ideal.
 * @property behind behind the ideal.
 * @property onTrack on the ideal line.
 * @property scopeSummary the scope sentence.
 * @property notStarted "Not started".
 * @property format writes a value.
 */
@Suppress("LongParameterList")
internal class MonarchBurndownChartWords(
    val remaining: String,
    val ideal: String,
    val scope: String,
    val day: String,
    val scopeChange: String,
    val noValue: String,
    val value: String,
    val pointValues: String,
    val change: (String, Boolean) -> String,
    val status: (String, String, String) -> String,
    val statusTrend: String,
    val ahead: (String) -> String,
    val behind: (String) -> String,
    val onTrack: String,
    val scopeSummary: (String, String, String) -> String,
    val notStarted: String,
    val format: (Double) -> String,
)

/**
 * Which lines are drawn and read.
 *
 * @property remaining the work left.
 * @property ideal the ideal line.
 * @property scope the scope line, its changes and its table columns.
 * @property scopeOn whether the chart has a scope at all (shown or hidden): its changes are read and tabled.
 */
internal class MonarchBurndownChartShown(
    val remaining: Boolean,
    val ideal: Boolean,
    val scope: Boolean,
    val scopeOn: Boolean,
)

/** The text alternatives of MonarchBurndownChart: each day, the summary, the data table. Pure. */
internal object MonarchBurndownChartText {
    private const val SEPARATOR = ", "
    private const val SENTENCE = ". "

    /** A scope change as written: "+5", "−2", or empty for no change. */
    fun changeText(change: Double, words: MonarchBurndownChartWords): String =
        if (change == 0.0) "" else words.change(words.format(abs(change)), change > 0.0)

    /**
     * One day as TalkBack and the inspection row read it: the day, each shown line with its value ("No value" for
     * a day not reached), then the scope change on a day it changed. "Thu: Remaining 31, Ideal 16, Scope 43, Scope
     * change −2".
     */
    fun describe(
        index: Int,
        days: List<String>,
        data: MonarchBurndownChartData,
        shown: MonarchBurndownChartShown,
        words: MonarchBurndownChartWords,
    ): String {
        fun text(value: Double?) = value?.let(words.format) ?: words.noValue
        val parts = buildList {
            if (shown.remaining) add(words.value.format(words.remaining, text(data.remaining[index])))
            if (shown.ideal) add(words.value.format(words.ideal, text(data.ideal[index])))
            if (shown.scope) add(words.value.format(words.scope, text(data.scope[index])))
            val change = data.changes[index]
            if (shown.scopeOn && change != 0.0) {
                add(words.value.format(words.scopeChange, changeText(change, words)))
            }
        }
        return words.pointValues.format(days.getOrElse(index) { "" }, parts.joinToString(SEPARATOR))
    }

    /**
     * The summary: where the sprint stands on the latest day with work left (ahead, behind or on the ideal line),
     * or "Not started", then the latest scope with the work added and removed since the start.
     */
    fun summary(
        days: List<String>,
        data: MonarchBurndownChartData,
        shown: MonarchBurndownChartShown,
        words: MonarchBurndownChartWords,
    ): String {
        if (days.isEmpty()) return words.noValue
        val difference = data.difference
        val status = if (difference == null) {
            words.notStarted
        } else {
            val left = data.remaining[data.lastActual] ?: 0.0
            val ideal = data.ideal[data.lastActual] ?: left
            val comparison = when {
                difference > 0.0 -> words.behind(words.format(difference))
                difference < 0.0 -> words.ahead(words.format(-difference))
                else -> words.onTrack
            }
            words.statusTrend.format(
                words.status(days[data.lastActual], words.format(left), words.format(ideal)),
                comparison,
            )
        }
        val scope = data.lastScope?.takeIf { shown.scopeOn }?.let {
            words.scopeSummary(words.format(it), words.format(data.added), words.format(data.removed))
        }
        return listOfNotNull(status, scope).joinToString(SENTENCE)
    }

    /**
     * The data table: one row per day with the work left, the ideal and, when the chart has a scope, the scope and
     * its change ("+5"; "0" on a day without one). Hidden lines stay in the table.
     */
    fun table(
        days: List<String>,
        data: MonarchBurndownChartData,
        shown: MonarchBurndownChartShown,
        words: MonarchBurndownChartWords,
    ): MonarchChartTable {
        fun column(name: String, values: List<Double?>) =
            MonarchChartTableSeries(name, values, values.map { it?.let(words.format).orEmpty() })
        val columns = buildList {
            add(column(words.remaining, data.remaining))
            add(column(words.ideal, data.ideal))
            if (shown.scopeOn) {
                add(column(words.scope, data.scope))
                add(
                    MonarchChartTableSeries(
                        words.scopeChange,
                        data.changes,
                        data.changes.map { if (it == 0.0) words.format(0.0) else changeText(it, words) },
                    ),
                )
            }
        }
        return MonarchChartTable(words.day, days, columns)
    }
}
