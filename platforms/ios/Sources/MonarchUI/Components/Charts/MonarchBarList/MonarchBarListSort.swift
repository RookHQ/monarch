/// The order of the rows of a ``MonarchBarList``.
public enum MonarchBarListSort: Sendable, Hashable, CaseIterable {
    /// The largest value first. The default.
    case descending
    /// The smallest value first.
    case ascending
    /// The order the items were given in.
    case none
}
