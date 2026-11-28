package com.rookhq.monarch.components.scheduling.shiftroster

import android.icu.text.DateFormat
import android.icu.text.NumberFormat
import android.icu.util.TimeZone
import java.util.Date
import java.util.Locale

/** The day, time and number formats of MonarchShiftRoster in one locale. Days are formatted in UTC. */
internal class ShiftRosterFormats(locale: Locale, is24Hour: Boolean) {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun format(skeleton: String, locale: Locale) =
        DateFormat.getInstanceForSkeleton(skeleton, locale).apply { timeZone = utc }

    private val full = format(FULL, locale)
    private val weekday = format(WEEKDAY, locale)
    private val dayNumber = format(DAY_NUMBER, locale)
    private val date = format(DATE, locale)
    private val time = format(if (is24Hour) TIME_24 else TIME_12, locale)
    private val number = NumberFormat.getInstance(locale).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }

    /** "Wednesday, November 4". */
    fun fullDay(day: Long): String = full.format(Date(day))

    /** "Wed". */
    fun weekday(day: Long): String = weekday.format(Date(day))

    /** "4". */
    fun dayNumber(day: Long): String = dayNumber.format(Date(day))

    /** "Nov 4". */
    fun date(day: Long): String = date.format(Date(day))

    /** "6:00 AM" or "06:00" for a time in minutes from midnight. */
    fun time(minutes: Int): String =
        time.format(Date(minutes.mod(ShiftRosterRules.MINUTES_PER_DAY) * MINUTE_MILLIS))

    /** "37.5" for a number of hours, rounded to one decimal. */
    fun hours(hours: Double): String = number.format(hours)

    private companion object {
        const val FULL = "EEEEMMMMd"
        const val WEEKDAY = "EEE"
        const val DAY_NUMBER = "d"
        const val DATE = "MMMd"
        const val TIME_12 = "hmm"
        const val TIME_24 = "Hmm"
        const val MINUTE_MILLIS = 60_000L
    }
}
