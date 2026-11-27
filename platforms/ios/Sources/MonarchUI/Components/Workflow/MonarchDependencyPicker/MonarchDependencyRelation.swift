/// How another item relates to this one in a ``MonarchDependencyPicker``. The cases are in display order.
public enum MonarchDependencyRelation: String, Hashable, Sendable, CaseIterable {
    /// The other item must finish first.
    case blockedBy
    /// This item must finish first.
    case blocks
    /// Linked without an order.
    case relates
    /// This item is the same work as the other.
    case duplicates
    /// The other item is the same work as this one.
    case duplicatedBy
}
