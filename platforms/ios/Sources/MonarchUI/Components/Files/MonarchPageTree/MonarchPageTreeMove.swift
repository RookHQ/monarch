/// What changed in a reorder of a ``MonarchPageTree``.
public struct MonarchPageTreeMove: Hashable, Sendable {
    /// Whether a page or a group moved.
    public let kind: MonarchPageTreeMoveKind
    /// The id of the page or group that moved.
    public let id: String
    /// For a page: the group it left. `nil` for a group.
    public let fromGroupID: String?
    /// For a page: the group it is in now. `nil` for a group.
    public let toGroupID: String?
    /// The new index within its group (a page) or among the groups (a group).
    public let toIndex: Int

    /// A move.
    ///
    /// - Parameters:
    ///   - kind: Whether a page or a group moved.
    ///   - id: The id of the page or group that moved.
    ///   - fromGroupID: For a page: the group it left.
    ///   - toGroupID: For a page: the group it is in now.
    ///   - toIndex: The new index within its group or among the groups.
    public init(
        kind: MonarchPageTreeMoveKind,
        id: String,
        fromGroupID: String? = nil,
        toGroupID: String? = nil,
        toIndex: Int
    ) {
        self.kind = kind
        self.id = id
        self.fromGroupID = fromGroupID
        self.toGroupID = toGroupID
        self.toIndex = toIndex
    }
}
