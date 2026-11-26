/// Built-in strings of the search results, from the `MonarchSearchResults` strings table.
enum MonarchSearchResultsStrings {
    private static let table = "MonarchSearchResults"

    private static func key(_ name: String) -> String { "monarch.searchResults.\(name)" }

    private static func plural(_ name: String, _ count: Int) -> String {
        key(name + (count == 1 ? ".one" : ".other"))
    }

    /// "12 results".
    static func count(_ count: Int) -> String {
        MonarchStrings.string(plural("count", count), table: table, count)
    }

    /// "12 results for “travel”".
    static func countForQuery(_ count: Int, query: String) -> String {
        MonarchStrings.string(plural("countForQuery", count), table: table, count, query)
    }

    /// "Searching…".
    static var loading: String { MonarchStrings.string(key("loading"), table: table) }
    /// "No results".
    static var emptyTitle: String { MonarchStrings.string(key("emptyTitle"), table: table) }

    /// "No results for “travel”".
    static func emptyTitle(query: String) -> String {
        MonarchStrings.string(key("emptyTitleForQuery"), table: table, query)
    }

    /// The empty state's message.
    static var emptyMessage: String { MonarchStrings.string(key("emptyMessage"), table: table) }
    /// "Search failed".
    static var errorTitle: String { MonarchStrings.string(key("errorTitle"), table: table) }

    /// "Show all 24 in Documents".
    static func showAll(group: String, total: Int) -> String {
        MonarchStrings.string(key("showAll"), table: table, total, group)
    }

    /// "Show all 24".
    static func showAll(total: Int) -> String {
        MonarchStrings.string(key("showAllUngrouped"), table: table, total)
    }
}
