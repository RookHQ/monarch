package dev.monarchui.components.scheduling.teamcalendar

import android.icu.text.DateFormat
import android.icu.text.DateFormatSymbols
import android.icu.text.NumberFormat
import dev.monarchui.internal.scheduling.MonarchCalendarDates
import java.util.Date
import java.util.Locale

/** Names of days and months for MonarchTeamCalendar, in [locale], read in UTC. */
internal class TeamCalendarFormats(locale: Locale) {
    private val utc = android.icu.util.TimeZone.getTimeZone("UTC")
    private fun format(skeleton: String, locale: Locale): DateFormat =
        DateFormat.getInstanceForSkeleton(skeleton, locale).apply { timeZone = utc }

    private val monthFormat = format("yMMMM", locale)
    private val dayFormat = format("MMMMEEEEd", locale)
    private val shortFormat = format("MMMd", locale)
    private val shortYearFormat = format("yMMMd", locale)
    private val numbers = NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }
    private val narrowWeekdays: Array<String> =
        DateFormatSymbols(locale).getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.NARROW)

    /** "November 2026". */
    fun month(day: Long): String = monthFormat.format(Date(day))

    /** "Nov 2 – Nov 30, 2026", the name of a custom range. */
    fun range(first: Long, last: Long): String =
        "${shortFormat.format(Date(first))} – ${shortYearFormat.format(Date(last))}"

    /** "Monday, November 2". */
    fun day(day: Long): String = dayFormat.format(Date(day))

    /** "2". */
    fun number(day: Long): String = numbers.format(MonarchCalendarDates.dayOfMonth(day).toLong())

    /** "M". */
    fun narrowWeekday(day: Long): String = narrowWeekdays[MonarchCalendarDates.dayOfWeek(day)]
}
