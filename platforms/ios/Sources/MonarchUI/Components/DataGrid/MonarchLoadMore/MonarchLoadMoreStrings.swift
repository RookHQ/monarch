/// Built-in strings of the load-more footer, from the `MonarchLoadMore` strings table.
enum MonarchLoadMoreStrings {
    private static let table = "MonarchLoadMore"

    /// The button title.
    static var loadMore: String { MonarchStrings.string("monarch.loadMore.loadMore", table: table) }
    /// The button title while a page loads.
    static var loading: String { MonarchStrings.string("monarch.loadMore.loading", table: table) }
    /// Shown when a page fails.
    static var failed: String { MonarchStrings.string("monarch.loadMore.failed", table: table) }
    /// The retry button title.
    static var retry: String { MonarchStrings.string("monarch.loadMore.retry", table: table) }
    /// Shown when everything is loaded.
    static var end: String { MonarchStrings.string("monarch.loadMore.end", table: table) }

    /// "Showing 40 items" or "Showing 40 of 120 items", with the locale's
    /// plural rules (`Localizable.stringsdict`).
    static func count(_ loaded: Int, total: Int?) -> String {
        if let total {
            return MonarchStrings.plural("monarch.loadMore.countOfTotal", loaded, total)
        }
        return MonarchStrings.plural("monarch.loadMore.count", loaded)
    }

    /// "20 more items loaded.", with the locale's plural rules.
    static func loaded(_ added: Int) -> String {
        MonarchStrings.plural("monarch.loadMore.loaded", added)
    }
}
