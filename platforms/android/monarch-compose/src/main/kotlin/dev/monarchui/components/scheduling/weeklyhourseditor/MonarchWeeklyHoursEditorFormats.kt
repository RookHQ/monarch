package dev.monarchui.components.scheduling.weeklyhourseditor

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import java.util.Date
import java.util.Locale

/** The weekday names and times of a MonarchWeeklyHoursEditor in one locale, formatted in UTC. */
internal class MonarchWeeklyHoursEditorFormats(
    locale: Locale,
    private val is24Hour: Boolean,
    private val midnight: String,
) {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun format(skeleton: String, locale: Locale) =
        DateFormat.getInstanceForSkeleton(skeleton, locale).apply { timeZone = utc }

    private val weekday = format(WEEKDAY, locale)
    private val time = format(if (is24Hour) TIME_24 else TIME_12, locale)
    private val cache = HashMap<Int, String>()

    /** "Monday" for ISO weekday 1. */
    fun day(isoDay: Int): String = weekday.format(Date(MONDAY_NOON + (isoDay - 1) * DAY_MILLIS))

    /** "9:00 AM" or "09:00" for minutes from midnight; the end of the day is "Midnight" or "24:00". */
    fun time(minutes: Int): String = cache.getOrPut(minutes) {
        when {
            minutes >= MonarchWeeklyHoursEditorTokens.MINUTES_PER_DAY -> if (is24Hour) END_OF_DAY_24 else midnight
            else -> time.format(Date(minutes.coerceAtLeast(0) * MINUTE_MILLIS))
        }
    }

    private companion object {
        const val WEEKDAY = "EEEE"
        const val TIME_12 = "hmm"
        const val TIME_24 = "HHmm"
        const val END_OF_DAY_24 = "24:00"
        const val MINUTE_MILLIS = 60_000L
        const val DAY_MILLIS = 86_400_000L

        /** 2024-01-01T12:00Z, a Monday. */
        const val MONDAY_NOON = 1_704_110_400_000L
    }
}
