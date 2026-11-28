/// Built-in strings of the data table, from the `MonarchDataTable` strings table.
enum MonarchDataTableStrings {
    private static let table = "MonarchDataTable"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.dataTable.\(key)", table: table)
    }

    /// The select-all checkbox.
    static var selectAll: String { string("selectAll") }
    /// The header of the actions column.
    static var actionsHeader: String { string("actionsHeader") }
    /// The hint on a row that opens.
    static var openHint: String { string("openHint") }
    /// The sort menu.
    static var sort: String { string("sort") }
    /// The spoken state of an ascending sort.
    static var sortedAscending: String { string("sortedAscending") }
    /// The spoken state of a descending sort.
    static var sortedDescending: String { string("sortedDescending") }
    /// Read for an empty cell.
    static var noValue: String { string("noValue") }
    /// The first-load placeholder.
    static var loading: String { string("loading") }
    /// The empty state title.
    static var emptyTitle: String { string("emptyTitle") }
    /// The empty state message.
    static var emptyMessage: String { string("emptyMessage") }
    /// The title when filters match nothing.
    static var filteredTitle: String { string("filteredTitle") }
    /// The message when filters match nothing.
    static var filteredMessage: String { string("filteredMessage") }
    /// The title when the rows failed to load.
    static var errorTitle: String { string("errorTitle") }

    /// "Select Ticket 42".
    static func selectRow(_ row: String) -> String {
        MonarchStrings.string("monarch.dataTable.selectRow", table: table, row)
    }

    /// "Actions for Ticket 42".
    static func rowActions(_ row: String) -> String {
        MonarchStrings.string("monarch.dataTable.rowActions", table: table, row)
    }

    /// "3 selected".
    static func selectedCount(_ count: Int) -> String {
        MonarchStrings.string("monarch.dataTable.selectedCount", table: table, count)
    }
}
