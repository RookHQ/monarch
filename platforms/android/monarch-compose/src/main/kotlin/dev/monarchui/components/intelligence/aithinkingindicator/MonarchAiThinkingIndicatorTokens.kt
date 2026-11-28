package dev.monarchui.components.intelligence.aithinkingindicator

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and the pure rules of MonarchAiThinkingIndicator. */
internal object MonarchAiThinkingIndicatorTokens {
    /** The gap between the mark, the label and the elapsed time. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** One sweep of the shimmer, or one pulse of the mark: the shimmer duration. */
    const val CYCLE_MILLIS: Int = MonarchTokens.Duration.shimmer

    /** The smallest scale of the mark while it pulses. */
    const val PULSE_LOW_SCALE: Float = 0.85f

    /** The default delay before the elapsed time appears: 3 seconds, as on the web. */
    const val DEFAULT_SHOW_ELAPSED_AFTER_MILLIS: Long = 3_000L

    const val MILLIS_PER_SECOND: Long = 1_000L
    private const val SECONDS_PER_MINUTE = 60

    /** Whole seconds from [startedAt] to [now]. A start in the future counts as 0. */
    fun elapsedSeconds(startedAt: Long, now: Long): Int =
        ((now - startedAt).coerceAtLeast(0L) / MILLIS_PER_SECOND).toInt()

    /** Whether the elapsed time is shown after [seconds], given the delay in milliseconds. */
    fun showsElapsed(seconds: Int, showElapsedAfterMillis: Long): Boolean =
        seconds.toLong() * MILLIS_PER_SECOND >= showElapsedAfterMillis

    /** The milliseconds until the next whole second after [startedAt]. */
    fun untilNextSecond(startedAt: Long, now: Long): Long {
        val into = (now - startedAt).coerceAtLeast(0L) % MILLIS_PER_SECOND
        return MILLIS_PER_SECOND - into
    }

    /**
     * Whole seconds as the narrow unit format of [locale]: "5s", "1m 5s" in
     * English, as on the web and iOS.
     */
    fun elapsedText(seconds: Int, locale: Locale): String {
        val format = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.NARROW)
        val minutes = seconds / SECONDS_PER_MINUTE
        val rest = seconds % SECONDS_PER_MINUTE
        val secondsPart = Measure(rest, MeasureUnit.SECOND)
        return when {
            minutes == 0 -> format.formatMeasures(secondsPart)
            rest == 0 -> format.formatMeasures(Measure(minutes, MeasureUnit.MINUTE))
            else -> format.formatMeasures(Measure(minutes, MeasureUnit.MINUTE), secondsPart)
        }
    }

    /** The resting colour of the label: muted text. Also the label colour when animations are off. */
    fun labelColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the band that sweeps through the label. */
    fun shimmerColor(colors: MonarchColors): Color = colors.aiStrong

    /** The colour of the elapsed time. */
    fun elapsedColor(colors: MonarchColors): Color = colors.textSubtle

    /** The content colour given to the mark. */
    fun markColor(colors: MonarchColors): Color = colors.aiStrong

    /**
     * The start x of the shimmer band at [progress] (0 to 1) across a label
     * [width] pixels wide. The band sweeps from before the start edge to after
     * the end edge, so from right to left in a right-to-left layout.
     */
    fun bandStart(progress: Float, width: Float, rightToLeft: Boolean): Float {
        val ltr = -width + 2 * width * progress
        return if (rightToLeft) -ltr else ltr
    }
}
