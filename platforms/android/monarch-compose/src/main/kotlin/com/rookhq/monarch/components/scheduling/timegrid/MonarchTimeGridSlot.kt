package com.rookhq.monarch.components.scheduling.timegrid

import androidx.compose.runtime.Immutable

/**
 * An empty part of a `MonarchTimeGrid` column that the person chose, to create an event there.
 *
 * @property day the day key of the column: the epoch millisecond of the UTC midnight of its date.
 * @property minutes the wall-clock minutes from midnight in the grid's time zone, snapped down to the grid's
 * snap interval. A tap from TalkBack or a keyboard gives the start of the hour.
 * @property start the instant the slot starts, in epoch milliseconds. On a daylight-saving day a time that does
 * not exist moves forward to the next valid time.
 */
@Immutable
public class MonarchTimeGridSlot(
    public val day: Long,
    public val minutes: Int,
    public val start: Long,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchTimeGridSlot && other.day == day && other.minutes == minutes && other.start == start

    override fun hashCode(): Int = (day.hashCode() * HASH + minutes) * HASH + start.hashCode()

    override fun toString(): String = "MonarchTimeGridSlot(day=$day, minutes=$minutes, start=$start)"

    private companion object {
        const val HASH = 31
    }
}
