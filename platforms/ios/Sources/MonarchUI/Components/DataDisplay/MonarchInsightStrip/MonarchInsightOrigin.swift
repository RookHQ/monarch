/// Where an insight came from. It is always shown, so people can tell a
/// fixed rule from the output of a model.
public enum MonarchInsightOrigin: Sendable, Hashable, CaseIterable {
    /// A fixed, deterministic rule written by people, such as "the invoice
    /// is 30 days overdue".
    case rule
    /// Produced by a model.
    case ai
}
