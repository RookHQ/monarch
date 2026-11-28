package dev.monarchui.components.charts.barchart

import androidx.compose.runtime.Immutable

/**
 * A dashed line across the bars of a `MonarchBarChart` at one value, such as a target or an average. It is
 * included in the value axis and read in the spoken summary.
 *
 * @property value where the line is drawn, on the value axis.
 * @property label the text written at the end of the line and read with its value, such as "Target".
 */
@Immutable
public class MonarchBarChartReferenceLine(
    public val value: Double,
    public val label: String? = null,
)
