package dev.monarchui.components.scheduling.timegrid

import dev.monarchui.components.scheduling.agendalist.AgendaListDays
import dev.monarchui.components.scheduling.agendalist.MonarchAgendaEvent
import java.util.TimeZone

/**
 * The layout and move rules of MonarchTimeGrid on top of [TimeGridModel]:
 * packing overlaps, shading, the columns, snapping, clamping and moves.
 * Pure, so it is tested without a view.
 */
internal object TimeGridLayout {
    private const val DEFAULT_SCROLL = 8 * TimeGridModel.MINUTES_PER_HOUR

    /**
     * The side-by-side layout of overlapping intervals: clusters of items that
     * overlap one another, each item in the first free column, every item in a
     * cluster sharing the cluster's column count. The web's `packColumns`.
     */
    fun pack(items: List<Pair<MonarchAgendaEvent, TimeGridSpan>>): List<TimeGridPlaced> {
        val sorted = items.sortedWith(compareBy<Pair<MonarchAgendaEvent, TimeGridSpan>> { it.second.top }
            .thenByDescending { it.second.bottom })
        val out = mutableListOf<TimeGridPlaced>()
        val cluster = mutableListOf<TimeGridPlaced>()
        val colEnds = mutableListOf<Int>()
        var clusterEnd = -1
        fun flush() {
            cluster.forEach { out += it.copy(cols = colEnds.size) }
            cluster.clear()
            colEnds.clear()
        }
        sorted.forEach { (event, span) ->
            if (cluster.isNotEmpty() && span.top >= clusterEnd) flush()
            var col = colEnds.indexOfFirst { it <= span.top }
            if (col == -1) {
                col = colEnds.size
                colEnds += span.bottom
            } else {
                colEnds[col] = span.bottom
            }
            cluster += TimeGridPlaced(event, span.top, span.bottom, col, 1)
            clusterEnd = maxOf(clusterEnd, span.bottom)
        }
        if (cluster.isNotEmpty()) flush()
        return out.sortedWith(compareBy<TimeGridPlaced> { it.top }.thenBy { it.col })
    }

    /** The parts of [range] outside the working hours of [weekday]; nothing without [workingHours]. */
    fun shade(
        workingHours: List<MonarchTimeGridWorkingHours>?,
        weekday: Int,
        range: TimeGridRange,
    ): List<TimeGridSpan> {
        if (workingHours == null) return emptyList()
        val open = workingHours.filter { it.weekday == weekday }.sortedBy { it.start }
        val shade = mutableListOf<TimeGridSpan>()
        var cursor = range.start
        open.forEach { hours ->
            if (hours.start > cursor) shade += TimeGridSpan(cursor, minOf(hours.start, range.end))
            cursor = maxOf(cursor, hours.end)
        }
        if (cursor < range.end) shade += TimeGridSpan(cursor, range.end)
        return shade.filter { it.bottom > it.top }
    }

    /**
     * The columns of the grid. A timed event is clipped to [range] and drawn
     * at least [minMinutes] long (moved up when that would pass the end of the
     * range), and overlaps are packed by the drawn length, so drawn events
     * never cover one another.
     */
    @Suppress("LongParameterList")
    fun columns(
        days: List<Long>,
        events: List<MonarchAgendaEvent>,
        unavailable: List<MonarchTimeGridUnavailable>,
        workingHours: List<MonarchTimeGridWorkingHours>?,
        range: TimeGridRange,
        zone: TimeZone,
        minMinutes: Int,
    ): List<TimeGridColumn> = days.map { day ->
        val timed = events.filter { !it.allDay }.mapNotNull { event ->
            val span = TimeGridModel.span(event.start, event.end, day, zone, 1) ?: return@mapNotNull null
            val top = maxOf(span.top, range.start)
            val bottom = minOf(span.bottom, range.end)
            if (bottom <= top) null else event to drawn(top, bottom, range, minMinutes)
        }
        val allDay = events.filter { event ->
            event.allDay && AgendaListDays.span(event, zone).let { day in it.first..it.last }
        }
        val blocks = unavailable.mapNotNull { block ->
            val span = TimeGridModel.span(block.start, block.end, day, zone, 1) ?: return@mapNotNull null
            val top = maxOf(span.top, range.start)
            val bottom = minOf(span.bottom, range.end)
            if (bottom > top) TimeGridBlock(block, top, bottom) else null
        }
        val shade = shade(workingHours, TimeGridModel.isoWeekday(day), range)
        TimeGridColumn(day, pack(timed), allDay, blocks, shade)
    }

    /** [top] to [bottom], at least [minMinutes] long and inside [range]. */
    private fun drawn(top: Int, bottom: Int, range: TimeGridRange, minMinutes: Int): TimeGridSpan {
        val length = minOf(maxOf(bottom - top, minMinutes), range.end - range.start)
        val start = minOf(top, range.end - length)
        return TimeGridSpan(start, start + length)
    }

    /** Minutes from the top of the range, snapped down to [snap] and kept inside the range. */
    fun snapDown(minutes: Float, range: TimeGridRange, snap: Int): Int {
        val raw = (range.start + minutes).toInt()
        val snapped = Math.floorDiv(raw, snap) * snap
        return snapped.coerceIn(range.start, maxOf(range.start, range.end - snap))
    }

    /**
     * Where a moved event may start: inside the visible hours, so the whole
     * event stays on screen; an event longer than the range starts at its top.
     */
    fun clampStart(start: Int, duration: Int, range: TimeGridRange, snap: Int): Int {
        val latest = maxOf(range.start, range.end - maxOf(duration, snap))
        return start.coerceIn(range.start, latest)
    }

    /** The duration of [event] in whole minutes, at least [snap]; an event without an end lasts [snap]. */
    fun durationMinutes(event: MonarchAgendaEvent, snap: Int): Int {
        val end = event.end?.takeIf { it >= event.start } ?: return snap
        return maxOf(snap, ((end - event.start) / TimeGridModel.MINUTE_MILLIS).toInt())
    }

    /** The move of [event] to [minutes] on [day], keeping its duration; `null` when the start would not change. */
    fun move(event: MonarchAgendaEvent, day: Long, minutes: Int, zone: TimeZone, snap: Int): MonarchTimeGridMove? {
        val start = TimeGridModel.instant(day, minutes, zone)
        if (start == event.start) return null
        val end = event.end?.takeIf { it >= event.start } ?: (event.start + snap * TimeGridModel.MINUTE_MILLIS)
        val length = end - event.start
        return MonarchTimeGridMove(event, start, start + length)
    }

    /** The minutes to scroll to: [scrollToMinutes], else an hour before the first event, else 08:00. */
    fun scrollTarget(scrollToMinutes: Int?, columns: List<TimeGridColumn>): Int {
        if (scrollToMinutes != null) return scrollToMinutes
        val first = columns.flatMap { column -> column.timed.map { it.top } }.minOrNull()
        return if (first != null) maxOf(0, first - TimeGridModel.MINUTES_PER_HOUR) else DEFAULT_SCROLL
    }
}
