/// Built-in strings of the data grid, from the `MonarchDataGrid` strings table.
enum MonarchDataGridStrings {
    private static let table = "MonarchDataGrid"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.dataGrid.\(key)", table: table)
    }

    /// The select-all checkbox in the header.
    static var selectAll: String { string("selectAll") }
    /// The header of the actions column.
    static var actionsHeader: String { string("actionsHeader") }
    /// The hint of a header that opens the column menu.
    static var columnOptions: String { string("columnOptions") }
    /// The column menu item that sorts ascending.
    static var sortAscending: String { string("sortAscending") }
    /// The column menu item that sorts descending.
    static var sortDescending: String { string("sortDescending") }
    /// The column menu item that removes the sort.
    static var clearSort: String { string("clearSort") }
    /// The column menu item that pins the column to the leading edge.
    static var pinStart: String { string("pinStart") }
    /// The column menu item that pins the column to the trailing edge.
    static var pinEnd: String { string("pinEnd") }
    /// The column menu item that unpins the column.
    static var unpin: String { string("unpin") }
    /// The column menu item that hides the column.
    static var hideColumn: String { string("hideColumn") }
    /// The column menu item that makes the column wider.
    static var widen: String { string("widen") }
    /// The column menu item that makes the column narrower.
    static var narrow: String { string("narrow") }
    /// The spoken state of a header sorted ascending.
    static var sortedAscending: String { string("sortedAscending") }
    /// The spoken state of a header sorted descending.
    static var sortedDescending: String { string("sortedDescending") }
    /// The spoken state of a sortable header that is not sorted.
    static var notSorted: String { string("notSorted") }
    /// The spoken state of a header pinned to the leading edge.
    static var pinnedStart: String { string("pinnedStart") }
    /// The spoken state of a header pinned to the trailing edge.
    static var pinnedEnd: String { string("pinnedEnd") }
    /// The action that opens a row's details.
    static var open: String { string("open") }
    /// The hint of a record that opens the record editor, and the editor's title when the row has no name.
    static var editRecord: String { string("editRecord") }
    /// The button that closes the record editor.
    static var done: String { string("done") }
    /// Read for an empty cell.
    static var noValue: String { string("noValue") }
    /// The spoken name of the first-load placeholder.
    static var loading: String { string("loading") }
    /// The empty state title.
    static var emptyTitle: String { string("emptyTitle") }
    /// The empty state message.
    static var emptyMessage: String { string("emptyMessage") }
    /// "Select INV-1".
    static func selectRow(_ row: String) -> String {
        MonarchStrings.string("monarch.dataGrid.selectRow", table: table, row)
    }
    /// "Actions for INV-1".
    static func rowActions(_ row: String) -> String {
        MonarchStrings.string("monarch.dataGrid.rowActions", table: table, row)
    }
    /// "Customer for INV-1".
    static func editLabel(_ first: String, _ second: String) -> String {
        MonarchStrings.string("monarch.dataGrid.editLabel", table: table, first, second)
    }
    /// "Status: Paid".
    static func cell(_ first: String, _ second: String) -> String {
        MonarchStrings.string("monarch.dataGrid.cell", table: table, first, second)
    }
    /// "3 selected".
    static func selectedCount(_ count: Int) -> String {
        MonarchStrings.string("monarch.dataGrid.selectedCount", table: table, count)
    }
}
