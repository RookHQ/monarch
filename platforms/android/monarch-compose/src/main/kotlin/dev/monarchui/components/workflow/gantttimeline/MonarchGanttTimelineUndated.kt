package dev.monarchui.components.workflow.gantttimeline

/** Where a `MonarchGanttTimeline` puts top-level items that have no dates. */
public enum class MonarchGanttTimelineUndated {
    /** In an Unscheduled list under the chart. */
    List,

    /** As chart rows that say "No dates". */
    Row,
}
