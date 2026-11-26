package com.rookhq.monarch.components.charts.barchart

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor

/**
 * One measure of a `MonarchBarChart`: a bar per category.
 *
 * @property key the stable id of the series; the value kept in the hidden series set.
 * @property label the name of the series in the legend, the table, the summary and TalkBack. Already localised.
 * @property values one value per category, in category order. `null`, or a value that is not finite, is a gap
 * and draws no bar.
 * @property color the palette slot. `null` uses the position of the series: the first five take `Data1` to
 * `Data5`, later ones `Neutral`, as the legend does.
 */
@Immutable
public class MonarchBarChartSeries(
    public val key: String,
    public val label: String,
    public val values: List<Double?>,
    public val color: MonarchChartColor? = null,
)
