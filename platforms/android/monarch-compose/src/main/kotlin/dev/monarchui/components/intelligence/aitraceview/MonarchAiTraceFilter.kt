package dev.monarchui.components.intelligence.aitraceview

/** Which spans a `MonarchAiTraceView` shows, and in what order. */
public enum class MonarchAiTraceFilter {
    /** Every span, children in start order. */
    All,

    /** Failed spans and the spans they ran inside. */
    Errors,

    /** Every span, children sorted slowest first. */
    Slowest,
}
