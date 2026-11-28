package dev.monarchui.components.intelligence.aitraceview

/** How a span ended, or `Running` while it has not. */
public enum class MonarchAiTraceSpanStatus {
    /** It finished without an error. */
    Ok,

    /** It failed. */
    Error,

    /** It was cancelled. */
    Cancelled,

    /** It has not ended. */
    Running,
}
