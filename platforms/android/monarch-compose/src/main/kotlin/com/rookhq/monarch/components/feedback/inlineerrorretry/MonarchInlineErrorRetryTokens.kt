package com.rookhq.monarch.components.feedback.inlineerrorretry

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchInlineErrorRetry, built from primitives and roles only. */
internal object MonarchInlineErrorRetryTokens {
    val ColumnGap: Dp = MonarchTokens.Space.space3.dp
    val RowGap: Dp = MonarchTokens.Space.space2.dp
    val IconGap: Dp = MonarchTokens.Space.space2.dp

    fun padding(variant: MonarchInlineErrorRetryVariant): PaddingValues = when (variant) {
        MonarchInlineErrorRetryVariant.Inline -> PaddingValues(MonarchTokens.Space.space0.dp)
        MonarchInlineErrorRetryVariant.Banner -> PaddingValues(
            start = MonarchTokens.Space.space4.dp,
            end = MonarchTokens.Space.space3.dp,
            top = MonarchTokens.Space.space2_5.dp,
            bottom = MonarchTokens.Space.space2_5.dp,
        )
    }

    /**
     * The fill, icon colour and border. The inline row has no fill; the banner
     * has the tinted fill and border of the tone. The icon takes the strong
     * colour of the tone in both.
     */
    fun colors(
        tone: MonarchTone,
        variant: MonarchInlineErrorRetryVariant,
        colors: MonarchColors,
    ): MonarchSurfaceColors {
        val palette = MonarchTonePalette.of(tone, colors)
        return when (variant) {
            MonarchInlineErrorRetryVariant.Inline -> MonarchSurfaceColors(Color.Transparent, palette.strong, null)
            MonarchInlineErrorRetryVariant.Banner ->
                MonarchSurfaceColors(palette.subtle, palette.strong, palette.border)
        }
    }
}
