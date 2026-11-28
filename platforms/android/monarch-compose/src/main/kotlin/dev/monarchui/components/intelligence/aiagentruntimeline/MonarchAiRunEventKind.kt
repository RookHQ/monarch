package dev.monarchui.components.intelligence.aiagentruntimeline

/** What an event of an agent run is, for `MonarchAiAgentRunTimeline`. */
public enum class MonarchAiRunEventKind {
    /** A call to a model. */
    Model,

    /** A call to a tool. */
    Tool,

    /** A hand-off to another agent. */
    Handoff,

    /** A failure. */
    Error,
}
