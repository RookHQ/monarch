import CoreGraphics

/// The pure move logic of ``MonarchKanbanBoard``.
enum MonarchKanbanBoardModel {
    /// Where a card is: its column's position and its own.
    static func location<Item: Identifiable>(
        of itemID: Item.ID,
        in columns: [MonarchKanbanColumn<Item>]
    ) -> (column: Int, index: Int)? {
        for (c, column) in columns.enumerated() {
            if let index = column.items.firstIndex(where: { $0.id == itemID }) { return (c, index) }
        }
        return nil
    }

    /// The columns with a card at a new place: taken out, then put at `toIndex` (clamped) of the
    /// target column. `nil` when the card or column is unknown or nothing changes.
    static func moving<Item: Identifiable>(
        _ columns: [MonarchKanbanColumn<Item>],
        itemID: Item.ID,
        toColumnID: String,
        toIndex: Int
    ) -> (columns: [MonarchKanbanColumn<Item>], move: MonarchKanbanMove<Item.ID>)? {
        guard let from = location(of: itemID, in: columns),
            let target = columns.firstIndex(where: { $0.id == toColumnID })
        else { return nil }
        var next = columns
        let item = next[from.column].items.remove(at: from.index)
        let index = max(0, min(toIndex, next[target].items.count))
        if target == from.column && index == from.index { return nil }
        next[target].items.insert(item, at: index)
        let move = MonarchKanbanMove(
            itemID: itemID,
            fromColumnID: columns[from.column].id,
            fromIndex: from.index,
            toColumnID: toColumnID,
            toIndex: index
        )
        return (next, move)
    }

    /// The columns with a failed move undone: the card goes back to its column and position
    /// (clamped), wherever it is now. Unchanged when the card or its column is gone.
    static func reverting<Item: Identifiable>(
        _ columns: [MonarchKanbanColumn<Item>],
        _ move: MonarchKanbanMove<Item.ID>
    ) -> [MonarchKanbanColumn<Item>] {
        moving(columns, itemID: move.itemID, toColumnID: move.fromColumnID, toIndex: move.fromIndex)?.columns
            ?? columns
    }

    /// The place a dropped card takes. Dropped on a card (`onto` is its index) it takes that
    /// card's place; dropped on the end of a column (`onto` is `nil`) it goes last.
    static func dropIndex(isSameColumn: Bool, onto index: Int?, count: Int) -> Int {
        guard let index else { return isSameColumn ? max(0, count - 1) : count }
        return max(0, min(index, isSameColumn ? count - 1 : count))
    }

    /// Whether a column takes a card. Its own column always does; another column refuses it when
    /// `canMove` says no, or when limits are enforced and the column is at or over its limit.
    static func allows<Item: Identifiable>(
        _ item: Item,
        toColumnID: String,
        fromColumnID: String,
        columns: [MonarchKanbanColumn<Item>],
        canMove: ((Item, String, String) -> Bool)?,
        isWipLimitEnforced: Bool
    ) -> Bool {
        if toColumnID == fromColumnID { return true }
        guard let target = columns.first(where: { $0.id == toColumnID }) else { return false }
        if isWipLimitEnforced, let limit = target.wipLimit, target.items.count >= limit { return false }
        return canMove?(item, toColumnID, fromColumnID) ?? true
    }

    /// The place one step up or down in the card's own column, or `nil` at the ends.
    static func step<Item: Identifiable>(
        _ columns: [MonarchKanbanColumn<Item>],
        itemID: Item.ID,
        up: Bool
    ) -> Int? {
        guard let from = location(of: itemID, in: columns) else { return nil }
        let target = from.index + (up ? -1 : 1)
        return columns[from.column].items.indices.contains(target) ? target : nil
    }

    /// How many columns fit side by side: as many of `minimumWidth` as `width` holds, at least one,
    /// at most the number of columns.
    static func columnsPerPage(width: CGFloat, minimumWidth: CGFloat, spacing: CGFloat, count: Int) -> Int {
        guard count > 0 else { return 1 }
        guard width > 0, minimumWidth > 0 else { return 1 }
        let fit = Int(((width + spacing) / (minimumWidth + spacing)).rounded(.down))
        return max(1, min(fit, count))
    }

    /// The drag payload of a card: the board's own namespace and the card's id as text, so drops
    /// from another board or another app are ignored.
    static func payload<ID: Hashable>(namespace: String, itemID: ID) -> String {
        "\(namespace)#\(String(describing: itemID))"
    }

    /// The id of the card a payload from this board names, or `nil`.
    static func itemID<Item: Identifiable>(
        payload: String,
        namespace: String,
        in columns: [MonarchKanbanColumn<Item>]
    ) -> Item.ID? {
        let prefix = "\(namespace)#"
        guard payload.hasPrefix(prefix) else { return nil }
        let key = String(payload.dropFirst(prefix.count))
        return columns.lazy.flatMap(\.items).first { String(describing: $0.id) == key }?.id
    }
}
