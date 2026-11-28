package com.rookhq.monarch.components.collaboration.audiolevelmeter

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import kotlin.math.roundToInt

/**
 * The colours of an audio level meter.
 *
 * @property on segments that are on, and the fill of the bar.
 * @property off segments that are off, drawn as dots.
 * @property track the track of the bar.
 */
@Immutable
internal data class MonarchAudioLevelMeterColors(val on: Color, val off: Color, val track: Color)

/** Component tokens and the pure level rules of MonarchAudioLevelMeter. */
internal object MonarchAudioLevelMeterTokens {
    val SegmentWidth: Dp = MonarchTokens.Space.space1.dp
    val SegmentGap: Dp = MonarchTokens.Space.space0_5.dp
    val BarHeight: Dp = MonarchTokens.Space.space1.dp

    /** At or above this level the meter shows sound as on when animations are removed. */
    const val SPEAKING_LEVEL = 0.08f

    fun height(size: MonarchAudioLevelMeterSize): Dp = when (size) {
        MonarchAudioLevelMeterSize.Sm -> MonarchTokens.Space.space3.dp
        MonarchAudioLevelMeterSize.Md -> MonarchTokens.Space.space4.dp
    }

    /** The level limited to 0..1; a level that is not finite, or a muted meter, is silence. */
    fun level(level: Float, muted: Boolean): Float = if (muted || !level.isFinite()) 0f else level.coerceIn(0f, 1f)

    /** The level that is drawn: the level, or only on and off when animations are removed. */
    fun shown(level: Float, reducedMotion: Boolean): Float = when {
        !reducedMotion -> level
        level >= SPEAKING_LEVEL -> 1f
        else -> 0f
    }

    /** The number of segments; at least one. */
    fun count(segments: Int): Int = segments.coerceAtLeast(1)

    /** How many segments are on at [shown]. */
    fun lit(shown: Float, count: Int): Int = (shown * count).roundToInt().coerceIn(0, count)

    /** The level as a whole percentage, for the spoken value. */
    fun percent(level: Float): Int = (level * PERCENT).roundToInt()

    fun colors(muted: Boolean, colors: MonarchColors): MonarchAudioLevelMeterColors =
        if (muted) {
            MonarchAudioLevelMeterColors(on = colors.textDisabled, off = colors.textDisabled, track = colors.bgMuted)
        } else {
            MonarchAudioLevelMeterColors(on = colors.successSolid, off = colors.borderStrong, track = colors.bgMuted)
        }

    private const val PERCENT = 100
}
