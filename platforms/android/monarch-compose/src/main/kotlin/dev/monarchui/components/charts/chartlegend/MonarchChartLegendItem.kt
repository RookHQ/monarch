package dev.monarchui.components.charts.chartlegend

import androidx.compose.runtime.Immutable

/**
 * One series in a `MonarchChartLegend`.
 *
 * @property key the stable id of the series; the value kept in the hidden series set.
 * @property label the visible name of the series. It is always shown, so a series is never identified by
 * colour alone. Already localised.
 * @property color the palette slot. `null` uses the position of the entry: the first five entries take
 * `Data1` to `Data5`, later ones `Neutral`, as the charts do.
 * @property dashed draws the marker dashed (line) or outlined (square), to match a dashed series.
 * @property value a value after the label, such as the series total, already formatted.
 */
@Immutable
public class MonarchChartLegendItem(
    public val key: String,
    public val label: String,
    public val color: MonarchChartColor? = null,
    public val dashed: Boolean = false,
    public val value: String? = null,
)
