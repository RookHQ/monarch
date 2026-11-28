package dev.monarchui.components.datagrid.columnmapping

/** How sure a `MonarchColumnMappingSuggestion` is. */
public enum class MonarchColumnMappingConfidence {
    /** The header equals the field's id, label or an alias, ignoring case and punctuation: "Matched". */
    Exact,

    /** A close match by words, containment or spelling: "Suggested". */
    Similar,
}
