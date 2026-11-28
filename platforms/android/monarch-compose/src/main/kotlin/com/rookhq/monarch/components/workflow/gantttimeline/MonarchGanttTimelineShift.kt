package com.rookhq.monarch.components.workflow.gantttimeline

/** Which dates of an item a move by whole days changes. See [MonarchGanttTimelineDates.shifted]. */
public enum class MonarchGanttTimelineShift {
    /** Both dates: the item moves and keeps its length. */
    Move,

    /** Only the first day: the item starts earlier or later. */
    Start,

    /** Only the last day: the item ends earlier or later. */
    End,
}
