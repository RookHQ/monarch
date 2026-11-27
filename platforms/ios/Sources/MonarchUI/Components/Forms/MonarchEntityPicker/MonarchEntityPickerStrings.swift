/// Built-in strings of ``MonarchEntityPicker``, from the `MonarchEntityPicker` strings table.
enum MonarchEntityPickerStrings {
    private static let table = "MonarchEntityPicker"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.entityPicker.\(key)", table: table)
    }

    /// The field with nothing chosen.
    static var select: String { string("select") }
    /// The search field's placeholder.
    static var search: String { string("search") }
    /// Closes a single-choice list.
    static var cancel: String { string("cancel") }
    /// Closes a multiple-choice list.
    static var done: String { string("done") }
    /// Results are loading.
    static var loading: String { string("loading") }
    /// Nothing matches.
    static var empty: String { string("empty") }
    /// "Type at least 2 characters to search."
    static func tooShort(_ length: Int) -> String {
        MonarchStrings.string("monarch.entityPicker.tooShort", table: table, length)
    }
    /// The search failed.
    static var failed: String { string("failed") }
    /// "Create “text”".
    static func create(_ text: String) -> String {
        MonarchStrings.string("monarch.entityPicker.create", table: table, text)
    }
    /// "Ana Silva +2".
    static func summary(_ first: String, _ more: Int) -> String {
        MonarchStrings.string("monarch.entityPicker.summary", table: table, first, more)
    }
    /// Clears the selection.
    static var clear: String { string("clear") }
    /// The spoken value with nothing chosen.
    static var noSelection: String { string("noSelection") }
}
