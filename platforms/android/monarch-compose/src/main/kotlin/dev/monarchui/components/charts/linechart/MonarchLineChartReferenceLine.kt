package dev.monarchui.components.charts.linechart

import androidx.compose.runtime.Immutable

/**
 * A dashed horizontal line across a `MonarchLineChart` at one value, such as a target, a baseline or a service
 * level. It is included in the value axis and read in the spoken summary.
 *
 * @property value where the line is drawn, on the value axis.
 * @property label the text written at the end of the line and read with its value, such as "Target".
 */
@Immutable
public class MonarchLineChartReferenceLine(
    public val value: Double,
    public val label: String? = null,
)
