/// What a ``MonarchPageTreeMove`` moved.
public enum MonarchPageTreeMoveKind: Sendable, Hashable, CaseIterable {
    /// A page, within its group or to another group.
    case page
    /// A group, among the groups.
    case group
}
