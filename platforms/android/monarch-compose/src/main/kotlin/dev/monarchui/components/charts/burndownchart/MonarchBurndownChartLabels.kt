package dev.monarchui.components.charts.burndownchart

import androidx.compose.runtime.Immutable
import dev.monarchui.components.charts.chartframe.MonarchChartFrameLabels

/**
 * Replacements for the built-in strings of a `MonarchBurndownChart`. Every `null` uses the built-in string named
 * after it in `strings_burndownchart.xml` (the line chart's for [details] and [clearSelection]).
 *
 * @property remaining the name of the actual line in the legend, TalkBack, the inspection row and the table:
 * "Remaining".
 * @property ideal the name of the ideal line: "Ideal".
 * @property scope the name of the scope line: "Scope".
 * @property day the heading of the day column of the table: "Day".
 * @property today the label of the today marker: "Today".
 * @property scopeChange the heading of the scope change column and its name when read: "Scope change".
 * @property change writes a scope change from the written amount and whether work was added: "+5", "−2".
 * @property status the summary of the latest day with data, from the day, the remaining and the ideal value:
 * "Mar 6: 24 remaining, ideal 17.8".
 * @property ahead added to the status when the work left is under the ideal, from the written difference:
 * "6.2 ahead of ideal".
 * @property behind added to the status when the work left is over the ideal: "6.2 behind ideal".
 * @property onTrack added to the status when the work left equals the ideal: "on the ideal line".
 * @property scopeSummary the summary of the scope, from the latest scope, the work added and the work removed:
 * "Scope 43, 5 added and 2 removed since the start".
 * @property notStarted the summary when no day has a remaining value: "Not started".
 * @property details the drill-in button of the inspection row: "Details".
 * @property clearSelection the button that ends the inspection: "Clear".
 * @property frame the strings of the chart frame: "View as table", "No value" and the others.
 */
@Immutable
public class MonarchBurndownChartLabels(
    public val remaining: String? = null,
    public val ideal: String? = null,
    public val scope: String? = null,
    public val day: String? = null,
    public val today: String? = null,
    public val scopeChange: String? = null,
    public val change: ((amount: String, added: Boolean) -> String)? = null,
    public val status: ((day: String, remaining: String, ideal: String) -> String)? = null,
    public val ahead: ((amount: String) -> String)? = null,
    public val behind: ((amount: String) -> String)? = null,
    public val onTrack: String? = null,
    public val scopeSummary: ((scope: String, added: String, removed: String) -> String)? = null,
    public val notStarted: String? = null,
    public val details: String? = null,
    public val clearSelection: String? = null,
    public val frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
)
