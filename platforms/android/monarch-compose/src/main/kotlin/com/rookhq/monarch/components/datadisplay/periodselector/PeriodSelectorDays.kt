package com.rookhq.monarch.components.datadisplay.periodselector

import java.util.Calendar
import java.util.TimeZone

/** Calendar arithmetic on days held as UTC midnight in epoch milliseconds. */
internal object PeriodSelectorDays {
    const val DAY_MILLIS: Long = 86_400_000L
    private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

    /** The calendar day of [millis] in [zone], as UTC midnight. */
    fun dayOf(millis: Long, zone: TimeZone): Long {
        val local = Calendar.getInstance(zone).apply { timeInMillis = millis }
        return utc {
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }
    }

    /** [millis] moved back to UTC midnight of its day. */
    fun startOfDay(millis: Long): Long = Math.floorDiv(millis, DAY_MILLIS) * DAY_MILLIS

    fun addDays(day: Long, days: Int): Long = day + days * DAY_MILLIS

    fun addMonths(day: Long, months: Int): Long = utc(day) { add(Calendar.MONTH, months) }

    fun startOfMonth(day: Long): Long = utc(day) { set(Calendar.DAY_OF_MONTH, 1) }

    fun endOfMonth(day: Long): Long = addDays(addMonths(startOfMonth(day), 1), -1)

    fun startOfYear(day: Long): Long = utc(day) { set(Calendar.DAY_OF_YEAR, 1) }

    fun startOfQuarter(day: Long): Long {
        val month = Calendar.getInstance(Utc).apply { timeInMillis = startOfDay(day) }.get(Calendar.MONTH)
        return addMonths(startOfMonth(day), -(month % QUARTER_MONTHS))
    }

    /** The first day of the week that holds [day]; [firstDayOfWeek] is a `Calendar` day constant. */
    fun startOfWeek(day: Long, firstDayOfWeek: Int): Long {
        val weekday = Calendar.getInstance(Utc).apply { timeInMillis = day }.get(Calendar.DAY_OF_WEEK)
        val back = (weekday - firstDayOfWeek + WEEK_DAYS) % WEEK_DAYS
        return addDays(day, -back)
    }

    /** The number of days from [start] to [end], both included. */
    fun length(start: Long, end: Long): Int = ((startOfDay(end) - startOfDay(start)) / DAY_MILLIS).toInt() + 1

    private const val QUARTER_MONTHS = 3
    private const val WEEK_DAYS = 7

    private inline fun utc(block: Calendar.() -> Unit): Long =
        Calendar.getInstance(Utc).apply {
            clear()
            block()
        }.timeInMillis

    private inline fun utc(day: Long, block: Calendar.() -> Unit): Long =
        Calendar.getInstance(Utc).apply {
            timeInMillis = startOfDay(day)
            block()
        }.timeInMillis
}
