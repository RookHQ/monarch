package com.rookhq.monarch.components.scheduling.agendalist

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** The first and last day (inclusive) an event touches, as day keys. */
internal data class AgendaListSpan(val first: Long, val last: Long)

/** One day of the agenda: its key, its events in order and its notes. */
internal data class AgendaListDay(val day: Long, val events: List<MonarchAgendaEvent>, val notes: List<String>)

/** How an event's time reads on one day: the visible text and what TalkBack reads. */
internal data class AgendaListTime(val visible: String, val spoken: String)

/**
 * Calendar arithmetic of MonarchAgendaList. A day is a "day key": the epoch
 * millisecond of the UTC midnight of a date in the agenda's time zone, as in
 * the Material 3 `DatePickerState`. `java.time` needs API 26, so this uses
 * `java.util.Calendar`.
 */
internal object AgendaListDays {
    const val DAY_MILLIS: Long = 86_400_000L

    /** The longest range listed, about a year of days, so a multi-year event does not list thousands. */
    const val MAX_DAYS: Int = 400

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    /** The day key of the date that contains [millis] in [zone]. */
    fun dayOf(millis: Long, zone: TimeZone): Long {
        val local = Calendar.getInstance(zone, Locale.ROOT).apply { timeInMillis = millis }
        return Calendar.getInstance(utc, Locale.ROOT).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    /** Whether [millis] is exactly midnight in [zone]. */
    private fun isMidnight(millis: Long, zone: TimeZone): Boolean {
        val local = Calendar.getInstance(zone, Locale.ROOT).apply { timeInMillis = millis }
        return local.get(Calendar.HOUR_OF_DAY) == 0 && local.get(Calendar.MINUTE) == 0 &&
            local.get(Calendar.SECOND) == 0 && local.get(Calendar.MILLISECOND) == 0
    }

    /** The end of [event], or `null` when it has none or it is before the start. */
    fun endOf(event: MonarchAgendaEvent): Long? = event.end?.takeIf { it >= event.start }

    /**
     * Every day [event] touches. A timed event that ends exactly at midnight
     * does not touch the day it ends on; an all-day event's end is any time
     * on its last day.
     */
    fun span(event: MonarchAgendaEvent, zone: TimeZone): AgendaListSpan {
        val first = dayOf(event.start, zone)
        val end = endOf(event) ?: return AgendaListSpan(first, first)
        var last = dayOf(end, zone)
        if (!event.allDay && last > first && isMidnight(end, zone)) last -= DAY_MILLIS
        return AgendaListSpan(first, maxOf(first, last))
    }

    /**
     * The days listed: from [from] (or the first day with an event) for
     * [days] days (or through the last event), at most [MAX_DAYS].
     */
    fun range(events: List<MonarchAgendaEvent>, from: Long?, days: Int?, zone: TimeZone): List<Long> {
        val spans = events.map { span(it, zone) }
        val first = from?.let { dayOf(it, zone) } ?: spans.minOfOrNull { it.first } ?: return emptyList()
        val last = when {
            days != null -> first + (maxOf(1, days) - 1) * DAY_MILLIS
            else -> maxOf(first, spans.maxOfOrNull { it.last } ?: first)
        }
        val count = minOf(MAX_DAYS.toLong(), (last - first) / DAY_MILLIS + 1).toInt()
        return List(count) { first + it * DAY_MILLIS }
    }

    /**
     * The days to show, each with its events (all-day first, then by start,
     * then by title) and notes. Days without events or notes are left out
     * unless [showEmptyDays].
     */
    fun days(
        events: List<MonarchAgendaEvent>,
        range: List<Long>,
        zone: TimeZone,
        notes: Map<Long, List<String>>,
        showEmptyDays: Boolean,
    ): List<AgendaListDay> {
        if (range.isEmpty()) return emptyList()
        val byDay = range.associateWith { mutableListOf<MonarchAgendaEvent>() }
        val notesByDay = notes.entries.groupBy({ dayOf(it.key, zone) }, { it.value }).mapValues { it.value.flatten() }
        events.forEach { event ->
            val span = span(event, zone)
            var day = maxOf(span.first, range.first())
            while (day <= span.last && day <= range.last()) {
                byDay[day]?.add(event)
                day += DAY_MILLIS
            }
        }
        val order = compareByDescending<MonarchAgendaEvent> { it.allDay }.thenBy { it.start }.thenBy { it.title }
        return range.map { day ->
            AgendaListDay(day, byDay.getValue(day).sortedWith(order), notesByDay[day].orEmpty())
        }.filter { showEmptyDays || it.events.isNotEmpty() || it.notes.isNotEmpty() }
    }

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
    ): AgendaListTime {
        if (event.allDay) return AgendaListTime(allDay, allDay)
        val start = time(event.start)
        val end = endOf(event)?.let(time) ?: return AgendaListTime(start, start)
        val span = span(event, zone)
        return when {
            span.first < day && day < span.last -> AgendaListTime(allDay, allDay)
            span.first < day -> AgendaListTime("– $end", until(end))
            else -> AgendaListTime("$start – $end", range(start, end))
        }
    }
}
