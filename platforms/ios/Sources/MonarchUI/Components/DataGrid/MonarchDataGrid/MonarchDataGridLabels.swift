/// Replacements for the built-in strings of ``MonarchDataGrid``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchDataGridLabels: Sendable {
    /// The select-all checkbox in the header. Built in: "Select all rows".
    public var selectAll: String?
    /// A row's checkbox, from the row's name. Built in: "Select INV-1".
    public var selectRow: (@Sendable (String) -> String)?
    /// The count of selected rows. Built in: "3 selected".
    public var selectedCount: (@Sendable (Int) -> String)?
    /// A row's actions menu, from the row's name. Built in: "Actions for INV-1".
    public var rowActions: (@Sendable (String) -> String)?
    /// The header of the actions column. Built in: "Actions".
    public var actionsHeader: String?
    /// The hint of a header that opens the column menu. Built in: "Column options".
    public var columnOptions: String?
    /// The column menu item that sorts ascending. Built in: "Sort ascending".
    public var sortAscending: String?
    /// The column menu item that sorts descending. Built in: "Sort descending".
    public var sortDescending: String?
    /// The column menu item that removes the sort. Built in: "Clear sort".
    public var clearSort: String?
    /// The column menu item that pins the column to the leading edge. Built in: "Pin to start".
    public var pinStart: String?
    /// The column menu item that pins the column to the trailing edge. Built in: "Pin to end".
    public var pinEnd: String?
    /// The column menu item that unpins the column. Built in: "Unpin".
    public var unpin: String?
    /// The column menu item that hides the column. Built in: "Hide column".
    public var hideColumn: String?
    /// The column menu item that makes the column wider. Built in: "Widen column".
    public var widen: String?
    /// The column menu item that makes the column narrower. Built in: "Narrow column".
    public var narrow: String?
    /// The spoken state of a header sorted ascending. Built in: "Sorted ascending".
    public var sortedAscending: String?
    /// The spoken state of a header sorted descending. Built in: "Sorted descending".
    public var sortedDescending: String?
    /// The spoken state of a sortable header that is not sorted. Built in: "Not sorted".
    public var notSorted: String?
    /// The spoken state of a header pinned to the leading edge. Built in: "Pinned to start".
    public var pinnedStart: String?
    /// The spoken state of a header pinned to the trailing edge. Built in: "Pinned to end".
    public var pinnedEnd: String?
    /// The action that opens a row's details. Built in: "Open details".
    public var open: String?
    /// The sort menu of the record list. Built in: the sort title of ``MonarchDataTable``.
    public var sort: String?
    /// The hint of a record that opens the record editor, and the editor's title when the row has no name. Built in: "Edit".
    public var editRecord: String?
    /// The button that closes the record editor. Built in: "Done".
    public var done: String?
    /// Read for an empty cell. Built in: "No value".
    public var noValue: String?
    /// The spoken name of the first-load placeholder. Built in: "Loading rows".
    public var loading: String?
    /// The empty state title. Built in: "Nothing here yet".
    public var emptyTitle: String?
    /// The empty state message. Built in: "Rows appear here once they are added.".
    public var emptyMessage: String?
    /// An editable cell, from the column and the row's name. Built in: "Customer for INV-1".
    public var editLabel: (@Sendable (String, String) -> String)?
    /// A read-only cell, from the column and the value. Built in: "Status: Paid".
    public var cell: (@Sendable (String, String) -> String)?
    /// Replacements for the built-in strings of the editable cells.
    public var editableCell: MonarchEditableCellLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        selectAll: String? = nil,
        selectRow: (@Sendable (String) -> String)? = nil,
        selectedCount: (@Sendable (Int) -> String)? = nil,
        rowActions: (@Sendable (String) -> String)? = nil,
        actionsHeader: String? = nil,
        columnOptions: String? = nil,
        sortAscending: String? = nil,
        sortDescending: String? = nil,
        clearSort: String? = nil,
        pinStart: String? = nil,
        pinEnd: String? = nil,
        unpin: String? = nil,
        hideColumn: String? = nil,
        widen: String? = nil,
        narrow: String? = nil,
        sortedAscending: String? = nil,
        sortedDescending: String? = nil,
        notSorted: String? = nil,
        pinnedStart: String? = nil,
        pinnedEnd: String? = nil,
        open: String? = nil,
        sort: String? = nil,
        editRecord: String? = nil,
        done: String? = nil,
        noValue: String? = nil,
        loading: String? = nil,
        emptyTitle: String? = nil,
        emptyMessage: String? = nil,
        editLabel: (@Sendable (String, String) -> String)? = nil,
        cell: (@Sendable (String, String) -> String)? = nil,
        editableCell: MonarchEditableCellLabels = MonarchEditableCellLabels()
    ) {
        self.selectAll = selectAll
        self.selectRow = selectRow
        self.selectedCount = selectedCount
        self.rowActions = rowActions
        self.actionsHeader = actionsHeader
        self.columnOptions = columnOptions
        self.sortAscending = sortAscending
        self.sortDescending = sortDescending
        self.clearSort = clearSort
        self.pinStart = pinStart
        self.pinEnd = pinEnd
        self.unpin = unpin
        self.hideColumn = hideColumn
        self.widen = widen
        self.narrow = narrow
        self.sortedAscending = sortedAscending
        self.sortedDescending = sortedDescending
        self.notSorted = notSorted
        self.pinnedStart = pinnedStart
        self.pinnedEnd = pinnedEnd
        self.open = open
        self.sort = sort
        self.editRecord = editRecord
        self.done = done
        self.noValue = noValue
        self.loading = loading
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
        self.editLabel = editLabel
        self.cell = cell
        self.editableCell = editableCell
    }
}
