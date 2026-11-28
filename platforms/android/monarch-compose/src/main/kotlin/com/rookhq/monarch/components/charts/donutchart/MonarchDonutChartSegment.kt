package com.rookhq.monarch.components.charts.donutchart

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor

/**
 * One part of the whole in a `MonarchDonutChart`.
 *
 * @property key the stable id of the segment.
 * @property label the name in the legend, the table, the summary and TalkBack. Already localised.
 * @property value the amount. Negative values and values that are not finite count as 0.
 * @property color the palette slot. `null` uses the position after sorting: the first five take `Data1` to
 * `Data5`. Pin it so a colour follows the same entity across charts.
 */
@Immutable
public class MonarchDonutChartSegment(
    public val key: String,
    public val label: String,
    public val value: Double,
    public val color: MonarchChartColor? = null,
)
