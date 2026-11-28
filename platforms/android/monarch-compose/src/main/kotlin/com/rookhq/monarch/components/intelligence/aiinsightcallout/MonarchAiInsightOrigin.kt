package com.rookhq.monarch.components.intelligence.aiinsightcallout

/**
 * Where an insight in `MonarchAiInsightCallout` came from. It is always
 * shown, so people know whether a model or a fixed rule raised it.
 */
public enum class MonarchAiInsightOrigin {
    /** Produced by a model. Labelled "AI insight" in the assistant colours. Use only when that is true. */
    Ai,

    /** Raised by a fixed rule written by people. Labelled "Rule-based" in neutral colours. */
    Rule,
}
