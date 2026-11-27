package com.rookhq.monarch.components.intelligence.aibackgroundtasks

/** Where a background task of an agent is. */
public enum class MonarchAiBackgroundTaskStatus {
    /** Waiting to start. */
    Queued,

    /** Running. */
    Running,

    /** Waiting on the person. */
    NeedsInput,

    /** Finished with a result. */
    Succeeded,

    /** Finished with an error. */
    Failed,

    /** Stopped before it finished. */
    Cancelled,
}
