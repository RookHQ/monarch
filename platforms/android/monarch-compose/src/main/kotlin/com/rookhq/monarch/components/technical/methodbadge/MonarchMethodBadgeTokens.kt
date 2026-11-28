package com.rookhq.monarch.components.technical.methodbadge

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.MonarchToneEmphasis
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** Component tokens of MonarchMethodBadge, built from primitives and roles only. */
internal object MonarchMethodBadgeTokens {
    /** 56dp, 3.5rem on the web, so a column of methods lines up. Scaled with the font size. */
    val MinWidth: Dp = (MonarchTokens.Space.space12 + MonarchTokens.Space.space2).dp
    val MinHeight: Dp = MonarchTokens.Space.space6.dp
    val HorizontalPadding: Dp = MonarchTokens.Space.space1_5.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space0_5.dp
    const val FONT_WEIGHT: Int = MonarchTokens.FontWeight.semibold

    /** The visible text: the method trimmed and in upper case. */
    fun label(method: String): String = method.trim().uppercase(Locale.ROOT)

    /**
     * The colours of a method badge: the tinted fill, strong text and border
     * of the tone; the neutral tone has no border, as on the web.
     */
    fun colors(tone: MonarchTone, colors: MonarchColors): MonarchSurfaceColors {
        val palette = MonarchTonePalette.of(tone, colors)
        val subtle = palette.surface(MonarchToneEmphasis.Subtle)
        return if (tone == MonarchTone.Neutral) subtle else subtle.copy(border = palette.border)
    }
}
