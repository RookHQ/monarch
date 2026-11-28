package dev.monarchui.components.charts.gauge

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/**
 * The colours of a gauge.
 *
 * @property track the empty track.
 * @property value the value arc or the needle's band colour.
 * @property mark the target tick and the needle.
 */
@Immutable
internal data class MonarchGaugeColors(val track: Color, val value: Color, val mark: Color)

/** The scale of a gauge after its ends are put in order. */
internal data class MonarchGaugeScale(val low: Double, val high: Double) {
    /** The share of the scale at [value], limited to 0..1. */
    fun share(value: Double): Float = ((value.coerceIn(low, high) - low) / (high - low)).toFloat()

    /** The progress range TalkBack reads, with [value] limited to the scale. */
    fun rangeInfo(value: Double): ProgressBarRangeInfo =
        ProgressBarRangeInfo(value.coerceIn(low, high).toFloat(), low.toFloat()..high.toFloat())

    companion object {
        /** Puts the ends in order and widens a scale whose ends are equal by one. */
        fun of(range: ClosedFloatingPointRange<Double>): MonarchGaugeScale {
            val low = minOf(range.start, range.endInclusive)
            val high = if (range.endInclusive > range.start) range.endInclusive else low + 1.0
            return MonarchGaugeScale(low, high)
        }
    }
}

/** Component tokens and the pure rules of MonarchGauge. */
internal object MonarchGaugeTokens {
    /** The track is 7% as thick as the dial is wide. */
    const val THICKNESS = 0.07f

    /** Above this font scale the value is written under the dial so it is never clipped. */
    const val VALUE_INSIDE_UP_TO_FONT_SCALE = 1.3f

    val BandStroke: Dp = MonarchTokens.Space.space0_5.dp
    val MarkStroke: Dp = MonarchTokens.Space.space0_5.dp
    val Gap: Dp = MonarchTokens.Space.space1.dp
    val TextGap: Dp = MonarchTokens.Space.space0_5.dp

    /** Whether the value is written inside the dial: with an arc, up to a font scale of 1.3. */
    fun valueInside(indicator: MonarchGaugeIndicator, fontScale: Float): Boolean =
        indicator == MonarchGaugeIndicator.Arc && fontScale <= VALUE_INSIDE_UP_TO_FONT_SCALE

    fun width(size: MonarchGaugeSize): Dp = when (size) {
        MonarchGaugeSize.Sm -> (MonarchTokens.Space.space24 + MonarchTokens.Space.space6).dp
        MonarchGaugeSize.Md -> (MonarchTokens.Space.space20 * 2).dp
        MonarchGaugeSize.Lg -> (MonarchTokens.Space.space24 * 2 + MonarchTokens.Space.space2).dp
    }

    /**
     * The band [value] falls in. A value on a boundary belongs to the upper
     * band, and the end of the scale to the band that reaches it.
     */
    fun bandAt(bands: List<MonarchGaugeBand>, value: Double, scale: MonarchGaugeScale): MonarchGaugeBand? =
        bands.lastOrNull { band ->
            val from = band.range.start
            val to = band.range.endInclusive
            value >= from && (value < to || (to >= scale.high && value <= to))
        }

    /**
     * The tone of the value: [tone] when given; otherwise the tone of the band
     * the value is in, Neutral when it is in none, and `null` (the accent)
     * when there are no bands. Without a value the gauge is Neutral.
     */
    fun resolvedTone(
        tone: MonarchTone?,
        hasValue: Boolean,
        band: MonarchGaugeBand?,
        hasBands: Boolean,
    ): MonarchTone? =
        when {
            tone != null -> tone
            !hasValue -> MonarchTone.Neutral
            band != null -> band.tone
            hasBands -> MonarchTone.Neutral
            else -> null
        }

    /** The colour of a tone on the dial: the solid colour of a status tone, the strong border for neutral. */
    fun toneColor(tone: MonarchTone?, colors: MonarchColors): Color = when (tone) {
        null -> colors.accentSolid
        MonarchTone.Neutral -> colors.borderStrong
        else -> MonarchTonePalette.of(tone, colors).solid
    }

    fun colors(tone: MonarchTone?, colors: MonarchColors): MonarchGaugeColors =
        MonarchGaugeColors(track = colors.bgMuted, value = toneColor(tone, colors), mark = colors.textDefault)
}
