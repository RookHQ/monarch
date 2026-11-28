package com.rookhq.monarch.components.intelligence.aitraceview

/** What a span of a trace did. */
public enum class MonarchAiTraceSpanKind {
    /** A model call. */
    Model,

    /** A tool call. */
    Tool,

    /** A retrieval from an index or a store. */
    Retrieval,

    /** A sub-agent. */
    Agent,

    /** A guardrail check. */
    Guardrail,

    /** Anything else. */
    Other,
}
