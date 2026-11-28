/// Built-in strings of ``MonarchCombobox``, from the `MonarchCombobox` strings table.
enum MonarchComboboxStrings {
    private static let table = "MonarchCombobox"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.combobox.\(key)", table: table)
    }

    /// The search field's placeholder.
    static var search: String { string("search") }
    /// Shown while options load.
    static var loading: String { string("loading") }
    /// Shown when no option matches.
    static var empty: String { string("empty") }
    /// The create option.
    static func create(_ query: String) -> String {
        MonarchStrings.string("monarch.combobox.create", table: table, query)
    }
    /// Shown under a list cut short by the limit.
    static func more(shown: Int, total: Int) -> String {
        MonarchStrings.string("monarch.combobox.more", table: table, shown, total)
    }
    /// Closes the list without choosing.
    static var cancel: String { string("cancel") }
    /// Clears the selection.
    static var clear: String { string("clear") }
    /// The spoken value while nothing is chosen.
    static var noSelection: String { string("noSelection") }
    /// Shown when the options failed to load and the app gave no message.
    static var loadFailed: String { string("loadFailed") }
}
