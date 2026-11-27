package com.rookhq.monarch.components.layout.marketinghero

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchMarketingHero, built from primitives and roles only. */
internal object MonarchMarketingHeroTokens {
    val VerticalPadding: Dp = MonarchTokens.Space.space12.dp
    val HorizontalPadding: Dp = MonarchTokens.Space.space4.dp
    val Gap: Dp = MonarchTokens.Space.space4.dp
    val ActionGap: Dp = MonarchTokens.Space.space3.dp
    val MediaGap: Dp = MonarchTokens.Space.space8.dp

    /** The eyebrow takes the accent. */
    fun eyebrow(colors: MonarchColors): Color = colors.accentStrong

    /** The wash behind the hero: the subtle accent fading into the screen background, as the web's gradient. */
    fun wash(colors: MonarchColors): Brush = Brush.verticalGradient(listOf(colors.accentSubtle, colors.bgCanvas))
}
