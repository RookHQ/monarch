package dev.monarchui.components.charts.barchart

import androidx.compose.runtime.Immutable
import dev.monarchui.components.charts.chartframe.MonarchChartFrameLabels

/**
 * Replacements for the built-in strings of a `MonarchBarChart`. Every `null` uses the built-in string named
 * after it in `strings_barchart.xml`.
 *
 * @property frame the strings of the chart frame: "View as table", "Loading chart", "No data yet", "No value"
 * and the others.
 * @property category the heading of the category column of the table when there is no `categoryTitle`:
 * "Category".
 * @property total the stack total in the table, the inspection row and TalkBack: "Total".
 * @property noData the spoken summary of a series without values: "No data".
 * @property details the drill-in button of the inspection row: "Details".
 * @property clearSelection the button that ends the inspection: "Clear".
 * @property highlighted the state TalkBack reads on the highlighted category: "Highlighted".
 */
@Immutable
public class MonarchBarChartLabels(
    public val frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
    public val category: String? = null,
    public val total: String? = null,
    public val noData: String? = null,
    public val details: String? = null,
    public val clearSelection: String? = null,
    public val highlighted: String? = null,
)
