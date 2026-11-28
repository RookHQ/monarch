package dev.monarchui.components.charts.stackedbarchart

import androidx.compose.runtime.Immutable
import dev.monarchui.components.charts.barchart.MonarchBarChartLabels

/**
 * Replacements for the built-in strings of a `MonarchStackedBarChart`. Every `null` uses the built-in string.
 *
 * @property bar the strings of the underlying bar chart: the frame's, "Category", "Total", "No data", "Details",
 * "Clear" and "Highlighted".
 * @property shareSeries one series in the spoken summary of the 100% offset, from the series, its average share,
 * the category of its largest share and that share: "%1$s: average share %2$s, largest %3$s, %4$s".
 */
@Immutable
public class MonarchStackedBarChartLabels(
    public val bar: MonarchBarChartLabels = MonarchBarChartLabels(),
    public val shareSeries: String? = null,
)
