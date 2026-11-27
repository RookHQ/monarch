/// One existing link from this item to another, in a ``MonarchDependencyPicker``.
public struct MonarchDependencyLink: Identifiable, Hashable, Sendable {
    /// The stable id of the link itself.
    public let id: String
    /// How the other item relates to this one.
    public let relation: MonarchDependencyRelation
    /// The other item.
    public let item: MonarchDependencyItem

    /// A link.
    ///
    /// - Parameters:
    ///   - id: The stable id of the link.
    ///   - relation: How the other item relates to this one.
    ///   - item: The other item.
    public init(id: String, relation: MonarchDependencyRelation, item: MonarchDependencyItem) {
        self.id = id
        self.relation = relation
        self.item = item
    }
}
