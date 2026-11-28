package com.rookhq.monarch.components.intelligence.aisummarycard

/** Where a model-written summary is in its life. */
public enum class MonarchAiSummaryCardStatus {
    /** The summary is current. */
    Ready,

    /** A summary is being written: a placeholder, or the previous summary with Refresh busy. */
    Generating,

    /** The data changed since the summary was written; a warning asks for a refresh. */
    Stale,

    /** The summary could not be generated; an error replaces it. */
    Error,
}
