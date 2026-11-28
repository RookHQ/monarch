package dev.monarchui.components.collaboration.recordingindicator

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.text.format.DateUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** A change of the recording state that is announced once. */
internal enum class RecordingChange { Started, Stopped, Paused, Resumed }

/** Component tokens and rules of MonarchRecordingIndicator, built from primitives and roles only. */
internal object MonarchRecordingIndicatorTokens {
    val MinHeight: Dp = MonarchTokens.Space.space6.dp
    val HorizontalPadding: Dp = MonarchTokens.Space.space2.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space0_5.dp
    val ContentGap: Dp = MonarchTokens.Space.space1_5.dp
    val Gap: Dp = MonarchTokens.Space.space1.dp

    /** The recording dot and the paused square. They grow with the font size setting. */
    val MarkSize: Dp = MonarchTokens.Space.space2.dp

    /** The square inside the stop button. */
    val StopGlyphSize: Dp = MonarchTokens.Space.space2_5.dp

    const val LABEL_WEIGHT: Int = MonarchTokens.FontWeight.semibold
    const val TIME_WEIGHT: Int = MonarchTokens.FontWeight.medium

    /** The dot fades to this opacity halfway through a pulse, as on the web. */
    const val PULSE_LOW_ALPHA: Float = 0.35f
    const val PULSE_DURATION_MILLIS: Int = MonarchTokens.Duration.shimmer

    const val MILLIS_PER_SECOND: Long = 1_000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3_600L

    /** The colours of the badge. */
    fun colors(variant: MonarchRecordingIndicatorVariant, colors: MonarchColors): MonarchSurfaceColors =
        when (variant) {
            MonarchRecordingIndicatorVariant.Subtle ->
                MonarchSurfaceColors(colors.dangerSubtle, colors.dangerStrong, colors.dangerBorder)
            MonarchRecordingIndicatorVariant.Solid ->
                MonarchSurfaceColors(colors.bgInverse, colors.textInverse, null)
        }

    /** The colour of the recording dot: the danger fill, or its inverse form on the inverse surface. */
    fun dotColor(variant: MonarchRecordingIndicatorVariant, colors: MonarchColors): Color = when (variant) {
        MonarchRecordingIndicatorVariant.Subtle -> colors.dangerSolid
        MonarchRecordingIndicatorVariant.Solid -> colors.dangerStrongInverse
    }

    /** The colours of the stop button: a surface with a border and the strong danger glyph. */
    fun stopColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.bgSurface, colors.dangerStrong, colors.borderDefault)

    /** The recorded time: [elapsedMillis] when given, else the time since [since], never negative. */
    fun recordedMillis(since: Long?, elapsedMillis: Long?, now: Long): Long = when {
        elapsedMillis != null -> maxOf(0L, elapsedMillis)
        since != null -> maxOf(0L, now - since)
        else -> 0L
    }

    /** Whether the time is shown: [showElapsed] when given, else whenever a time source is. */
    fun showsTime(showElapsed: Boolean?, since: Long?, elapsedMillis: Long?): Boolean =
        showElapsed ?: (since != null || elapsedMillis != null)

    /** Whether the time advances by itself: recording, not paused, counted from [since]. */
    fun ticks(recording: Boolean, paused: Boolean, since: Long?, elapsedMillis: Long?): Boolean =
        recording && !paused && elapsedMillis == null && since != null

    /** The change to announce between two states, or `null` for none. Pausing only counts while recording. */
    fun change(wasRecording: Boolean, wasPaused: Boolean, recording: Boolean, paused: Boolean): RecordingChange? =
        when {
            recording != wasRecording -> if (recording) RecordingChange.Started else RecordingChange.Stopped
            recording && paused != wasPaused -> if (paused) RecordingChange.Paused else RecordingChange.Resumed
            else -> null
        }

    /** The time as digits in the user's locale, "00:42" or "1:02:03". */
    fun clock(millis: Long): String = DateUtils.formatElapsedTime(millis / MILLIS_PER_SECOND)

    /** The time in words for TalkBack, "1 minute, 5 seconds", in [locale]. */
    // At most three measures: the copy made by the spread is negligible.
    @Suppress("SpreadOperator")
    fun spokenTime(millis: Long, locale: Locale = Locale.getDefault()): String {
        val total = millis / MILLIS_PER_SECOND
        val hours = total / SECONDS_PER_HOUR
        val minutes = total % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        val seconds = total % SECONDS_PER_MINUTE
        val parts = buildList {
            if (hours > 0) add(Measure(hours, MeasureUnit.HOUR))
            if (minutes > 0) add(Measure(minutes, MeasureUnit.MINUTE))
            if (seconds > 0 || isEmpty()) add(Measure(seconds, MeasureUnit.SECOND))
        }
        return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE).formatMeasures(*parts.toTypedArray())
    }
}
