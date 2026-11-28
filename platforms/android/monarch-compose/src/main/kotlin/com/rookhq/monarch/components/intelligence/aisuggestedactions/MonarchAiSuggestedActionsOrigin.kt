package com.rookhq.monarch.components.intelligence.aisuggestedactions

/**
 * Where the suggestions of `MonarchAiSuggestedActions` came from. It is
 * always shown next to the heading, so people know whether a model or a fixed
 * rule suggested them.
 */
public enum class MonarchAiSuggestedActionsOrigin {
    /** Suggested by a model. Labelled "Suggested by AI" in the assistant colours. Use only when that is true. */
    Ai,

    /** Suggested by rules written by people. Labelled "Rule-based" in neutral colours. */
    Rule,
}
