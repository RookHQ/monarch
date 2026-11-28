import Foundation

/// The sort of a ``MonarchDataTable``: one column and a direction.
public struct MonarchDataTableSort: Sendable, Hashable {
    /// The id of the column the rows are sorted by.
    public var columnID: String
    /// `.forward` sorts ascending, `.reverse` descending.
    public var order: SortOrder

    /// A sort.
    ///
    /// - Parameters:
    ///   - columnID: The id of the column the rows are sorted by.
    ///   - order: `.forward` sorts ascending, `.reverse` descending.
    public init(_ columnID: String, order: SortOrder = .forward) {
        self.columnID = columnID
        self.order = order
    }
}
