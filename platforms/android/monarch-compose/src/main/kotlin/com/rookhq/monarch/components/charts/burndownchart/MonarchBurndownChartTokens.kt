package com.rookhq.monarch.components.charts.burndownchart

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartTokens
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The colours of a burndown's own marks.
 *
 * @property remaining the line of work left: the first data colour.
 * @property ideal the dashed ideal line: the neutral series colour.
 * @property scope the scope step line and its change dots: the second data colour.
 * @property changeText the "+5" labels of scope changes.
 * @property today the dashed today marker.
 * @property todayText the label of the today marker.
 */
@Immutable
internal data class MonarchBurndownChartColors(
    val remaining: Color,
    val ideal: Color,
    val scope: Color,
    val changeText: Color,
    val today: Color,
    val todayText: Color,
)

/** Component tokens and the pure colour rules of MonarchBurndownChart. Axis and line sizes are the line chart's. */
internal object MonarchBurndownChartTokens {
    /** The palette slot of the work left. */
    val RemainingSlot: MonarchChartColor = MonarchChartColor.Data1

    /** The palette slot of the ideal line. */
    val IdealSlot: MonarchChartColor = MonarchChartColor.Neutral

    /** The palette slot of the scope line. */
    val ScopeSlot: MonarchChartColor = MonarchChartColor.Data2

    /** The thickness of the scope step line: thinner than the work left, so the two differ without colour. */
    val ScopeStroke: Dp = MonarchTokens.Space.px.dp

    /** The radius of a scope change dot. */
    val ChangeDotRadius: Dp = MonarchLineChartTokens.DotRadius

    /**
     * The colours of the burndown: the work left and the scope in the first two data colours, the ideal in the
     * neutral series colour, change labels in the default text colour, the today marker in the strong border
     * colour with a muted label.
     */
    fun colors(colors: MonarchColors): MonarchBurndownChartColors = MonarchBurndownChartColors(
        remaining = MonarchLineChartTokens.lineColor(RemainingSlot, colors),
        ideal = MonarchLineChartTokens.lineColor(IdealSlot, colors),
        scope = MonarchLineChartTokens.lineColor(ScopeSlot, colors),
        changeText = colors.textDefault,
        today = colors.borderStrong,
        todayText = colors.textMuted,
    )
}
