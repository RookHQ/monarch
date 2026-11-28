/// One column of a ``MonarchKanbanBoard``: a stage of work such as "To do", "Doing" or "Done".
public struct MonarchKanbanColumn<Item: Identifiable>: Identifiable {
    /// A stable, unique id, passed back in moves.
    public let id: String
    /// The heading, already localised. Also used in the Move menu and announcements.
    public var title: String
    /// The cards, top to bottom.
    public var items: [Item]
    /// The work-in-progress limit. The header shows "2 / 3" and marks the column at and over the
    /// limit; with `isWipLimitEnforced` a full column refuses cards from other columns.
    public var wipLimit: Int?
    /// Shown when the column has no cards. By default "No cards".
    public var emptyText: String?
    /// An SF Symbol name shown before the title. Decorative.
    public var systemImage: String?

    /// A column.
    ///
    /// - Parameters:
    ///   - id: A stable, unique id.
    ///   - title: The heading, already localised.
    ///   - items: The cards, top to bottom.
    ///   - wipLimit: The work-in-progress limit.
    ///   - emptyText: Shown when the column has no cards.
    ///   - systemImage: An SF Symbol name shown before the title.
    public init(
        id: String,
        title: String,
        items: [Item],
        wipLimit: Int? = nil,
        emptyText: String? = nil,
        systemImage: String? = nil
    ) {
        self.id = id
        self.title = title
        self.items = items
        self.wipLimit = wipLimit
        self.emptyText = emptyText
        self.systemImage = systemImage
    }

    /// The same column with other cards.
    ///
    /// - Parameter items: The new cards.
    /// - Returns: A copy of the column with `items`.
    public func withItems(_ items: [Item]) -> MonarchKanbanColumn<Item> {
        var copy = self
        copy.items = items
        return copy
    }
}

extension MonarchKanbanColumn: Equatable where Item: Equatable {}
extension MonarchKanbanColumn: Hashable where Item: Hashable {}
extension MonarchKanbanColumn: Sendable where Item: Sendable {}
