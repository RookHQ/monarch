package dev.monarchui.components.workflow.gantttimeline

import java.util.Calendar
import java.util.TimeZone

/** Calendar-day maths of MonarchGanttTimeline. Days are epoch day numbers (days since 1970-01-01, UTC). */
internal object GanttTimelineDays {
    const val DAY_MILLIS: Long = 86_400_000L
    private const val THURSDAY_OFFSET = 4
    private const val WEEK = 7

    /** The day key (midnight UTC) of the date of [millis] in UTC. */
    fun key(millis: Long): Long = Math.floorDiv(millis, DAY_MILLIS) * DAY_MILLIS

    /** The epoch day number of [millis], read in UTC. */
    fun day(millis: Long): Long = Math.floorDiv(millis, DAY_MILLIS)

    /** The day key of a day number. */
    fun millis(day: Long): Long = day * DAY_MILLIS

    /** 0 = Sunday … 6 = Saturday. */
    fun weekday(day: Long): Int = Math.floorMod(day + THURSDAY_OFFSET, WEEK.toLong()).toInt()

    /** The Monday on or before [day]. */
    fun mondayOf(day: Long): Long = day - (weekday(day) + WEEK - 1) % WEEK

    /** The day number of the date that [millis] falls on in [zone]. */
    fun localDay(millis: Long, zone: TimeZone): Long {
        val local = Calendar.getInstance(zone).apply { timeInMillis = millis }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }
        return day(utc.timeInMillis)
    }

    /** The day of the month (1–31) and the month (0–11) of [day]. */
    fun dayAndMonth(day: Long): Pair<Int, Int> {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis(day) }
        return utc.get(Calendar.DAY_OF_MONTH) to utc.get(Calendar.MONTH)
    }
}
