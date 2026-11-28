package com.rookhq.monarch.components.workflow.gantttimeline

/** How much time the visible window of a `MonarchGanttTimeline` covers, and how far Earlier and Later move it. */
public enum class MonarchGanttTimelineZoom {
    /** Four weeks, a column per day; Earlier and Later move a week. */
    Week,

    /** Thirteen weeks; Earlier and Later move thirty days. */
    Month,

    /** Nine months; Earlier and Later move a quarter. */
    Quarter,

    /** Two years; Earlier and Later move half a year. */
    Year,
}
