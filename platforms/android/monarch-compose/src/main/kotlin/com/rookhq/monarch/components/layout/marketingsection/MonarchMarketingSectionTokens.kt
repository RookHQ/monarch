package com.rookhq.monarch.components.layout.marketingsection

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of the marketing sections, built from primitives and roles only. */
internal object MonarchMarketingSectionTokens {
    val SectionPadding: Dp = MonarchTokens.Space.space6.dp
    val SectionGap: Dp = MonarchTokens.Space.space6.dp
    val HeaderGap: Dp = MonarchTokens.Space.space2.dp
    val CardPadding: Dp = MonarchTokens.Space.space4.dp
    val CardGap: Dp = MonarchTokens.Space.space2.dp
    val GridGap: Dp = MonarchTokens.Space.space4.dp

    /** The narrowest a feature card may be before the grid drops a column. */
    val MinCardWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space16).dp
    val FrameBarPadding: Dp = MonarchTokens.Space.space2.dp
    val FrameDot: Dp = MonarchTokens.Space.space2_5.dp
    val StatGap: Dp = MonarchTokens.Space.space8.dp

    /** The eyebrow above a title takes the accent, as on the web. */
    fun eyebrow(colors: MonarchColors): Color = colors.accentStrong

    /** The fill of a section: none, or the subtle background. */
    fun sectionFill(tone: MonarchMarketingSectionTone, colors: MonarchColors): Color =
        when (tone) {
            MonarchMarketingSectionTone.Default -> Color.Transparent
            MonarchMarketingSectionTone.Subtle -> colors.bgSubtle
        }

    /** The figure of a stat in the default text colour; its label is muted. */
    fun statValue(colors: MonarchColors): Color = colors.textDefault

    /** The band of a call to action: the subtle accent fill with its border. */
    fun ctaFill(colors: MonarchColors): Color = colors.accentSubtle
    fun ctaBorder(colors: MonarchColors): Color = colors.accentBorder
}
