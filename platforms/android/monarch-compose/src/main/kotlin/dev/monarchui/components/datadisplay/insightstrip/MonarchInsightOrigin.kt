package dev.monarchui.components.datadisplay.insightstrip

/** Where an insight in a `MonarchInsightStrip` came from. It is always shown. */
public enum class MonarchInsightOrigin {
    /** A fixed rule that people wrote, for example "3 domains expire in 30 days". */
    Rule,

    /** A model produced it. Use only when that is true. */
    Ai,
}
