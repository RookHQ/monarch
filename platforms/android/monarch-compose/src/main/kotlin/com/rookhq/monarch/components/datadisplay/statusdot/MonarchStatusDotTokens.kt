package com.rookhq.monarch.components.datadisplay.statusdot

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchStatusDot, built from primitives and roles only. */
internal object MonarchStatusDotTokens {
    /** How far the pulse ring grows, as a multiple of the dot. */
    const val PULSE_SCALE: Float = 2.4f

    /** The opacity the pulse ring starts at; it fades to none as it grows. */
    const val PULSE_START_ALPHA: Float = 0.6f

    /** The duration of one pulse. */
    const val PULSE_DURATION_MILLIS: Int = MonarchTokens.Duration.shimmer

    /** The diameter of the dot at the default font size. It grows with the font size setting. */
    fun dotSize(size: MonarchStatusDotSize): Dp = when (size) {
        MonarchStatusDotSize.Sm -> MonarchTokens.Space.space2.dp
        MonarchStatusDotSize.Md -> MonarchTokens.Space.space2_5.dp
    }

    /** The gap between the dot and the label. */
    fun gap(size: MonarchStatusDotSize): Dp = when (size) {
        MonarchStatusDotSize.Sm -> MonarchTokens.Space.space1_5.dp
        MonarchStatusDotSize.Md -> MonarchTokens.Space.space2.dp
    }

    /** The text style of the label: caption for `Sm`, the label size at regular weight for `Md`. */
    fun textStyle(size: MonarchStatusDotSize, typography: MonarchTypography): TextStyle = when (size) {
        MonarchStatusDotSize.Sm -> typography.caption
        MonarchStatusDotSize.Md -> typography.label.copy(fontWeight = FontWeight(MonarchTokens.FontWeight.regular))
    }

    /**
     * The colour of the dot: the solid colour of the tone. Neutral uses the
     * subtle text colour, as on the web, because the inverse surface would be
     * as loud as an alert.
     */
    fun dotColor(tone: MonarchTone, colors: MonarchColors): Color =
        if (tone == MonarchTone.Neutral) colors.textSubtle else MonarchTonePalette.of(tone, colors).solid

    /** Whether the pulse ring is drawn: only when asked for and animations are on. */
    fun showsPulse(pulsing: Boolean, reducedMotion: Boolean): Boolean = pulsing && !reducedMotion
}
