package com.rookhq.monarch.components.intelligence.aicomputerview

/** Where the session of a `MonarchAiComputerView` is. */
public enum class MonarchAiComputerViewStatus {
    /** The session is starting; the agent holds control. */
    Starting,

    /** The agent is acting. */
    Working,

    /** The person paused the agent; it still holds control. */
    Paused,

    /** The agent waits for the person to do a step it must not do itself. */
    WaitingForYou,

    /** The person has taken control. */
    TakenOver,

    /** The task is finished. */
    Done,

    /** The task failed. */
    Failed,

    /** The person stopped the agent. */
    Stopped,
}
