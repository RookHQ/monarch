package com.rookhq.monarch.components.workflow.gantttimeline

import androidx.compose.runtime.Immutable

/**
 * The dates of an item of a `MonarchGanttTimeline`, as day keys: the epoch millisecond of the day's midnight in UTC,
 * as in the Material 3 `DatePickerState`. With only one of the two the item is a one-day marker.
 *
 * @property start the first day, or `null`.
 * @property end the last day, inclusive, or `null`.
 */
@Immutable
public class MonarchGanttTimelineDates(public val start: Long?, public val end: Long?) {
    /**
     * These dates moved by [days] whole days (negative to go back), with the rules the timeline uses for a drag, a
     * key press or a TalkBack action. [MonarchGanttTimelineShift.Start] and [MonarchGanttTimelineShift.End] never
     * let the first day pass the last.
     *
     * @param shift which dates move.
     * @param days how many days, negative for earlier.
     */
    public fun shifted(shift: MonarchGanttTimelineShift, days: Int): MonarchGanttTimelineDates {
        val first = start?.let(GanttTimelineDays::key)
        val last = end?.let(GanttTimelineDays::key)
        if (days == 0) return MonarchGanttTimelineDates(first, last)
        val by = days * GanttTimelineDays.DAY_MILLIS
        return when (shift) {
            MonarchGanttTimelineShift.Move -> MonarchGanttTimelineDates(first?.plus(by), last?.plus(by))
            MonarchGanttTimelineShift.Start ->
                MonarchGanttTimelineDates((first ?: last)?.plus(by)?.let { minOf(it, last ?: it) }, last)
            MonarchGanttTimelineShift.End ->
                MonarchGanttTimelineDates(first, (last ?: first)?.plus(by)?.let { maxOf(it, first ?: it) })
        }
    }

    override fun equals(other: Any?): Boolean =
        other is MonarchGanttTimelineDates && other.start == start && other.end == end

    override fun hashCode(): Int = 31 * start.hashCode() + end.hashCode()

    override fun toString(): String = "MonarchGanttTimelineDates(start=$start, end=$end)"
}
