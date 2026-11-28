package com.rookhq.monarch.components.scheduling.weeklyhourseditor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Calendar

/** Component tokens and the pure logic of MonarchWeeklyHoursEditor. */
internal object MonarchWeeklyHoursEditorTokens {
    /** The gap between the days. */
    val DayGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the controls of one day. */
    val ControlGap: Dp = MonarchTokens.Space.space2.dp

    /** From this width a day is one row (switch, intervals, copy), as on the web; narrower it is stacked. */
    val WideWidth: Dp = MonarchTokens.Breakpoint.sm.dp

    /** The width of the switch and name column of a wide day. */
    val WideNameWidth: Dp = (MonarchTokens.Space.space20 * 2).dp

    /** Minutes in a day: the latest end. */
    const val MINUTES_PER_DAY: Int = 1440

    /** The default minutes between time options, as on the web. */
    const val DEFAULT_STEP: Int = 15

    /** The default most intervals per day, as on the web. */
    const val DEFAULT_MAX: Int = 6

    /** The default start of a day switched on for the first time: 09:00. */
    const val DEFAULT_START: Int = 540

    /** The default end of a day switched on for the first time: 17:00. */
    const val DEFAULT_END: Int = 1020

    private const val MAX_STEP = 60
    private const val DAYS = 7
    private const val HOUR = 60

    /** The colour of a day's error message. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong

    /** The colour of "Unavailable" and of the read-only hours. */
    fun noteColor(colors: MonarchColors): Color = colors.textMuted

    /** The step between time options: 1 to 60 minutes; zero or a negative step falls back to 15, as on the web. */
    fun step(step: Int): Int = if (step >= 1) minOf(MAX_STEP, step) else DEFAULT_STEP

    /** The seven ISO weekdays in the order shown, starting at [firstDayOfWeek] (a `Calendar` constant). */
    fun order(firstDayOfWeek: Int): List<Int> {
        // ISO weekdays: Monday is 1 and Sunday 7, where Calendar counts Sunday 1 to Saturday 7.
        val calendarDay = firstDayOfWeek.coerceIn(Calendar.SUNDAY, Calendar.SATURDAY)
        val first = if (calendarDay == Calendar.SUNDAY) DAYS else calendarDay - 1
        return List(DAYS) { ((first - 1 + it) % DAYS) + 1 }
    }

    /** The intervals of [day], in the order they were arranged. */
    fun dayOf(value: List<MonarchWeeklyHoursInterval>, day: Int): List<MonarchWeeklyHoursInterval> =
        value.filter { it.day == day }

    /**
     * [value] with the intervals of [day] replaced by [ranges], ordered by ISO weekday and, within a day, in the
     * order arranged. Intervals with a day outside 1 to 7 are dropped.
     */
    fun withDay(
        value: List<MonarchWeeklyHoursInterval>,
        day: Int,
        ranges: List<MonarchWeeklyHoursInterval>,
    ): List<MonarchWeeklyHoursInterval> = (1..DAYS).flatMap { d ->
        if (d == day) ranges.map { it.copy(day = d) } else dayOf(value, d)
    }

    /** Every day gets the intervals of [day]. */
    fun copiedToAll(value: List<MonarchWeeklyHoursInterval>, day: Int): List<MonarchWeeklyHoursInterval> {
        val ranges = dayOf(value, day)
        return (1..DAYS).flatMap { d -> ranges.map { it.copy(day = d) } }
    }

    /** The interval the add button appends: an hour after the last one and an hour long, within the day. */
    fun nextInterval(ranges: List<MonarchWeeklyHoursInterval>, day: Int, step: Int): MonarchWeeklyHoursInterval {
        val last = ranges.maxByOrNull { it.start }
            ?: return MonarchWeeklyHoursInterval(day, DEFAULT_START, DEFAULT_END)
        val start = minOf(last.end + HOUR, MINUTES_PER_DAY - step)
        return MonarchWeeklyHoursInterval(day, start, minOf(start + HOUR, MINUTES_PER_DAY))
    }

    /** The times a start can take: every [step] from midnight to a step before the end of the day, and [current]. */
    fun startOptions(step: Int, current: Int): List<Int> = options(0, MINUTES_PER_DAY - step, step, current)

    /** The times an end can take: every [step] from the first step to the end of the day, and [current]. */
    fun endOptions(step: Int, current: Int): List<Int> = options(step, MINUTES_PER_DAY, step, current)

    private fun options(min: Int, max: Int, step: Int, current: Int): List<Int> {
        val list = (0..MINUTES_PER_DAY step step).filter { it in min..max }
        return if (current in list) list else (list + current).sorted()
    }
}
