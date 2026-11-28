package dev.monarchui.components.feedback.inlinenotice

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchInlineNotice, built from primitives and roles only. */
internal object MonarchInlineNoticeTokens {
    val ContentGap: Dp = MonarchTokens.Space.space2.dp

    fun padding(variant: MonarchInlineNoticeVariant): PaddingValues = when (variant) {
        MonarchInlineNoticeVariant.Subtle ->
            PaddingValues(horizontal = MonarchTokens.Space.space2_5.dp, vertical = MonarchTokens.Space.space1_5.dp)
        MonarchInlineNoticeVariant.Plain -> PaddingValues(MonarchTokens.Space.space0.dp)
    }

    /**
     * The fill and the icon colour. The subtle notice has the tinted fill of
     * the tone (the muted background for neutral); the plain one has none.
     */
    fun colors(tone: MonarchTone, variant: MonarchInlineNoticeVariant, colors: MonarchColors): MonarchSurfaceColors {
        val palette = MonarchTonePalette.of(tone, colors)
        val container = when (variant) {
            MonarchInlineNoticeVariant.Subtle -> palette.subtle
            MonarchInlineNoticeVariant.Plain -> Color.Transparent
        }
        return MonarchSurfaceColors(container = container, content = palette.strong, border = null)
    }

    /** The text colour: default text on the tinted strip, the strong tone colour when plain. */
    fun textColor(tone: MonarchTone, variant: MonarchInlineNoticeVariant, colors: MonarchColors): Color =
        when (variant) {
            MonarchInlineNoticeVariant.Subtle -> colors.textDefault
            MonarchInlineNoticeVariant.Plain -> MonarchTonePalette.of(tone, colors).strong
        }
}
