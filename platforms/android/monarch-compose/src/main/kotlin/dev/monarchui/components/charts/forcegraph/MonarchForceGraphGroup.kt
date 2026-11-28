package dev.monarchui.components.charts.forcegraph

import androidx.compose.runtime.Immutable
import dev.monarchui.components.charts.chartlegend.MonarchChartColor

/**
 * A group of nodes in a `MonarchForceGraph`, drawn in one palette colour and
 * listed in the legend.
 *
 * @property key the key that nodes name in `group`.
 * @property label the name in the legend, the details and what TalkBack reads. Already localised.
 * @property color the palette slot. `null` uses the group's position: `Data1` to `Data5`, then `Neutral`.
 */
@Immutable
public class MonarchForceGraphGroup(
    public val key: String,
    public val label: String,
    public val color: MonarchChartColor? = null,
)
