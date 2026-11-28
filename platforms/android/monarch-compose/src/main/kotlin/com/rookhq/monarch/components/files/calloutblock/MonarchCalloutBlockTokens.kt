package com.rookhq.monarch.components.files.calloutblock

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.internal.MonarchToneStrings
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchCalloutBlock, built from primitives and roles only. */
internal object MonarchCalloutBlockTokens {
    val HorizontalPadding: Dp = MonarchTokens.Space.space4.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space3.dp
    val IconGap: Dp = MonarchTokens.Space.space3.dp
    val ContentGap: Dp = MonarchTokens.Space.space2.dp

    /**
     * The colours of a callout block.
     *
     * @property container the fill.
     * @property border the 1dp outline.
     * @property icon the colour of the icon.
     * @property content the colour of the text.
     */
    data class Colors(val container: Color, val border: Color, val icon: Color, val content: Color)

    /**
     * The colours of a tone, from the shared tone palette. The neutral block
     * sits on the subtle background with the subtle border, as on the web.
     */
    fun colors(tone: MonarchTone, colors: MonarchColors): Colors {
        val palette = MonarchTonePalette.of(tone, colors)
        val neutral = tone == MonarchTone.Neutral
        return Colors(
            container = if (neutral) colors.bgSubtle else palette.subtle,
            border = if (neutral) colors.borderSubtle else palette.border,
            icon = palette.strong,
            content = colors.textDefault,
        )
    }

    /** The built-in spoken name of a tone, or `null` for the neutral and accent tones, which announce nothing. */
    @StringRes
    fun toneLabel(tone: MonarchTone): Int? = MonarchToneStrings.resource(tone)
}
