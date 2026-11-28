package dev.monarchui.components.scheduling.timegrid

import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The day and time formats of a time grid in one locale and zone. Day keys
 * are formatted in UTC so they never shift; times of instants in the grid's
 * zone; hour labels from wall-clock minutes, so a daylight-saving day reads
 * the same as any other.
 */
internal class TimeGridFormats(locale: Locale, zone: TimeZone) {
    private val utc = android.icu.util.TimeZone.getTimeZone("UTC")
    private val weekdayFormat = android.icu.text.DateFormat.getInstanceForSkeleton(WEEKDAY, locale).apply {
        timeZone = utc
    }
    private val dayNumberFormat = android.icu.text.DateFormat.getInstanceForSkeleton(DAY_NUMBER, locale).apply {
        timeZone = utc
    }
    private val longDayFormat = android.icu.text.DateFormat.getInstanceForSkeleton(LONG_DAY, locale).apply {
        timeZone = utc
    }
    private val timeFormat = android.icu.text.DateFormat.getInstanceForSkeleton(TIME, locale).apply {
        timeZone = android.icu.util.TimeZone.getTimeZone(zone.id)
    }
    private val wallFormat = android.icu.text.DateFormat.getInstanceForSkeleton(TIME, locale).apply {
        timeZone = utc
    }

    /** "Wed" for a day key. */
    fun weekday(day: Long): String = weekdayFormat.format(Date(day))

    /** "4" for a day key. */
    fun dayNumber(day: Long): String = dayNumberFormat.format(Date(day))

    /** "Wednesday, November 4" for a day key. */
    fun longDay(day: Long): String = longDayFormat.format(Date(day))

    /** "9:30 AM" or "09:30" for an instant, as the locale writes a time. */
    fun time(millis: Long): String = timeFormat.format(Date(millis))

    /** "9:00 AM" or "09:00" for wall-clock minutes from midnight. */
    fun minutes(minutes: Int): String = wallFormat.format(Date(minutes * TimeGridModel.MINUTE_MILLIS))

    private companion object {
        const val WEEKDAY = "EEE"
        const val DAY_NUMBER = "d"
        const val LONG_DAY = "EEEEMMMMd"
        const val TIME = "jmm"
    }
}
