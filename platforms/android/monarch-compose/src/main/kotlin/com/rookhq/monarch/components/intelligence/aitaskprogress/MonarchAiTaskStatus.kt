package com.rookhq.monarch.components.intelligence.aitaskprogress

/** Where a long-running task of a `MonarchAiTaskProgress` is. The same names exist on every platform. */
public enum class MonarchAiTaskStatus {
    /** Working. Cancel and notify are offered. */
    Running,

    /** Stopped for now, for example waiting for an approval. Cancel and notify are still offered. */
    Paused,

    /** Finished. */
    Done,

    /** Stopped by an error. Announced assertively. */
    Failed,

    /** Stopped by the person. */
    Cancelled,
}
