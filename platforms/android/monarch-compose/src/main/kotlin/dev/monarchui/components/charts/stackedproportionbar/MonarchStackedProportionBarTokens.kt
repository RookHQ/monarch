package dev.monarchui.components.charts.stackedproportionbar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.components.charts.chartlegend.MonarchChartLegendTokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure colour rules of MonarchStackedProportionBar. */
internal object MonarchStackedProportionBarTokens {
    /** The space between the bar and the legend. */
    val Gap: Dp = MonarchTokens.Space.space2_5.dp

    /** The space between two parts of the bar. */
    val PartGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The narrowest a part with a value is drawn, so a tiny share stays visible. */
    val MinPart: Dp = MonarchTokens.Space.space1.dp

    /** The thickness of the bar. */
    fun height(size: MonarchStackedProportionBarSize): Dp = when (size) {
        MonarchStackedProportionBarSize.Sm -> MonarchTokens.Space.space1.dp
        MonarchStackedProportionBarSize.Md -> MonarchTokens.Space.space2.dp
        MonarchStackedProportionBarSize.Lg -> MonarchTokens.Space.space3.dp
    }

    /** The empty track and the remainder: the muted background. */
    fun track(colors: MonarchColors): Color = colors.bgMuted

    /** The outline of the remainder's legend marker, which names the empty track. */
    fun remainderMarker(colors: MonarchColors): Color = colors.borderStrong

    /** The "No data yet" text: muted. */
    fun emptyText(colors: MonarchColors): Color = colors.textMuted

    /**
     * The colour of a part: in status mode the solid colour of its tone (`Neutral` takes the subtle text colour,
     * as the web does); otherwise its palette slot's data colour.
     */
    fun partColor(slot: MonarchChartColor, tone: MonarchTone?, colors: MonarchColors): Color = when (tone) {
        null -> MonarchChartLegendTokens.color(slot, colors)
        MonarchTone.Neutral -> colors.textSubtle
        else -> MonarchTonePalette.of(tone, colors).solid
    }
}
