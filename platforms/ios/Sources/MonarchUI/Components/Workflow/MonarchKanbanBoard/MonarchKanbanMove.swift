/// A finished move in a ``MonarchKanbanBoard``.
public struct MonarchKanbanMove<ItemID: Hashable>: Hashable {
    /// The id of the card moved.
    public let itemID: ItemID
    /// The column it came from.
    public let fromColumnID: String
    /// Its position there, from 0.
    public let fromIndex: Int
    /// The column it went to.
    public let toColumnID: String
    /// Its position there, from 0.
    public let toIndex: Int

    /// A move.
    ///
    /// - Parameters:
    ///   - itemID: The id of the card moved.
    ///   - fromColumnID: The column it came from.
    ///   - fromIndex: Its position there.
    ///   - toColumnID: The column it went to.
    ///   - toIndex: Its position there.
    public init(itemID: ItemID, fromColumnID: String, fromIndex: Int, toColumnID: String, toIndex: Int) {
        self.itemID = itemID
        self.fromColumnID = fromColumnID
        self.fromIndex = fromIndex
        self.toColumnID = toColumnID
        self.toIndex = toIndex
    }
}

extension MonarchKanbanMove: Sendable where ItemID: Sendable {}
