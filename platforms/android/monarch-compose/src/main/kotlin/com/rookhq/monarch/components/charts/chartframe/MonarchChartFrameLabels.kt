package com.rookhq.monarch.components.charts.chartframe

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchChartFrame`. Every `null`
 * uses the built-in string named after it in `strings_chartframe.xml`.
 *
 * @property viewTable the toggle while the chart shows: "View as table".
 * @property viewChart the toggle while the table shows: "View as chart".
 * @property loading the spoken name of the loading skeleton: "Loading chart".
 * @property emptyTitle the title of the empty state: "No data yet".
 * @property error the message of the error status: "This chart didn't load.".
 * @property legend the spoken name of the legend: "Legend".
 * @property noValue the spoken text of a gap in the table: "No value".
 * @property showAll the legend button that shows every hidden series: "Show all".
 */
@Immutable
public class MonarchChartFrameLabels(
    public val viewTable: String? = null,
    public val viewChart: String? = null,
    public val loading: String? = null,
    public val emptyTitle: String? = null,
    public val error: String? = null,
    public val legend: String? = null,
    public val noValue: String? = null,
    public val showAll: String? = null,
)
