package com.rookhq.monarch.components.charts.heatmap

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameLabels

/**
 * Replacements for the built-in strings of a `MonarchHeatmap`. Every `null`
 * uses the built-in string of `strings_heatmap.xml`. Sentences with values
 * are format strings with `%1$s`-style placeholders, in the order given for
 * each.
 *
 * @property frame the strings of the chart frame. Its `legend` also names the Less to More legend.
 * @property rowHeader the table heading of the row names: "Row".
 * @property less the start of the legend: "Less".
 * @property more the end of the legend: "More".
 * @property noData the text of a cell without a value: "No data".
 * @property rangeBelow the range of the lightest step, from its upper bound: "Below %1$s".
 * @property above the range of the lightest step when every threshold is zero: "Above %1$s".
 * @property range the range of a middle step, from its bounds: "%1$s to %2$s".
 * @property rangeAbove the range of the darkest step, from its lower bound: "%1$s or more".
 * @property rowCount writes the number of rows in the summary: "7 rows".
 * @property columnCount writes the number of columns in the summary: "24 columns".
 * @property size the first sentence of the summary, from the rows and the columns: "%1$s by %2$s".
 * @property highest the sentence of the highest cell, from its description: "Highest: %1$s".
 * @property cell what TalkBack reads for a cell, from the row, the column and the value: "%1$s, %2$s: %3$s".
 * @property withNote a value followed by its note: "%1$s, %2$s".
 * @property select the TalkBack action that selects a cell when the heatmap has `onSelect`: "Select".
 */
@Immutable
public class MonarchHeatmapLabels(
    public val frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
    public val rowHeader: String? = null,
    public val less: String? = null,
    public val more: String? = null,
    public val noData: String? = null,
    public val rangeBelow: String? = null,
    public val above: String? = null,
    public val range: String? = null,
    public val rangeAbove: String? = null,
    public val rowCount: ((rows: Int) -> String)? = null,
    public val columnCount: ((columns: Int) -> String)? = null,
    public val size: String? = null,
    public val highest: String? = null,
    public val cell: String? = null,
    public val withNote: String? = null,
    public val select: String? = null,
)
