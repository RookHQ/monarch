package dev.monarchui.components.charts.linechart

import androidx.compose.runtime.Immutable
import dev.monarchui.components.charts.chartlegend.MonarchChartColor

/**
 * One measure of a `MonarchLineChart`: a line through one value per x value.
 *
 * @property key the stable id of the series; the value kept in the hidden series set.
 * @property label the name of the series in the legend, the table, the summary and TalkBack. Already localised.
 * @property values one value per x value, in order. `null`, or a value that is not finite, is a gap: the line
 * breaks there and a lone value between gaps is drawn as a dot. Values beyond the x values are ignored.
 * @property color the palette slot. `null` uses the position of the series: the first five take `Data1` to
 * `Data5`, later ones `Neutral`, as the legend does.
 * @property dashed draws the line and its legend marker dashed, such as for a previous period or a forecast,
 * so the series is told apart without colour.
 */
@Immutable
public class MonarchLineChartSeries(
    public val key: String,
    public val label: String,
    public val values: List<Double?>,
    public val color: MonarchChartColor? = null,
    public val dashed: Boolean = false,
)
