/// Built-in strings of ``MonarchMultiSelect``, from the `MonarchMultiSelect` strings table.
enum MonarchMultiSelectStrings {
    private static let table = "MonarchMultiSelect"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.multiSelect.\(key)", table: table)
    }

    /// The field's button with nothing chosen.
    static var select: String { string("select") }
    /// The field's button with something chosen.
    static var change: String { string("change") }
    /// Closes the list.
    static var done: String { string("done") }
    /// "2 of 3 selected".
    static func count(_ count: Int, _ limit: Int) -> String {
        MonarchStrings.string("monarch.multiSelect.count", table: table, count, limit)
    }
}
