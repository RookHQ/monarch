/// How sure a ``MonarchColumnMappingSuggestion`` is.
public enum MonarchColumnMappingConfidence: Sendable, Hashable, CaseIterable {
    /// The header equals the field's id, label or an alias, ignoring case
    /// and punctuation. Shown as "Matched".
    case exact
    /// A close match. Shown as "Suggested".
    case similar
}
