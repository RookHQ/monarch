/// One step an accessibility action of ``MonarchDragHandle`` asks for.
public enum MonarchDragHandleDirection: Sendable, Hashable, CaseIterable {
    /// One place earlier in a vertical order.
    case up
    /// One place later in a vertical order.
    case down
    /// One place earlier in a horizontal order: towards the leading edge.
    case backward
    /// One place later in a horizontal order: towards the trailing edge.
    case forward
}
