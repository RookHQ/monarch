package com.rookhq.monarch.components.charts.donutchart

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameLabels

/**
 * Replacements for the built-in strings of a `MonarchDonutChart`. Every `null` uses the built-in string named
 * after it in `strings_donutchart.xml`.
 *
 * @property frame the strings of the chart frame: "View as table", "Loading chart", "No data yet" and the
 * others.
 * @property other the name of the slice the smallest segments fold into: "Other".
 * @property segment the heading of the segment column of the table: "Segment".
 * @property value the heading of the value column of the table: "Value".
 * @property share the heading of the share column of the table: "Share".
 * @property total the last row of the table and the start of the summary: "Total".
 */
@Immutable
public class MonarchDonutChartLabels(
    public val frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
    public val other: String? = null,
    public val segment: String? = null,
    public val value: String? = null,
    public val share: String? = null,
    public val total: String? = null,
)
