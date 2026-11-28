/// What the row builder of ``MonarchVirtualMessageList`` receives with each
/// message.
public struct MonarchVirtualMessageListContext<Item: MonarchVirtualMessageListItem> {
    /// The message continues the previous one's group: same author, same
    /// day, within the group window, and no divider between them. Draw a
    /// compact row, for example `MonarchMessageRow(…, isGrouped: true)`.
    public let isGrouped: Bool
    /// The position in the list, oldest first.
    public let index: Int
    /// The message before, if any.
    public let previous: Item?
    /// The message is the list's `highlightedID`, for example after a link to it.
    public let isHighlighted: Bool

    /// A context, built by the list; public so that row views can be previewed.
    ///
    /// - Parameters:
    ///   - isGrouped: Whether the message continues the previous one's group.
    ///   - index: The position in the list.
    ///   - previous: The message before.
    ///   - isHighlighted: Whether the message is highlighted.
    public init(isGrouped: Bool, index: Int, previous: Item?, isHighlighted: Bool) {
        self.isGrouped = isGrouped
        self.index = index
        self.previous = previous
        self.isHighlighted = isHighlighted
    }
}
