/// Replacements for the built-in strings of an editable ``MonarchTableBlock``.
///
/// Every property is `nil` by default, which keeps the built-in English
/// string. Set the ones the app translates.
public struct MonarchTableBlockLabels: Sendable {
    /// The title of the controls menu. Built in: "Table".
    public var controls: String?
    /// The menu item that inserts a row. Built in: "Add row below".
    public var addRow: String?
    /// The menu item that inserts a column. Built in: "Add column after".
    public var addColumn: String?
    /// The menu item that deletes a row. Built in: "Delete row".
    public var deleteRow: String?
    /// The menu item that deletes a column. Built in: "Delete column".
    public var deleteColumn: String?
    /// The menu item that switches the header row. Built in: "Header row".
    public var headerRow: String?
    /// The menu item that deletes the table. Built in: "Delete table".
    public var deleteTable: String?
    /// What VoiceOver reads for an empty cell of a read-only table. Built in: "Empty".
    public var emptyCell: String?
    /// The spoken position of a cell from its row and column, both counted
    /// from 1. Built in: "Row 2, column 1".
    public var cellPosition: (@Sendable (_ row: Int, _ column: Int) -> String)?

    /// Labels that keep every built-in string unless a replacement is given.
    public init(
        controls: String? = nil,
        addRow: String? = nil,
        addColumn: String? = nil,
        deleteRow: String? = nil,
        deleteColumn: String? = nil,
        headerRow: String? = nil,
        deleteTable: String? = nil,
        emptyCell: String? = nil,
        cellPosition: (@Sendable (_ row: Int, _ column: Int) -> String)? = nil
    ) {
        self.controls = controls
        self.addRow = addRow
        self.addColumn = addColumn
        self.deleteRow = deleteRow
        self.deleteColumn = deleteColumn
        self.headerRow = headerRow
        self.deleteTable = deleteTable
        self.emptyCell = emptyCell
        self.cellPosition = cellPosition
    }
}
