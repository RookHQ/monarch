package dev.monarchui.components.intelligence.aiagentruntimeline

/** How `MonarchAiAgentRunTimeline` shows the run. */
public enum class MonarchAiAgentRunTimelineMode {
    /** Bars on a shared time axis, one row per event, each with details that open. */
    Timeline,

    /** A table of the same events that scrolls sideways. */
    Table,
}
