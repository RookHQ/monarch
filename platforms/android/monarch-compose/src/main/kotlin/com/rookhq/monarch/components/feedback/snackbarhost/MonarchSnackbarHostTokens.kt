package com.rookhq.monarch.components.feedback.snackbarhost

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.rookhq.monarch.foundation.MonarchElevation
import com.rookhq.monarch.foundation.MonarchShadow
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The colours of a snackbar in one tone. */
@Immutable
internal data class MonarchSnackbarColors(
    val container: Color,
    val content: Color,
    val icon: Color,
    val border: Color,
)

/**
 * Component tokens of MonarchSnackbarHost, from the web's `toast.css`: a
 * panel up to 384dp wide on the raised background with a subtle hairline,
 * 12dp corners and one soft shadow (none in a dark theme), a 20dp status icon
 * in the tone colour, 14 text, and the action as a small ghost button.
 */
internal object MonarchSnackbarHostTokens {
    /** The web's `min(24rem, 100vw - 2rem)`. */
    val MaxWidth: Dp = (MonarchTokens.Space.space24 * 4).dp

    /** Around the panel, inside the host. */
    val OuterHorizontal: Dp = MonarchTokens.Space.space4.dp
    val OuterVertical: Dp = MonarchTokens.Space.space3.dp

    /** Inside the panel: 16 before the icon, 4 after the last button, whose target adds its own space. */
    val PaddingStart: Dp = MonarchTokens.Space.space4.dp
    val PaddingEnd: Dp = MonarchTokens.Space.space1.dp
    val PaddingVertical: Dp = MonarchTokens.Space.space1.dp

    /** Between the icon, the message and the buttons. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The status icon. */
    val IconSize: Dp = MonarchTokens.Space.space5.dp

    /** The cross of the close button, drawn smaller so its stroke matches the outlined icons. */
    val CloseSize: Dp = MonarchTokens.Space.space4.dp

    /** The ring of a drawn status icon, as a fraction of its size, and the inset of its glyph. */
    const val RING_STROKE: Float = 0.075f
    const val GLYPH_INSET: Float = 0.2f

    /** The message: 14 regular with the normal line height. */
    fun messageStyle(typography: MonarchTypography): TextStyle = typography.body.copy(
        fontSize = MonarchTokens.FontSize.sm.sp,
        lineHeight = MonarchTokens.LineHeight.normal.em,
    )

    /**
     * The colours of a snackbar: every tone is the raised surface with the
     * default text and a subtle border (strong in the high-contrast themes);
     * the tone colours only the icon, in its strong role, so the message keeps
     * the full text contrast.
     */
    fun colors(tone: MonarchTone, colors: MonarchColors, highContrast: Boolean = false): MonarchSnackbarColors =
        MonarchSnackbarColors(
            container = colors.bgRaised,
            content = colors.textDefault,
            icon = if (tone == MonarchTone.Neutral) colors.textMuted else MonarchTonePalette.of(tone, colors).strong,
            border = if (highContrast) colors.borderStrong else colors.borderSubtle,
        )

    /** The one soft shadow of a presented surface, in the light themes only. */
    fun shadow(elevation: MonarchElevation, dark: Boolean): MonarchShadow? = if (dark) null else elevation.md

    /**
     * How long a snackbar stays: longer for a danger tone and for one with an
     * action, so there is time to read and act, as on the web.
     */
    fun defaultDuration(tone: MonarchTone, actionLabel: String?): SnackbarDuration =
        if (actionLabel != null || tone == MonarchTone.Danger) SnackbarDuration.Long else SnackbarDuration.Short
}
