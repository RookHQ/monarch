package com.rookhq.monarch.internal.scheduling

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Calendar arithmetic of month grids (MonarchMiniMonth, MonarchTeamCalendar). A day is the epoch millisecond of
 * its midnight in UTC, as in the Material 3 `DatePickerState`; a month is its
 * first day. `java.time` needs API 26, so this uses `java.util.Calendar` in
 * UTC.
 */
internal object MonarchCalendarDates {
    const val DAY_MILLIS: Long = 86_400_000L
    const val DAYS_PER_WEEK: Int = 7
    private const val MONTHS_PER_YEAR = 12
    private const val FIXED_WEEKS = 6
    private const val ISO_MINIMAL_DAYS = 4
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    private fun calendar(millis: Long): Calendar =
        Calendar.getInstance(utc, Locale.ROOT).apply { timeInMillis = millis }

    /** The day that contains [millis]. */
    fun dayOf(millis: Long): Long = Math.floorDiv(millis, DAY_MILLIS) * DAY_MILLIS

    /** The first day of the month that contains [millis]. */
    fun monthOf(millis: Long): Long = calendar(dayOf(millis)).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

    /** The month [count] months after [month]; negative goes back. */
    fun addMonths(month: Long, count: Int): Long =
        calendar(monthOf(month)).apply { add(Calendar.MONTH, count) }.timeInMillis

    /** A number that grows by one per month, to compare months. */
    fun monthIndex(millis: Long): Int =
        calendar(millis).let { it.get(Calendar.YEAR) * MONTHS_PER_YEAR + it.get(Calendar.MONTH) }

    /** The day of the month, 1 to 31. */
    fun dayOfMonth(day: Long): Int = calendar(day).get(Calendar.DAY_OF_MONTH)

    /** The day of the week of [day], `Calendar.SUNDAY` to `Calendar.SATURDAY`. */
    fun dayOfWeek(day: Long): Int = calendar(day).get(Calendar.DAY_OF_WEEK)

    /**
     * The weeks shown for [month]: rows of seven days that start on
     * [firstDayOfWeek] (`Calendar.SUNDAY` to `Calendar.SATURDAY`), with the
     * neighbouring months' days around the month. Six rows when [fixedWeeks],
     * otherwise only the rows the month needs.
     */
    fun weeks(month: Long, firstDayOfWeek: Int, fixedWeeks: Boolean): List<List<Long>> {
        val first = monthOf(month)
        val offset = Math.floorMod(dayOfWeek(first) - firstDayOfWeek, DAYS_PER_WEEK)
        val length = calendar(first).getActualMaximum(Calendar.DAY_OF_MONTH)
        val rows = if (fixedWeeks) FIXED_WEEKS else (offset + length + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK
        val start = first - offset * DAY_MILLIS
        return List(rows) { row ->
            List(DAYS_PER_WEEK) { column -> start + (row * DAYS_PER_WEEK + column) * DAY_MILLIS }
        }
    }

    /** The ISO 8601 week number of a row: the week of its Thursday. */
    fun isoWeek(row: List<Long>): Int {
        val thursday = row.first { dayOfWeek(it) == Calendar.THURSDAY }
        return Calendar.getInstance(utc, Locale.ROOT).apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = ISO_MINIMAL_DAYS
            timeInMillis = thursday
        }.get(Calendar.WEEK_OF_YEAR)
    }

    /** Today on the device's calendar, as the UTC midnight of that date. */
    fun today(now: Long = System.currentTimeMillis()): Long {
        val local = Calendar.getInstance().apply { timeInMillis = now }
        return Calendar.getInstance(utc, Locale.ROOT).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
}
