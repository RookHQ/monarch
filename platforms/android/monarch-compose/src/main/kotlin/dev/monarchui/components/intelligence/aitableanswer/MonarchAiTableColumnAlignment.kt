package dev.monarchui.components.intelligence.aitableanswer

/** Where the values of a `MonarchAiTableAnswer` column sit. Mirrors in right to left. */
public enum class MonarchAiTableColumnAlignment {
    /** At the start: the default for text and dates. */
    Start,

    /** At the end: the default for numbers, so their digits line up. */
    End,
}
