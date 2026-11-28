package dev.monarchui.components.scheduling.calendarmonthview

import dev.monarchui.components.scheduling.agendalist.MonarchAgendaEvent
import dev.monarchui.internal.scheduling.MonarchMonthGrid
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** The first and last day (inclusive) an event touches, as day keys. */
internal data class CalendarMonthViewSpan(val first: Long, val last: Long)

/** How an event's time reads on one day: the visible text and what TalkBack reads. */
internal data class CalendarMonthViewTime(val visible: String, val spoken: String)

/**
 * How events of MonarchCalendarMonthView land on days: instants placed on
 * the day keys they touch in a time zone, in order, and how their times read.
 */
internal object CalendarMonthViewEvents {
    private const val DAY_MILLIS = MonarchMonthGrid.DAY_MILLIS

    /** Whether [millis] is exactly midnight in [zone]. */
    private fun isMidnight(millis: Long, zone: TimeZone): Boolean {
        val local = Calendar.getInstance(zone, Locale.ROOT).apply { timeInMillis = millis }
        return local.get(Calendar.HOUR_OF_DAY) == 0 && local.get(Calendar.MINUTE) == 0 &&
            local.get(Calendar.SECOND) == 0 && local.get(Calendar.MILLISECOND) == 0
    }

    /** The end of [event], or `null` when it has none or it is before the start. */
    fun endOf(event: MonarchAgendaEvent): Long? = event.end?.takeIf { it >= event.start }

    /**
     * Every day [event] touches in [zone]. A timed event that ends exactly at
     * midnight does not touch the day it ends on; an all-day event's end is
     * any time on its last day.
     */
    fun span(event: MonarchAgendaEvent, zone: TimeZone): CalendarMonthViewSpan {
        val first = MonarchMonthGrid.dayOf(event.start, zone)
        val end = endOf(event) ?: return CalendarMonthViewSpan(first, first)
        var last = MonarchMonthGrid.dayOf(end, zone)
        if (!event.allDay && last > first && isMidnight(end, zone)) last -= DAY_MILLIS
        return CalendarMonthViewSpan(first, maxOf(first, last))
    }

    /**
     * The events of each day in [days], all-day first, then by start, then by
     * title. A longer event is on every day it touches.
     */
    fun eventsByDay(
        events: List<MonarchAgendaEvent>,
        days: List<Long>,
        zone: TimeZone,
    ): Map<Long, List<MonarchAgendaEvent>> {
        if (days.isEmpty()) return emptyMap()
        val first = days.first()
        val last = days.last()
        val byDay = days.associateWith { mutableListOf<MonarchAgendaEvent>() }
        events.forEach { event ->
            val span = span(event, zone)
            var day = maxOf(span.first, first)
            while (day <= span.last && day <= last) {
                byDay[day]?.add(event)
                day += DAY_MILLIS
            }
        }
        val order = compareByDescending<MonarchAgendaEvent> { it.allDay }.thenBy { it.start }.thenBy { it.title }
        return byDay.mapValues { (_, list) -> list.sortedWith(order) }
    }

    /**
     * How many events a cell lists before the overflow line: all of them when
     * they fit in [maxEvents], otherwise one fewer, so the "+N more" line
     * takes the last place.
     */
    fun shownCount(count: Int, maxEvents: Int): Int =
        if (count > maxEvents) maxOf(0, maxEvents - 1) else count

    /**
     * How [event] reads on [day]: "All day" for an all-day event or a day a
     * timed event covers whole, "09:30 – 10:15" on its first day, and
     * "– 02:00" on a later day it ends on.
     *
     * @param time formats an instant as a time of day.
     */
    @Suppress("LongParameterList")
    fun time(
        event: MonarchAgendaEvent,
        day: Long,
        zone: TimeZone,
        allDay: String,
        range: (String, String) -> String,
        until: (String) -> String,
        time: (Long) -> String,
    ): CalendarMonthViewTime {
        if (event.allDay) return CalendarMonthViewTime(allDay, allDay)
        val start = time(event.start)
        val end = endOf(event)?.let(time) ?: return CalendarMonthViewTime(start, start)
        val span = span(event, zone)
        return when {
            span.first < day && day < span.last -> CalendarMonthViewTime(allDay, allDay)
            span.first < day -> CalendarMonthViewTime("– $end", until(end))
            else -> CalendarMonthViewTime("$start – $end", range(start, end))
        }
    }

    /** The short time a cell shows before a title: the start on the first day of a timed event, else none. */
    fun cellTime(event: MonarchAgendaEvent, day: Long, zone: TimeZone, time: (Long) -> String): String? =
        if (event.allDay || span(event, zone).first != day) null else time(event.start)
}
