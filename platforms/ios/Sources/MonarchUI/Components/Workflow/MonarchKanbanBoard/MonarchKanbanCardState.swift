/// What the card content of a ``MonarchKanbanBoard`` knows about the card it draws.
public struct MonarchKanbanCardState: Hashable, Sendable {
    /// The column the card is in.
    public let columnID: String
    /// Its position in the column, from 0.
    public let index: Int
    /// A dragged card is over this card and will take its place when dropped.
    public let isDropTarget: Bool
    /// The card's move is waiting for `onMove` to finish.
    public let isPending: Bool
    /// The card can be moved.
    public let isEnabled: Bool

    /// A card state.
    ///
    /// - Parameters:
    ///   - columnID: The column the card is in.
    ///   - index: Its position in the column.
    ///   - isDropTarget: A dragged card is over this card.
    ///   - isPending: The card's move is being saved.
    ///   - isEnabled: The card can be moved.
    public init(columnID: String, index: Int, isDropTarget: Bool, isPending: Bool, isEnabled: Bool) {
        self.columnID = columnID
        self.index = index
        self.isDropTarget = isDropTarget
        self.isPending = isPending
        self.isEnabled = isEnabled
    }
}
