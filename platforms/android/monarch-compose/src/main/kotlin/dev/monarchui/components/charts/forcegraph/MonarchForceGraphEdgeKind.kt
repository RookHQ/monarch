package dev.monarchui.components.charts.forcegraph

import androidx.compose.runtime.Immutable
import dev.monarchui.components.charts.chartlegend.MonarchChartColor

/**
 * A kind of link in a `MonarchForceGraph`, listed in a second legend with its
 * line style.
 *
 * @property key the key that edges name in `kind`.
 * @property label the name in the legend and in what TalkBack reads. Already localised.
 * @property color the palette slot of the line. `null` is `Neutral`.
 * @property dashed draws the line dashed, a second key besides colour.
 */
@Immutable
public class MonarchForceGraphEdgeKind(
    public val key: String,
    public val label: String,
    public val color: MonarchChartColor? = null,
    public val dashed: Boolean = false,
)
