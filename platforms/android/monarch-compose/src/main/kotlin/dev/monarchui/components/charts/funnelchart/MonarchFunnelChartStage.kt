package dev.monarchui.components.charts.funnelchart

import androidx.compose.runtime.Immutable
import dev.monarchui.components.charts.chartlegend.MonarchChartColor

/**
 * One step of a `MonarchFunnelChart`.
 *
 * @property label the name of the step, such as "Visited pricing". Already localised.
 * @property value how many reached this step. Negative and non-finite values count as zero.
 * @property key the stable id of the step, passed back by `onSelect`. Defaults to the label, so give a key
 * when two steps share a label.
 * @property color the palette slot of the bar. `null` uses `Data1` for every stage: a funnel is one measure.
 */
@Immutable
public class MonarchFunnelChartStage(
    public val label: String,
    public val value: Double,
    public val key: String = label,
    public val color: MonarchChartColor? = null,
)
