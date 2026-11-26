package com.rookhq.monarch.components.workflow.slaindicator

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** A positive span of time in whole minutes, split into at most two units. */
internal data class SlaDuration(val days: Long, val hours: Long, val minutes: Long) {
    /** Under one minute. */
    val isUnderAMinute: Boolean get() = days == 0L && hours == 0L && minutes == 0L
}

/** Component tokens of MonarchSlaIndicator, built from primitives and roles only. */
internal object MonarchSlaIndicatorTokens {
    val MinHeight: Dp = MonarchTokens.Space.space6.dp
    val HorizontalPadding: Dp = MonarchTokens.Space.space2.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space0_5.dp
    val Gap: Dp = MonarchTokens.Space.space1.dp
    const val LABEL_WEIGHT: Int = MonarchTokens.FontWeight.medium
    const val NOT_APPLICABLE_WEIGHT: Int = MonarchTokens.FontWeight.regular
    const val MILLIS_PER_MINUTE: Long = 60_000L
    private const val MINUTES_PER_HOUR = 60L
    private const val MINUTES_PER_DAY = 1_440L

    /** The weight of the text: regular for not applicable, medium otherwise. */
    fun weight(state: MonarchSlaIndicatorState): Int =
        if (state == MonarchSlaIndicatorState.NotApplicable) NOT_APPLICABLE_WEIGHT else LABEL_WEIGHT

    /** Whether the target's clock runs. */
    fun isRunning(state: MonarchSlaIndicatorState): Boolean =
        state == MonarchSlaIndicatorState.OnTrack || state == MonarchSlaIndicatorState.AtRisk

    /**
     * The state to show. A running target whose due time has passed is
     * breached even if the server has not caught up; an on-track target due
     * within [warnBeforeMillis] is at risk. Paused, met, breached and not
     * applicable are never changed.
     */
    fun resolve(
        state: MonarchSlaIndicatorState,
        dueAt: Long?,
        now: Long,
        warnBeforeMillis: Long?,
    ): MonarchSlaIndicatorState {
        if (!isRunning(state) || dueAt == null) return state
        val remaining = dueAt - now
        return when {
            remaining <= 0 -> MonarchSlaIndicatorState.Breached
            state == MonarchSlaIndicatorState.OnTrack && warnBeforeMillis != null && remaining <= warnBeforeMillis ->
                MonarchSlaIndicatorState.AtRisk
            else -> state
        }
    }

    /** [millis] in whole minutes, rounded down, in at most two units: "3d 4h", "2h 15m", "45m". */
    fun duration(millis: Long): SlaDuration {
        val minutes = kotlin.math.abs(millis) / MILLIS_PER_MINUTE
        val days = minutes / MINUTES_PER_DAY
        val hours = minutes % MINUTES_PER_DAY / MINUTES_PER_HOUR
        return when {
            days > 0 -> SlaDuration(days, hours, 0)
            hours > 0 -> SlaDuration(0, hours, minutes % MINUTES_PER_HOUR)
            else -> SlaDuration(0, 0, minutes)
        }
    }

    /**
     * How long to wait before the shown text or state can change: the next
     * whole minute of the countdown or the overdue time, or the moment the
     * target becomes at risk, whichever is first.
     */
    fun nextTick(now: Long, dueAt: Long, warnBeforeMillis: Long?): Long {
        val remaining = dueAt - now
        var delay = if (remaining > 0) {
            remaining % MILLIS_PER_MINUTE + 1
        } else {
            MILLIS_PER_MINUTE - (now - dueAt) % MILLIS_PER_MINUTE
        }
        if (warnBeforeMillis != null) {
            val edge = dueAt - warnBeforeMillis - now
            if (edge in 1 until delay) delay = edge
        }
        return delay.coerceAtLeast(1)
    }

    /**
     * The colours of the indicator. The chip has a fill for at risk (warning
     * subtle with a border), breached (danger solid) and paused (muted); the
     * text variant only colours the text.
     */
    fun colors(
        state: MonarchSlaIndicatorState,
        variant: MonarchSlaIndicatorVariant,
        colors: MonarchColors,
    ): MonarchSurfaceColors {
        val chip = variant == MonarchSlaIndicatorVariant.Chip
        return when (state) {
            MonarchSlaIndicatorState.OnTrack -> MonarchSurfaceColors(Color.Transparent, colors.textMuted, null)
            MonarchSlaIndicatorState.AtRisk -> if (chip) {
                MonarchSurfaceColors(colors.warningSubtle, colors.warningStrong, colors.warningBorder)
            } else {
                MonarchSurfaceColors(Color.Transparent, colors.warningStrong, null)
            }
            MonarchSlaIndicatorState.Breached -> if (chip) {
                MonarchSurfaceColors(colors.dangerSolid, colors.dangerOnSolid, null)
            } else {
                MonarchSurfaceColors(Color.Transparent, colors.dangerStrong, null)
            }
            MonarchSlaIndicatorState.Paused ->
                MonarchSurfaceColors(if (chip) colors.bgMuted else Color.Transparent, colors.textMuted, null)
            MonarchSlaIndicatorState.Met -> MonarchSurfaceColors(Color.Transparent, colors.successStrong, null)
            MonarchSlaIndicatorState.NotApplicable -> MonarchSurfaceColors(Color.Transparent, colors.textSubtle, null)
        }
    }
}

/**
 * Writes durations in the locale with ICU's MeasureFormat: narrow units for
 * the visible text ("2h 15m" in English), whole words for TalkBack
 * ("2 hours, 15 minutes").
 */
internal class SlaDurationFormat(
    locale: Locale,
    private val underAMinute: String,
    private val underAMinuteSpoken: String,
) {
    private val narrow = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.NARROW)
    private val wide = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE)

    /** The visible duration. */
    fun visible(millis: Long): String = format(millis, narrow, underAMinute)

    /** The duration as TalkBack reads it. */
    fun spoken(millis: Long): String = format(millis, wide, underAMinuteSpoken)

    private fun format(millis: Long, format: MeasureFormat, under: String): String {
        val duration = MonarchSlaIndicatorTokens.duration(millis)
        if (duration.isUnderAMinute) return under
        val measures = listOf(
            duration.days to MeasureUnit.DAY,
            duration.hours to MeasureUnit.HOUR,
            duration.minutes to MeasureUnit.MINUTE,
        ).filter { it.first > 0 }.map { Measure(it.first, it.second) }
        // At most two units, so the measures are passed one by one.
        return if (measures.size == 1) {
            format.formatMeasures(measures[0])
        } else {
            format.formatMeasures(measures[0], measures[1])
        }
    }
}
