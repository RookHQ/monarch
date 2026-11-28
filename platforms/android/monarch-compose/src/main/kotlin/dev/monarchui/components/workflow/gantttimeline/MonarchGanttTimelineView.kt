package dev.monarchui.components.workflow.gantttimeline

/** What a `MonarchGanttTimeline` shows: the chart of bars, or the same items as a list. */
public enum class MonarchGanttTimelineView {
    /** Bars on a calendar, with dependency arrows, milestones and a today line. Scrolls sideways. */
    Chart,

    /**
     * A list row per item with its dates, duration, progress, dependencies and conflict in words, and a Change dates
     * button that opens the Material 3 date range picker. The phone and TalkBack alternative to the chart.
     */
    List,
}
