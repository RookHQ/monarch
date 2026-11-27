package com.rookhq.monarch.components.datadisplay.progress

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchControlMetrics
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchStillProgressFraction
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The colours of a progress indicator. */
internal data class MonarchProgressColors(val indicator: Color, val track: Color, val value: Color)

/** Component tokens of MonarchProgress, built from primitives and roles only. */
internal object MonarchProgressTokens {
    /** The gap between a bar and its value. */
    val ValueGap: Dp = MonarchTokens.Space.space2.dp

    /** The sliding segment of an indeterminate bar: 40 % of the track, as on the web. */
    const val SEGMENT_FRACTION: Float = 0.4f

    /** One slide of the indeterminate segment across the bar: 1.4s, as on the web. */
    const val SLIDE_MILLIS: Int = 1400

    /** One turn of an indeterminate ring: 0.7s, like the spinner. */
    const val TURN_MILLIS: Int = 700

    /** The filled part of a still indeterminate indicator, shown when animations are off. */
    const val STILL_FRACTION: Float = MonarchStillProgressFraction

    /**
     * The indicator in the tone's strong colour on the emphasis track, as the web draws it: the strong role is the
     * lighter shade in the dark themes, so the arc and the bar stay bright on a dark surface. Neutral uses the muted
     * text colour, as a status-free bar should not look like the inverse surface.
     */
    fun colors(tone: MonarchTone, colors: MonarchColors): MonarchProgressColors =
        MonarchProgressColors(
            indicator = MonarchTonePalette.of(tone, colors).strong,
            track = colors.bgEmphasis,
            value = colors.textDefault,
        )

    /** The thickness of a bar: the web's 4dp (small) and 8dp (default), and 12dp for a large bar. */
    fun barHeight(size: MonarchProgressSize): Dp = when (size) {
        MonarchProgressSize.Sm -> MonarchControlMetrics.ProgressBarSmall
        MonarchProgressSize.Md -> MonarchControlMetrics.ProgressBarMedium
        MonarchProgressSize.Lg -> MonarchTokens.Space.space3.dp
        MonarchProgressSize.Xl -> MonarchTokens.Space.space4.dp
    }

    /** The diameter of a ring. */
    fun ringDiameter(size: MonarchProgressSize): Dp = when (size) {
        MonarchProgressSize.Sm -> MonarchTokens.Space.space6.dp
        MonarchProgressSize.Md -> MonarchTokens.Space.space10.dp
        MonarchProgressSize.Lg -> MonarchTokens.Space.space16.dp
        MonarchProgressSize.Xl -> MonarchTokens.Space.space24.dp
    }

    /** The stroke of a ring. */
    fun ringStroke(size: MonarchProgressSize): Dp = when (size) {
        MonarchProgressSize.Sm -> MonarchTokens.Space.space0_5.dp
        MonarchProgressSize.Md -> MonarchTokens.Space.space1.dp
        MonarchProgressSize.Lg -> MonarchTokens.Space.space1_5.dp
        MonarchProgressSize.Xl -> MonarchTokens.Space.space2.dp
    }

    /** Whether the value is written inside the ring rather than after it: only a large ring has the room. */
    fun valueInside(variant: MonarchProgressVariant, size: MonarchProgressSize): Boolean =
        variant == MonarchProgressVariant.Circular &&
            (size == MonarchProgressSize.Lg || size == MonarchProgressSize.Xl)
}
