package com.rookhq.monarch.components.feedback.banner

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.rookhq.monarch.foundation.MonarchTypography
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchGlyphs
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchBanner, built from primitives and roles only. */
internal object MonarchBannerTokens {
    val HorizontalPadding: Dp = MonarchTokens.Space.space4.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space3.dp
    val ContentGap: Dp = MonarchTokens.Space.space3.dp
    val TitleGap: Dp = MonarchTokens.Space.space0_5.dp

    /** Moves the 16dp icon down onto the first line of 14sp text, as the web's 0.125rem. */
    val IconTopOffset: Dp = MonarchTokens.Space.space0_5.dp

    /** The default icon of a tone, as the web alert's `defaultIcon`: danger shows the warning triangle too. */
    fun glyph(tone: MonarchTone): ImageVector = when (tone) {
        MonarchTone.Success -> MonarchGlyphs.CheckCircle
        MonarchTone.Warning, MonarchTone.Danger -> MonarchGlyphs.Warning
        else -> MonarchGlyphs.Info
    }

    /** The title: 14sp semibold, like the web alert. */
    fun titleStyle(typography: MonarchTypography): TextStyle =
        typography.label.copy(fontWeight = FontWeight.SemiBold)

    /** The message: 14sp, not the 16sp body of the screen. */
    fun messageStyle(typography: MonarchTypography): TextStyle = typography.body.copy(
        fontSize = MonarchTokens.FontSize.sm.sp,
        lineHeight = (MonarchTokens.FontSize.sm * MonarchTokens.LineHeight.normal).sp,
    )

    /**
     * The colours of a banner: the tinted background of the tone with its
     * border, and `content` for the icon. The neutral banner sits on the
     * subtle background rather than the muted one, as on the web.
     */
    fun colors(tone: MonarchTone, colors: MonarchColors): MonarchSurfaceColors {
        val palette = MonarchTonePalette.of(tone, colors)
        val container = if (tone == MonarchTone.Neutral) colors.bgSubtle else palette.subtle
        return MonarchSurfaceColors(container = container, content = palette.strong, border = palette.border)
    }
}
