package com.rookhq.monarch.components.intelligence.aisuggestedactions

/** Where a suggested action of `MonarchAiSuggestedActions` is in its run. */
public enum class MonarchAiSuggestedActionStatus {
    /** Run is available. */
    Ready,

    /** The action is running: Run shows a progress indicator and "Running…" and ignores taps. */
    Running,

    /** The action finished: Run is replaced by "Done". */
    Done,

    /** The run failed: a line says to try again and Run is available again. */
    Failed,
}
