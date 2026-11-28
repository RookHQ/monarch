/// How a ``MonarchFileCard`` is arranged.
public enum MonarchFileCardLayout: Sendable, Hashable, CaseIterable {
    /// A preview above the name, for grids. The default.
    case card
    /// One line with a small preview, for lists.
    case row
}
