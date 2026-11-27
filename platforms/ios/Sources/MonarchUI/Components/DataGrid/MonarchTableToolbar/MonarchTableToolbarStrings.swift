/// Built-in strings of ``MonarchTableToolbar``, from the `MonarchTableToolbar` strings table.
enum MonarchTableToolbarStrings {
    private static let table = "MonarchTableToolbar"

    /// The spoken name of the toolbar.
    static var toolbar: String { MonarchStrings.string("monarch.tableToolbar.toolbar", table: table) }

    /// "1 result" or "1,204 results", with the count formatted for the locale.
    static func results(_ count: Int) -> String {
        let key = count == 1 ? "monarch.tableToolbar.result" : "monarch.tableToolbar.results"
        return MonarchStrings.string(key, table: table, count.formatted())
    }
}
