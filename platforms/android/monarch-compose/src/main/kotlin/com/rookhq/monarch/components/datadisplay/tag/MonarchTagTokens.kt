package com.rookhq.monarch.components.datadisplay.tag

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchTag, built from primitives and roles only. */
internal object MonarchTagTokens {
    val HorizontalPadding: Dp = MonarchTokens.Space.space2.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space0_5.dp
    val ContentGap: Dp = MonarchTokens.Space.space1.dp

    /** The visible minimum height of a read-only tag. Text that wraps makes it taller. */
    fun minHeight(size: MonarchTagSize): Dp = when (size) {
        MonarchTagSize.Sm -> MonarchTokens.Space.space6.dp
        MonarchTagSize.Md -> MonarchTokens.Control.xs.dp
    }

    fun textStyle(size: MonarchTagSize, typography: MonarchTypography): TextStyle = when (size) {
        MonarchTagSize.Sm -> typography.caption
        MonarchTagSize.Md -> typography.label
    }

    /**
     * The colours of a read-only or removable tag in [tone]: the tinted fill,
     * strong text and border of the tone. The neutral tag is the muted fill
     * with default text, as on the web.
     */
    fun colors(tone: MonarchTone, colors: MonarchColors): MonarchSurfaceColors {
        if (tone == MonarchTone.Neutral) {
            return MonarchSurfaceColors(colors.bgMuted, colors.textDefault, colors.borderSubtle)
        }
        val palette = MonarchTonePalette.of(tone, colors)
        return MonarchSurfaceColors(palette.subtle, palette.strong, palette.border)
    }

    /**
     * The colours of a tag that cannot be used. The text keeps the muted text
     * colour, not the disabled one, because the label is still information.
     */
    fun disabledColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.bgMuted, colors.textMuted, colors.borderSubtle)

    /** The colours of a selectable tag: an outlined filter when off, the accent when on. */
    fun selectableColors(
        selected: Boolean,
        enabled: Boolean,
        colors: MonarchColors,
        neutral: Boolean = false,
    ): MonarchSurfaceColors = when {
        !enabled -> disabledColors(colors)
        selected && neutral -> MonarchSurfaceColors(colors.bgMuted, colors.textDefault, colors.borderDefault)
        selected -> MonarchSurfaceColors(colors.accentSubtle, colors.accentStrong, colors.accentSolid)
        else -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderDefault)
    }

    fun border(colors: MonarchSurfaceColors): BorderStroke =
        BorderStroke(MonarchShapeDefaults.BorderWidth, colors.border ?: Color.Transparent)
}
