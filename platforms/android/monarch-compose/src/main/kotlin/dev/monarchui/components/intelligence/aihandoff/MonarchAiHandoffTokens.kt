package dev.monarchui.components.intelligence.aihandoff

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiHandoff. */
internal object MonarchAiHandoffTokens {
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val PictureGap: Dp = MonarchTokens.Space.space1.dp
    val BodyGap: Dp = MonarchTokens.Space.space2.dp
    val TextGap: Dp = MonarchTokens.Space.space0_5.dp
    val HorizontalPadding: Dp = MonarchTokens.Space.space2_5.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space1.dp
    val VerticalMargin: Dp = MonarchTokens.Space.space2.dp
    val LineWidth: Dp = MonarchTokens.Space.px.dp
    val LineMinLength: Dp = MonarchTokens.Space.space4.dp
    val Dash: Dp = MonarchTokens.Space.space1.dp

    /** Below this width the side lines give their room to the sentence, as on the web's narrow layout. */
    val LinesMinWidth: Dp = MonarchTokens.Breakpoint.sm.dp
    const val NAME_WEIGHT: Int = MonarchTokens.FontWeight.semibold

    /** The border and the dashed lines: the AI border for an agent, the default border for a person. */
    fun borderColor(toPerson: Boolean, colors: MonarchColors): Color =
        if (toPerson) colors.borderDefault else colors.aiBorder

    /** The fill of the marker. */
    fun container(colors: MonarchColors): Color = colors.bgSurface

    /** The sentence. */
    fun sentenceColor(colors: MonarchColors): Color = colors.textDefault

    /** The reason. */
    fun reasonColor(colors: MonarchColors): Color = colors.textMuted

    /** The time. */
    fun timeColor(colors: MonarchColors): Color = colors.textSubtle

    /** The widest the marker may be beside the lines: the width less two shortest lines and their gaps. */
    fun bodyMaxWidth(width: Dp): Dp = width - (LineMinLength + Gap) * 2

    /** Whether the side lines are drawn at [width]. */
    fun showsLines(width: Dp): Boolean = width >= LinesMinWidth
}
