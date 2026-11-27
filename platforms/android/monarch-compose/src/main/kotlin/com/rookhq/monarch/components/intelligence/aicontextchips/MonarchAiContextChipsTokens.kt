package com.rookhq.monarch.components.intelligence.aicontextchips

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiContextChips. */
internal object MonarchAiContextChipsTokens {
    /** The gap between chips. */
    val Gap: Dp = MonarchTokens.Space.space1_5.dp

    /** The gap between the parts of a chip. */
    val ContentGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The padding at the start and end of a chip that cannot be removed. */
    val HorizontalPadding: Dp = MonarchTokens.Space.space2_5.dp

    /** The padding above and below the content of a chip that cannot be removed. */
    val VerticalPadding: Dp = MonarchTokens.Space.space0_5.dp

    /** The least height of the visible chip. */
    val MinHeight: Dp = MonarchTokens.Control.xs.dp

    /** The widest the name grows before it is cut in the middle: 192dp. */
    val LabelMaxWidth: Dp = (MonarchTokens.Space.space24 * 2).dp

    /** The width of one bar of the weight mark. */
    val BarWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The gap between the bars of the weight mark. */
    val BarGap: Dp = MonarchTokens.Space.px.dp

    /** The heights of the three bars of the weight mark, shortest first. */
    val BarHeights: List<Dp> = listOf(
        MonarchTokens.Space.space1.dp,
        MonarchTokens.Space.space1_5.dp,
        MonarchTokens.Space.space2.dp,
    )

    /** The number of filled bars of the weight mark: one, two or three. */
    fun filledBars(weight: MonarchAiContextWeight): Int = when (weight) {
        MonarchAiContextWeight.Light -> 1
        MonarchAiContextWeight.Medium -> 2
        MonarchAiContextWeight.Heavy -> BarHeights.size
    }

    /** The colour of the bar at [index] of the weight mark. */
    fun barColor(index: Int, weight: MonarchAiContextWeight, colors: MonarchColors): Color =
        if (index < filledBars(weight)) colors.aiStrong else colors.borderDefault

    /** The colour of the token count: heavy items are drawn in the default text colour. */
    fun tokensColor(weight: MonarchAiContextWeight, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        weight == MonarchAiContextWeight.Heavy -> colors.textDefault
        else -> colors.textMuted
    }

    /** The colours of a chip: the AI subtle fill and AI border; muted when disabled. */
    fun chipColors(enabled: Boolean, colors: MonarchColors): MonarchSurfaceColors =
        if (enabled) {
            MonarchSurfaceColors(colors.aiSubtle, colors.textDefault, colors.aiBorder)
        } else {
            MonarchSurfaceColors(colors.bgMuted, colors.textDisabled, colors.borderSubtle)
        }

    /** The colour of the kind icon. */
    fun iconColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.aiStrong else colors.textDisabled
}
