/// Built-in strings of the editable cell, from the `MonarchEditableCell` strings table.
enum MonarchEditableCellStrings {
    private static let table = "MonarchEditableCell"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.editableCell.\(key)", table: table)
    }

    /// The hint of the cell in view mode.
    static var edit: String { string("edit") }
    /// The spoken value while a save runs.
    static var saving: String { string("saving") }
    /// The name of the button that leaves the editor.
    static var cancel: String { string("cancel") }
    /// The name of the button that saves the editor.
    static var save: String { string("save") }
    /// The message of a number draft that is not a number.
    static var invalidNumber: String { string("invalidNumber") }
    /// The message of an empty required cell.
    static var required: String { string("required") }
    /// The choice that clears a select cell.
    static var noValue: String { string("noValue") }
    /// The message of a failed save without a description.
    static var saveFailed: String { string("saveFailed") }

    /// The message of a value below the minimum.
    static func belowMinimum(_ minimum: String) -> String {
        MonarchStrings.string("monarch.editableCell.belowMinimum", table: table, minimum)
    }

    /// The message of a value above the maximum.
    static func aboveMaximum(_ maximum: String) -> String {
        MonarchStrings.string("monarch.editableCell.aboveMaximum", table: table, maximum)
    }

    /// The spoken hint of an editor with an error.
    static func error(_ message: String) -> String {
        MonarchStrings.string("monarch.editableCell.error", table: table, message)
    }
}
