package com.rookhq.monarch.internal.scheduling

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

/**
 * Calendar arithmetic of month views (MonarchCalendarMonthView and its users). A day is a "day key": the
 * epoch millisecond of the UTC midnight of a date, as in the Material 3
 * `DatePickerState`; a month is the key of its first day. `java.time` needs
 * API 26, so this uses `java.util.Calendar`.
 */
internal object MonarchMonthGrid {
    const val DAY_MILLIS: Long = 86_400_000L
    const val DAYS_PER_WEEK: Int = 7
    private const val MONTHS_PER_YEAR = 12
    private const val FIXED_WEEKS = 6

    /** The first month the pager reaches: January 1900. */
    private const val FIRST_YEAR = 1900

    /** The number of months the pager offers: 1900 to 2299. */
    const val PAGE_COUNT: Int = 400 * MONTHS_PER_YEAR

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    private fun utcCalendar(millis: Long): Calendar =
        Calendar.getInstance(utc, Locale.ROOT).apply { timeInMillis = millis }

    /** The day key of the UTC date that contains [millis]. */
    fun dayOf(millis: Long): Long = Math.floorDiv(millis, DAY_MILLIS) * DAY_MILLIS

    /** The day key of the date that contains [millis] in [zone]. */
    fun dayOf(millis: Long, zone: TimeZone): Long {
        val local = Calendar.getInstance(zone, Locale.ROOT).apply { timeInMillis = millis }
        return Calendar.getInstance(utc, Locale.ROOT).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    /** The key of the first day of the month that contains the day key [day]. */
    fun monthOf(day: Long): Long = utcCalendar(dayOf(day)).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

    /** A number that grows by one per month, to compare months. */
    fun monthIndex(day: Long): Int = utcCalendar(day).let {
        // Years before the common era count down from 1; make them continue the count below year 1.
        val year = it.get(Calendar.YEAR).let { y -> if (it.get(Calendar.ERA) == GregorianCalendar.BC) 1 - y else y }
        year * MONTHS_PER_YEAR + it.get(Calendar.MONTH)
    }

    /** The pager page of the month that contains [day], kept inside the pager's range. */
    fun pageOf(day: Long): Int = (monthIndex(day) - FIRST_YEAR * MONTHS_PER_YEAR).coerceIn(0, PAGE_COUNT - 1)

    /** The first day of the month on pager page [page]. */
    fun monthOfPage(page: Int): Long {
        val index = FIRST_YEAR * MONTHS_PER_YEAR + page
        return Calendar.getInstance(utc, Locale.ROOT).apply {
            clear()
            set(index / MONTHS_PER_YEAR, index % MONTHS_PER_YEAR, 1)
        }.timeInMillis
    }

    /** The day of the month, 1 to 31. */
    fun dayOfMonth(day: Long): Int = utcCalendar(day).get(Calendar.DAY_OF_MONTH)

    /** The day of the week of [day], `Calendar.SUNDAY` to `Calendar.SATURDAY`. */
    fun dayOfWeek(day: Long): Int = utcCalendar(day).get(Calendar.DAY_OF_WEEK)

    /**
     * The weeks shown for the month of [month]: rows of seven days from
     * [firstDayOfWeek], with the neighbouring months' days around the month.
     * Six rows when [fixedWeeks], otherwise only the rows the month needs.
     */
    fun weeks(month: Long, firstDayOfWeek: Int, fixedWeeks: Boolean): List<List<Long>> {
        val first = monthOf(month)
        val offset = Math.floorMod(dayOfWeek(first) - firstDayOfWeek, DAYS_PER_WEEK)
        val length = utcCalendar(first).getActualMaximum(Calendar.DAY_OF_MONTH)
        val rows = if (fixedWeeks) FIXED_WEEKS else (offset + length + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK
        val start = first - offset * DAY_MILLIS
        return List(rows) { row ->
            List(DAYS_PER_WEEK) { column -> start + (row * DAYS_PER_WEEK + column) * DAY_MILLIS }
        }
    }

    /** Every day of the month of [month], at UTC midnight. */
    fun daysOfMonth(month: Long): List<Long> {
        val first = monthOf(month)
        val length = utcCalendar(first).getActualMaximum(Calendar.DAY_OF_MONTH)
        return List(length) { first + it * DAY_MILLIS }
    }
}
