package dev.monarchui.components.charts.linechart

import androidx.compose.runtime.Immutable
import dev.monarchui.components.charts.chartframe.MonarchChartFrameLabels

/**
 * Replacements for the built-in strings of a `MonarchLineChart`. Every `null` uses the built-in string named
 * after it in `strings_linechart.xml`.
 *
 * @property frame the strings of the chart frame: "View as table", "Loading chart", "No data yet", "No value"
 * (also the text of a gap in the inspection row and TalkBack) and the others.
 * @property category the heading of the x column of the table for categories when there is no `xTitle`:
 * "Category".
 * @property date the heading of the x column of the table for dates when there is no `xTitle`: "Date".
 * @property noData the spoken summary of a series without values: "No data".
 * @property details the drill-in button of the inspection row: "Details".
 * @property clearSelection the button that ends the inspection: "Clear".
 */
@Immutable
public class MonarchLineChartLabels(
    public val frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
    public val category: String? = null,
    public val date: String? = null,
    public val noData: String? = null,
    public val details: String? = null,
    public val clearSelection: String? = null,
)
