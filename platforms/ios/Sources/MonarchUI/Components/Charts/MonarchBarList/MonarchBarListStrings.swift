/// Built-in strings of the bar list, from the `MonarchBarList` strings table.
enum MonarchBarListStrings {
    /// Spoken for a row without a value.
    static var missing: String { MonarchStrings.string("monarch.barList.missing", table: "MonarchBarList") }
    /// Shown when there are no rows.
    static var empty: String { MonarchStrings.string("monarch.barList.empty", table: "MonarchBarList") }
    /// Shown while the rows are loading.
    static var loading: String { MonarchStrings.string("monarch.barList.loading", table: "MonarchBarList") }

    /// The footer when `limit` hides rows.
    static func overflow(shown: String, total: String) -> String {
        MonarchStrings.string("monarch.barList.overflow", table: "MonarchBarList", shown, total)
    }
}
