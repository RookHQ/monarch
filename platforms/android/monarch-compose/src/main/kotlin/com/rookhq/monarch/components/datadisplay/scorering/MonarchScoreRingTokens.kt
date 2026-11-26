package com.rookhq.monarch.components.datadisplay.scorering

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
import kotlin.math.roundToLong

/** The colours of the arc and the number of a score ring. */
internal data class ScoreRingColors(val arc: Color, val text: Color, val track: Color)

/** Component tokens and rules of MonarchScoreRing, built from primitives and roles only. */
internal object MonarchScoreRingTokens {
    /** The scale used when the given maximum is zero, negative or not finite. */
    const val DEFAULT_MAX: Double = 100.0

    /** The stroke is a tenth of the diameter, as on the web, and never thinner than [MinStroke]. */
    const val STROKE_FRACTION: Float = 0.1f

    /** Shown in the centre when there is no score. Decorative: TalkBack reads "No score yet". */
    const val NO_VALUE_MARK: String = "\u2013"

    val MinStroke: Dp = MonarchTokens.Space.space1.dp

    /** No gap between the arc and the track, as on the web. */
    val ArcGap: Dp = MonarchTokens.Space.space0.dp
    val Gap: Dp = MonarchTokens.Space.space1.dp

    /** The diameter at the default font size; it grows with the font size setting. */
    fun diameter(size: MonarchScoreRingSize): Dp = when (size) {
        MonarchScoreRingSize.Sm -> MonarchTokens.Space.space12.dp
        MonarchScoreRingSize.Md -> MonarchTokens.Space.space20.dp
        MonarchScoreRingSize.Lg -> (MonarchTokens.Space.space24 + MonarchTokens.Space.space6).dp
    }

    /** The stroke width for a ring of [diameter]. */
    fun stroke(diameter: Dp): Dp = maxOf(MinStroke, diameter * STROKE_FRACTION)

    /** The style of the number in the centre, semibold, about a third of the diameter as on the web. */
    fun numberStyle(size: MonarchScoreRingSize, typography: MonarchTypography): TextStyle {
        val base = when (size) {
            MonarchScoreRingSize.Sm -> typography.label
            MonarchScoreRingSize.Md -> typography.title
            MonarchScoreRingSize.Lg -> typography.display
        }
        return base.copy(fontWeight = FontWeight(MonarchTokens.FontWeight.semibold))
    }

    /** Whether "/ 100" is shown: as asked, or by default on `Md` and `Lg`; never without a score. */
    fun showsMax(showMax: Boolean?, size: MonarchScoreRingSize, hasValue: Boolean): Boolean =
        hasValue && (showMax ?: (size != MonarchScoreRingSize.Sm))

    /** The maximum actually used: [max] when it is finite and above zero, else 100. */
    fun safeMax(max: Double): Double = if (max.isFinite() && max > 0) max else DEFAULT_MAX

    /** The score limited to 0 through [max], or `null` when there is none or it is not a number. */
    fun clamp(value: Double?, max: Double): Double? = value?.takeIf { it.isFinite() }?.coerceIn(0.0, max)

    /** The tone of the band [value] falls in, checked from the highest minimum; neutral when none matches. */
    fun bandTone(value: Double, thresholds: List<MonarchScoreRingThreshold>): MonarchTone =
        thresholds.sortedByDescending { it.minimum }.firstOrNull { value >= it.minimum }?.tone ?: MonarchTone.Neutral

    /** The tone of the ring, or `null` for the accent colour. Without a score the auto colour is neutral. */
    fun tone(color: MonarchScoreRingColor, value: Double?, thresholds: List<MonarchScoreRingThreshold>): MonarchTone? =
        when (color) {
            MonarchScoreRingColor.Auto -> value?.let { bandTone(it, thresholds) } ?: MonarchTone.Neutral
            MonarchScoreRingColor.Accent -> null
            is MonarchScoreRingColor.Tone -> color.tone
        }

    /**
     * The arc, number and track colours. The arc takes the solid colour of the
     * tone and the number its strong colour; the neutral tone uses muted text
     * for the arc and default text for the number, as on the web.
     */
    fun colors(tone: MonarchTone?, colors: MonarchColors): ScoreRingColors = when (tone) {
        null -> ScoreRingColors(colors.accentSolid, colors.accentStrong, colors.bgMuted)
        MonarchTone.Neutral -> ScoreRingColors(colors.textMuted, colors.textDefault, colors.bgMuted)
        else -> MonarchTonePalette.of(tone, colors).let { ScoreRingColors(it.solid, it.strong, colors.bgMuted) }
    }

    /** The default number format: the whole number in the current locale. */
    fun formatWhole(value: Double): String = java.text.NumberFormat.getIntegerInstance().format(value.roundToLong())
}
