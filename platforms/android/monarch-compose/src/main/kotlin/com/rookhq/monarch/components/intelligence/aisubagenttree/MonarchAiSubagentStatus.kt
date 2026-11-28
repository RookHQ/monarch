package com.rookhq.monarch.components.intelligence.aisubagenttree

/** Where one agent of a multi-agent run is. The order is the order of the tree's summary. */
public enum class MonarchAiSubagentStatus {
    /** Running a step now. */
    Working,

    /** Stopped until a person answers it. */
    NeedsInput,

    /** Not started yet, or waiting for another agent. */
    Waiting,

    /** Finished its work. */
    Done,

    /** Ended with an error. */
    Failed,

    /** Stopped by a person or by its lead. */
    Stopped,
}
