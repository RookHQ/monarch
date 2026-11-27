/// Built-in strings of the table block, from the `MonarchTableBlock` strings table.
enum MonarchTableBlockStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.tableBlock.\(key)", table: "MonarchTableBlock")
    }

    /// The spoken name of a table that has no caption and no label.
    static var region: String { string("region") }
    /// The title of the controls menu.
    static var controls: String { string("controls") }
    /// Inserts a row.
    static var addRow: String { string("addRow") }
    /// Inserts a column.
    static var addColumn: String { string("addColumn") }
    /// Deletes a row.
    static var deleteRow: String { string("deleteRow") }
    /// Deletes a column.
    static var deleteColumn: String { string("deleteColumn") }
    /// Switches the header row.
    static var headerRow: String { string("headerRow") }
    /// Deletes the table.
    static var deleteTable: String { string("deleteTable") }
    /// Read for an empty cell.
    static var emptyCell: String { string("emptyCell") }

    /// The spoken position of a cell, counted from 1.
    static func cellPosition(row: Int, column: Int) -> String {
        MonarchStrings.string(
            "monarch.tableBlock.cellPosition",
            table: "MonarchTableBlock",
            row.formatted(),
            column.formatted()
        )
    }
}
