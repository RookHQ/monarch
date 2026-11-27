/// Built-in strings of the column chooser, from the `MonarchColumnChooser` strings table.
enum MonarchColumnChooserStrings {
    private static let table = "MonarchColumnChooser"

    /// The title.
    static var title: String { MonarchStrings.string("monarch.columnChooser.title", table: table) }
    /// The prompt of the search field.
    static var search: String { MonarchStrings.string("monarch.columnChooser.search", table: table) }
    /// Shown when the search matches nothing.
    static var noMatches: String { MonarchStrings.string("monarch.columnChooser.noMatches", table: table) }
    /// The note under a column that cannot be hidden.
    static var required: String { MonarchStrings.string("monarch.columnChooser.required", table: table) }
    /// The button that shows every column.
    static var showAll: String { MonarchStrings.string("monarch.columnChooser.showAll", table: table) }
    /// The button that resets the columns.
    static var reset: String { MonarchStrings.string("monarch.columnChooser.reset", table: table) }
    /// The name of the clear-search button.
    static var clearSearch: String { MonarchStrings.string("monarch.columnChooser.clearSearch", table: table) }

    /// "5 of 8 shown".
    static func shownCount(_ shown: Int, _ total: Int) -> String {
        MonarchStrings.string("monarch.columnChooser.shownCount", table: table, shown, total)
    }

    /// "Move Status up".
    static func moveUp(_ column: String) -> String {
        MonarchStrings.string("monarch.columnChooser.moveUp", table: table, column)
    }

    /// "Move Status down".
    static func moveDown(_ column: String) -> String {
        MonarchStrings.string("monarch.columnChooser.moveDown", table: table, column)
    }

    /// "Status moved to position 3 of 8."
    static func moved(_ column: String, _ position: Int, _ total: Int) -> String {
        MonarchStrings.string("monarch.columnChooser.moved", table: table, column, position, total)
    }
}
