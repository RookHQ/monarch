package dev.monarchui.components.scheduling.timegrid

import dev.monarchui.components.scheduling.agendalist.AgendaListDays
import dev.monarchui.components.scheduling.agendalist.MonarchAgendaEvent
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** A wall-clock reading of an instant in a zone: the day key of its date and the minutes from midnight. */
internal data class TimeGridWall(val day: Long, val minutes: Int)

/** A `[top, bottom)` interval of wall-clock minutes inside one day. */
internal data class TimeGridSpan(val top: Int, val bottom: Int)

/**
 * A timed event placed in one day column.
 *
 * @property top the first visible minute of the event on this day.
 * @property bottom the last visible minute (exclusive), at least [top] plus the shortest drawn length.
 * @property col the zero-based column inside its overlap cluster.
 * @property cols the number of columns in the cluster.
 */
internal data class TimeGridPlaced(
    val event: MonarchAgendaEvent,
    val top: Int,
    val bottom: Int,
    val col: Int,
    val cols: Int,
)

/** An unavailable block inside one day column. */
internal data class TimeGridBlock(val block: MonarchTimeGridUnavailable, val top: Int, val bottom: Int)

/** Everything one day column shows. */
internal data class TimeGridColumn(
    val day: Long,
    val timed: List<TimeGridPlaced>,
    val allDay: List<MonarchAgendaEvent>,
    val blocks: List<TimeGridBlock>,
    val shade: List<TimeGridSpan>,
) {
    /** The most events side by side in this column. */
    val maxCols: Int get() = timed.maxOfOrNull { it.cols } ?: 0
}

/** The visible hours of a grid, in wall-clock minutes. */
internal data class TimeGridRange(val start: Int, val end: Int) {
    val hours: List<Int> get() = (start / MINUTES_PER_HOUR until end / MINUTES_PER_HOUR).toList()

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}

/**
 * The calendar arithmetic and layout rules of MonarchTimeGrid. Pure, so it
 * is tested without a view. Days are day keys, as in `MonarchAgendaList`;
 * events are placed by their wall-clock time in the grid's zone, so a
 * daylight-saving day still has 24 rows.
 */
internal object TimeGridModel {
    const val DAY_MILLIS: Long = AgendaListDays.DAY_MILLIS
    const val MINUTE_MILLIS: Long = 60_000L
    const val MINUTES_PER_HOUR: Int = 60
    const val MINUTES_PER_DAY: Int = 1_440
    private const val LAST_HOUR = 23
    private const val HOURS_PER_DAY = 24
    private const val DAYS_PER_WEEK = 7

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    /** The day key of a day given as any instant of its UTC date. */
    fun dayKey(day: Long): Long = Math.floorDiv(day, DAY_MILLIS) * DAY_MILLIS

    /** The visible hours from [startHour] to [endHour], clamped so at least one hour shows. */
    fun range(startHour: Int, endHour: Int): TimeGridRange {
        val start = startHour.coerceIn(0, LAST_HOUR) * MINUTES_PER_HOUR
        val end = maxOf(start + MINUTES_PER_HOUR, endHour.coerceAtMost(HOURS_PER_DAY) * MINUTES_PER_HOUR)
        return TimeGridRange(start, end)
    }

    /** The day key and wall-clock minutes of [millis] in [zone]. */
    fun wall(millis: Long, zone: TimeZone): TimeGridWall {
        val local = Calendar.getInstance(zone, Locale.ROOT).apply { timeInMillis = millis }
        val day = Calendar.getInstance(utc, Locale.ROOT).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
        return TimeGridWall(day, local.get(Calendar.HOUR_OF_DAY) * MINUTES_PER_HOUR + local.get(Calendar.MINUTE))
    }

    /**
     * The instant of [minutes] after midnight on [day] in [zone]. A time a
     * daylight-saving change skips moves forward to the next valid time.
     */
    fun instant(day: Long, minutes: Int, zone: TimeZone): Long {
        val date = Calendar.getInstance(utc, Locale.ROOT).apply { timeInMillis = day }
        return Calendar.getInstance(zone, Locale.ROOT).apply {
            clear()
            set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, minutes / MINUTES_PER_HOUR)
            set(Calendar.MINUTE, minutes % MINUTES_PER_HOUR)
        }.timeInMillis
    }

    /** The ISO weekday of [day]: 1 is Monday, 7 is Sunday. */
    fun isoWeekday(day: Long): Int {
        val weekday = Calendar.getInstance(utc, Locale.ROOT).apply { timeInMillis = day }.get(Calendar.DAY_OF_WEEK)
        return (weekday + DAYS_PER_WEEK - 2) % DAYS_PER_WEEK + 1
    }

    /**
     * The wall-clock minutes a timed interval covers on [day], or `null` when
     * it does not touch the day. An interval that ends exactly at midnight
     * does not touch the day it ends on; a multi-day interval is clipped. An
     * interval shorter than [minLength] (or without an end) is drawn [minLength] long.
     */
    fun span(start: Long, end: Long?, day: Long, zone: TimeZone, minLength: Int): TimeGridSpan? {
        val s = wall(start, zone)
        val e = end?.takeIf { it >= start }?.let { wall(it, zone) } ?: s
        if (e.day < day || s.day > day) return null
        if (e.day == day && e.minutes == 0 && s.day != day) return null
        val top = if (s.day < day) 0 else s.minutes
        var bottom = if (e.day > day) MINUTES_PER_DAY else e.minutes
        if (bottom - top < minLength) bottom = minOf(MINUTES_PER_DAY, top + minLength)
        return TimeGridSpan(top, bottom)
    }

}
