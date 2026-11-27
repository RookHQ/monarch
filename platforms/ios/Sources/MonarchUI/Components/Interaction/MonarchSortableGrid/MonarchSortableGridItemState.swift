/// What the content closure of ``MonarchSortableGrid`` knows about the tile it draws.
public struct MonarchSortableGridItemState: Sendable, Hashable {
    /// The position of the item, starting at 0.
    public let index: Int
    /// Whether a dragged tile is over this tile, so dropping would put it here.
    public let isDropTarget: Bool
    /// Whether this item cannot be moved, because the item or the whole grid is disabled.
    public let isDisabled: Bool

    /// The state of one tile.
    ///
    /// - Parameters:
    ///   - index: The position of the item, starting at 0.
    ///   - isDropTarget: Whether a dragged tile is over this tile.
    ///   - isDisabled: Whether this item cannot be moved.
    public init(index: Int, isDropTarget: Bool, isDisabled: Bool) {
        self.index = index
        self.isDropTarget = isDropTarget
        self.isDisabled = isDisabled
    }
}
