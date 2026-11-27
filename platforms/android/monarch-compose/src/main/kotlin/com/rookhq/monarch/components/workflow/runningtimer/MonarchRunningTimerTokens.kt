package com.rookhq.monarch.components.workflow.runningtimer

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** The action a timer is running. */
internal enum class RunningTimerAction { Start, Stop, StopAt, Discard }

/** Why a stop time cannot be saved. */
internal enum class RunningTimerStopError { Future, TooEarly }

/**
 * The colours of a timer.
 *
 * @property container the indicator's fill.
 * @property content the indicator's text.
 * @property clock the panel's clock.
 * @property warning the long-running warning: the strong warning colour.
 * @property error a failure: the strong danger colour.
 */
@Immutable
internal data class RunningTimerColors(
    val container: Color,
    val content: Color,
    val clock: Color,
    val warning: Color,
    val error: Color,
)

/** Component tokens and clock arithmetic of MonarchRunningTimer, built from primitives and roles only. */
internal object MonarchRunningTimerTokens {
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val SmallGap: Dp = MonarchTokens.Space.space1.dp
    val IndicatorPadding: Dp = MonarchTokens.Space.space2.dp

    const val SECOND_MILLIS: Long = 1_000L
    const val MINUTE_MILLIS: Long = 60_000L
    const val HOUR_MILLIS: Long = 3_600_000L

    /** The default warning: twelve hours. */
    const val DEFAULT_WARN_AFTER_MILLIS: Long = 12 * HOUR_MILLIS

    private const val SECONDS_PER_MINUTE = 60
    private const val MINUTES_PER_HOUR = 60
    private const val SECONDS_PER_HOUR = 3_600

    /** Whole seconds between [start] and [now]; 0 for a start in the future. */
    fun elapsedSeconds(start: Long, now: Long): Long = maxOf(0L, (now - start) / SECOND_MILLIS)

    /** A stopwatch reading: "0:04:09", "12:30:00", "100:00:00". */
    fun clock(seconds: Long): String {
        val hours = seconds / SECONDS_PER_HOUR
        val minutes = seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        val rest = seconds % SECONDS_PER_MINUTE
        return "$hours:${minutes.toString().padStart(2, '0')}:${rest.toString().padStart(2, '0')}"
    }

    /** Whole minutes as "2h 05m"; [dropZeroMinutes] writes "12h" for "12h 00m". */
    fun hoursMinutes(minutes: Long, dropZeroMinutes: Boolean = false): String {
        val whole = maxOf(0L, minutes)
        val rest = whole % MINUTES_PER_HOUR
        val hours = "${whole / MINUTES_PER_HOUR}h"
        return if (dropZeroMinutes && rest == 0L) hours else "$hours ${rest.toString().padStart(2, '0')}m"
    }

    /** The reading in words in [locale]: "4 minutes, 9 seconds", "1 hour, 0 minutes, 5 seconds". */
    fun spoken(seconds: Long, locale: Locale): String {
        val hours = seconds / SECONDS_PER_HOUR
        val minutes = seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        val rest = seconds % SECONDS_PER_MINUTE
        val format = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE)
        val second = Measure(rest, MeasureUnit.SECOND)
        val minute = Measure(minutes, MeasureUnit.MINUTE)
        return when {
            hours > 0 -> format.formatMeasures(Measure(hours, MeasureUnit.HOUR), minute, second)
            minutes > 0 -> format.formatMeasures(minute, second)
            else -> format.formatMeasures(second)
        }
    }

    /** Whether the timer has run longer than [warnAfter] milliseconds. */
    fun longRunning(start: Long?, now: Long, warnAfter: Long?): Boolean =
        start != null && warnAfter != null && now - start > warnAfter

    /** The stop time the earlier-stop dialog suggests: an hour after the start, or now if that is sooner. */
    fun suggestion(start: Long, now: Long): Long = minOf(now, start + HOUR_MILLIS)

    /**
     * The instant a chosen time of day means: the latest moment at or before
     * [now] whose local time in [zone] is [hour]:[minute].
     */
    fun resolve(hour: Int, minute: Int, now: Long, zone: TimeZone): Long {
        val calendar = Calendar.getInstance(zone, Locale.ROOT).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis > now) calendar.add(Calendar.DAY_OF_MONTH, -1)
        return calendar.timeInMillis
    }

    /** The hour and minute of [millis] in [zone]. */
    fun hourMinute(millis: Long, zone: TimeZone): Pair<Int, Int> {
        val calendar = Calendar.getInstance(zone, Locale.ROOT).apply { timeInMillis = millis }
        return calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
    }

    /** Checks a stop time: not after [now] and at least a minute after [start]. */
    fun stopError(at: Long, start: Long, now: Long): RunningTimerStopError? = when {
        at > now -> RunningTimerStopError.Future
        at - start < MINUTE_MILLIS -> RunningTimerStopError.TooEarly
        else -> null
    }

    /**
     * The colours: the accent while running, warning colours once long
     * running, danger colours after a failure and the muted colours while
     * stopped. The panel's clock uses the strong colour of the same state.
     */
    fun colors(colors: MonarchColors, running: Boolean, long: Boolean, failed: Boolean): RunningTimerColors {
        val (container, content) = when {
            failed -> colors.dangerSubtle to colors.dangerStrong
            !running -> colors.bgMuted to colors.textMuted
            long -> colors.warningSubtle to colors.warningStrong
            else -> colors.accentSubtle to colors.accentStrong
        }
        return RunningTimerColors(container, content, content, colors.warningStrong, colors.dangerStrong)
    }
}
