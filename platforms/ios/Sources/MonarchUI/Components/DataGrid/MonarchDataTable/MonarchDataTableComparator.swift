import Foundation

/// The sort comparator a ``MonarchDataTable`` column gives SwiftUI's
/// `Table`: it compares two rows by the column's rule. Two comparators are
/// equal when they name the same column and order, so the table's header
/// state survives redraws.
struct MonarchDataTableComparator<Row>: SortComparator {
    /// The column compared by.
    let columnID: String
    /// The direction.
    var order: SortOrder
    /// Whether one row sorts before another ascending.
    let ascends: @Sendable (Row, Row) -> Bool

    /// A comparator for a column.
    init(_ column: MonarchDataTableColumn<Row>, order: SortOrder) {
        self.columnID = column.id
        self.order = order
        self.ascends = { MonarchDataTableTokens.ascends($0, $1, by: column) }
    }

    /// Compares two rows in the comparator's order.
    func compare(_ lhs: Row, _ rhs: Row) -> ComparisonResult {
        let (first, second) = order == .forward ? (lhs, rhs) : (rhs, lhs)
        if ascends(first, second) { return .orderedAscending }
        if ascends(second, first) { return .orderedDescending }
        return .orderedSame
    }

    /// The table's sort as a value.
    var sort: MonarchDataTableSort { MonarchDataTableSort(columnID, order: order) }

    static func == (lhs: Self, rhs: Self) -> Bool {
        lhs.columnID == rhs.columnID && lhs.order == rhs.order
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(columnID)
        hasher.combine(order)
    }
}
