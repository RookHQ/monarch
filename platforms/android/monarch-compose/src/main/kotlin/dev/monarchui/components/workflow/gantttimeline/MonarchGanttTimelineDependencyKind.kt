package dev.monarchui.components.workflow.gantttimeline

/** How two items of a `MonarchGanttTimeline` depend on each other. */
public enum class MonarchGanttTimelineDependencyKind {
    /** The second item starts after the first ends. The arrow runs from the end of the first bar. */
    FinishToStart,

    /** The second item starts with the first. The arrow runs from the start of the first bar. */
    StartToStart,

    /** The second item ends with the first. The arrow runs to the end of the second bar. */
    FinishToFinish,
}
