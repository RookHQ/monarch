package dev.monarchui.components.feedback.consequencelist

/** What happens to the thing a `MonarchConsequence` describes. Grouped lists show the kinds in this order. */
public enum class MonarchConsequenceKind {
    /** Deleted or lost. Its count is in the danger colour. */
    Removed,

    /** Altered or moved. */
    Changed,

    /** Something to know before continuing. */
    Warning,

    /** Stays as it is. */
    Kept,
}
