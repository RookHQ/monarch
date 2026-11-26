package com.rookhq.monarch.components.charts.areachart

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameLabels

/**
 * Replacements for the built-in strings of a `MonarchAreaChart`. Every `null` uses the built-in string: the
 * line chart's for the words both charts share, `strings_areachart.xml` for [total] and [share].
 *
 * @property frame the strings of the chart frame: "View as table", "Loading chart", "No data yet", "No value"
 * (also the text of a gap in the inspection row and TalkBack) and the others.
 * @property category the heading of the x column of the table for categories when there is no `xTitle`:
 * "Category".
 * @property date the heading of the x column of the table for dates when there is no `xTitle`: "Date".
 * @property noData the spoken summary of a series without values: "No data".
 * @property details the drill-in button of the inspection row: "Details".
 * @property clearSelection the button that ends the inspection: "Clear".
 * @property total the name of a stack's total in the inspection row, TalkBack, the summary and the table:
 * "Total".
 * @property share writes a value with its share in [MonarchAreaChartStacking.Expand], from the written value
 * and the written percentage: "820 (45%)".
 */
@Immutable
public class MonarchAreaChartLabels(
    public val frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
    public val category: String? = null,
    public val date: String? = null,
    public val noData: String? = null,
    public val details: String? = null,
    public val clearSelection: String? = null,
    public val total: String? = null,
    public val share: ((value: String, share: String) -> String)? = null,
)
