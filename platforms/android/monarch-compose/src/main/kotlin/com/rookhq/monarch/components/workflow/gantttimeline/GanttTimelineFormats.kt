package com.rookhq.monarch.components.workflow.gantttimeline

import android.icu.text.DateFormat
import android.icu.text.NumberFormat
import android.icu.util.TimeZone
import java.util.Date
import java.util.Locale

/** The date and number formats of MonarchGanttTimeline in one locale. Days are formatted in UTC. */
internal class GanttTimelineFormats(locale: Locale) {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun format(skeleton: String, locale: Locale) =
        DateFormat.getInstanceForSkeleton(skeleton, locale).apply { timeZone = utc }

    private val date = format(DATE, locale)
    private val month = format(MONTH, locale)
    private val monthYear = format(MONTH_YEAR, locale)
    private val weekdayDay = format(WEEKDAY_DAY, locale)
    private val dayNumber = format(DAY_NUMBER, locale)
    private val percent = NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 0 }

    /** "Jan 12, 2026" for a day number. */
    fun date(day: Long): String = date.format(Date(GanttTimelineDays.millis(day)))

    /** "Jan" or "Jan 2026". */
    fun month(day: Long, withYear: Boolean): String =
        (if (withYear) monthYear else month).format(Date(GanttTimelineDays.millis(day)))

    /** "M 12": the narrow weekday and the day of the month. */
    fun weekdayDay(day: Long): String = weekdayDay.format(Date(GanttTimelineDays.millis(day)))

    /** "12". */
    fun dayNumber(day: Long): String = dayNumber.format(Date(GanttTimelineDays.millis(day)))

    /** "60%" for a fraction from 0 to 1. */
    fun percent(fraction: Float): String = percent.format(fraction.toDouble())

    private companion object {
        const val DATE = "yMMMd"
        const val MONTH = "MMM"
        const val MONTH_YEAR = "yMMM"
        const val WEEKDAY_DAY = "EEEEEd"
        const val DAY_NUMBER = "d"
    }
}
