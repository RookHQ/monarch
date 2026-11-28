package dev.monarchui.components.scheduling.timeoffpanel

import android.icu.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** The date and time texts of MonarchTimeOffPanel in one locale and zone. */
internal class TimeOffPanelFormats(locale: Locale, is24Hour: Boolean, zone: TimeZone) {
    private val utc = android.icu.util.TimeZone.GMT_ZONE
    private val inZone = android.icu.util.TimeZone.getTimeZone(zone.id)
    private val day = DateFormat.getInstanceForSkeleton(DATE, locale).apply { timeZone = utc }
    private val zonedDay = DateFormat.getInstanceForSkeleton(DATE, locale).apply { timeZone = inZone }
    private val time = DateFormat.getInstanceForSkeleton(if (is24Hour) TIME_24 else TIME_12, locale).apply {
        timeZone = utc
    }
    private val zonedTime = DateFormat.getInstanceForSkeleton(if (is24Hour) TIME_24 else TIME_12, locale).apply {
        timeZone = inZone
    }

    /** "Mon, Nov 9, 2026" for a day as its UTC midnight. */
    fun day(day: Long): String = this.day.format(Date(day))

    /** "9:00 AM" for minutes after midnight. */
    fun minutes(minutes: Int): String = time.format(Date(minutes * MonarchTimeOffPanelTokens.MINUTE_MILLIS))

    /**
     * When [entry] is: "Mon, Nov 9, 2026 – Fri, Nov 13, 2026 (all day)" or "Thu, Nov 5, 2026, 9:00 AM – 1:00 PM".
     */
    fun period(entry: MonarchTimeOffEntry, allDayText: String): String {
        if (entry.allDay) {
            val first = day(entry.start)
            val last = day(entry.end)
            return if (first == last) "$first ($allDayText)" else "$first – $last ($allDayText)"
        }
        val startDay = zonedDay.format(Date(entry.start))
        val endDay = zonedDay.format(Date(entry.end))
        val from = zonedTime.format(Date(entry.start))
        val to = zonedTime.format(Date(entry.end))
        return if (startDay == endDay) "$startDay, $from – $to" else "$startDay, $from – $endDay, $to"
    }

    private companion object {
        const val DATE = "EEEMMMdyyyy"
        const val TIME_12 = "hmm"
        const val TIME_24 = "Hmm"
    }
}
