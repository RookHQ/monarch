package dev.monarchui.components.charts.areachart

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.components.charts.linechart.MonarchLineChartTokens
import dev.monarchui.theme.MonarchColors

/** The colour rules of MonarchAreaChart. Sizes, axis and line colours come from the line chart core. */
internal object MonarchAreaChartTokens {
    /** How far a stacked band moves from its series colour towards the surface: stronger than an overlapping fill. */
    const val STACK_MIX: Float = 0.6f

    /** How far the fill of a dashed series moves towards the surface when the areas overlap: fainter. */
    const val DASHED_MIX: Float = 0.9f

    /**
     * The fill of a series: its data colour mixed towards the surface colour, opaque. Overlapping fills use the
     * line chart's mix (fainter for a dashed series); stacked bands, which touch, are stronger (a dashed series
     * takes the overlapping mix). Monarch never fills with transparency.
     */
    fun areaColor(slot: MonarchChartColor, stacked: Boolean, dashed: Boolean, colors: MonarchColors): Color {
        val mix = when {
            stacked && dashed -> MonarchLineChartTokens.AREA_MIX
            stacked -> STACK_MIX
            dashed -> DASHED_MIX
            else -> MonarchLineChartTokens.AREA_MIX
        }
        return lerp(MonarchLineChartTokens.lineColor(slot, colors), colors.bgSurface, mix)
    }
}
