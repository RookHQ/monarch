package dev.monarchui.components.charts.calendarheatmap

import androidx.compose.runtime.Immutable
import dev.monarchui.components.charts.chartframe.MonarchChartFrameLabels

/**
 * Replacements for the built-in strings of a `MonarchCalendarHeatmap`. Every `null` uses the built-in string of
 * `strings_calendarheatmap.xml`, or of `strings_heatmap.xml` for the legend words it shares with
 * `MonarchHeatmap`. Sentences with values are format strings with `%1$s`-style placeholders.
 *
 * @property frame the strings of the chart frame. Its `legend` also names the Less to More legend.
 * @property less the start of the legend: "Less".
 * @property more the end of the legend: "More".
 * @property noData the text of a day without a value: "No data".
 * @property rangeBelow the range of the lightest step, from its upper bound: "Below %1$s".
 * @property above the range of the lightest step when every threshold is zero: "Above %1$s".
 * @property range the range of a middle step, from its bounds: "%1$s to %2$s".
 * @property rangeAbove the range of the darkest step, from its lower bound: "%1$s or more".
 * @property date the table heading of the dates: "Date".
 * @property value the table heading of the values: "Value".
 * @property note the table heading of the notes: "Note".
 * @property today read and written after the value of today: "Today".
 * @property inactive read and written after the value of a non-working day: "Non-working day".
 * @property marker read and written after the value of a flagged day: "Marked".
 * @property dayCount writes the number of days in the summary: "181 days".
 * @property summary the first sentence of the summary, from the day count, the first day, the last day and the
 * total: "%1$s from %2$s to %3$s. Total %4$s".
 * @property highest the sentence of the highest day, from its description: "Highest: %1$s".
 * @property select the TalkBack action that selects a day when the chart has `onSelect`: "Select".
 */
@Immutable
public class MonarchCalendarHeatmapLabels(
    public val frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
    public val less: String? = null,
    public val more: String? = null,
    public val noData: String? = null,
    public val rangeBelow: String? = null,
    public val above: String? = null,
    public val range: String? = null,
    public val rangeAbove: String? = null,
    public val date: String? = null,
    public val value: String? = null,
    public val note: String? = null,
    public val today: String? = null,
    public val inactive: String? = null,
    public val marker: String? = null,
    public val dayCount: ((days: Int) -> String)? = null,
    public val summary: String? = null,
    public val highest: String? = null,
    public val select: String? = null,
)
