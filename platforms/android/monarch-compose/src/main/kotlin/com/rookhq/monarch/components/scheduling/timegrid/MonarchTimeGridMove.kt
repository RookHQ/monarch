package com.rookhq.monarch.components.scheduling.timegrid

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.scheduling.agendalist.MonarchAgendaEvent

/**
 * A finished move of an event in a `MonarchTimeGrid`, by long press and drag or by a TalkBack action. The grid
 * does not change the event: the app applies or confirms the move.
 *
 * @property event the event that was moved, as it was passed to the grid.
 * @property start the new start, in epoch milliseconds.
 * @property end the new end, in epoch milliseconds. The event keeps its duration.
 */
@Immutable
public class MonarchTimeGridMove(
    public val event: MonarchAgendaEvent,
    public val start: Long,
    public val end: Long,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchTimeGridMove && other.event == event && other.start == start && other.end == end

    override fun hashCode(): Int = (event.hashCode() * HASH + start.hashCode()) * HASH + end.hashCode()

    override fun toString(): String = "MonarchTimeGridMove(event=${event.id}, start=$start, end=$end)"

    private companion object {
        const val HASH = 31
    }
}
