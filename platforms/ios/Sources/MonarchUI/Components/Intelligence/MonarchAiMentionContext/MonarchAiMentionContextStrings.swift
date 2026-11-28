/// Built-in strings of the mention context menu, from the `MonarchAiMentionContext` strings table.
enum MonarchAiMentionContextStrings {
    private static let table = "MonarchAiMentionContext"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiMentionContext." + key, table: table)
    }

    /// Names the list.
    static var label: String { string("label") }
    /// Read while searching.
    static var loading: String { string("loading") }
    /// Shown when the search failed.
    static var error: String { string("error") }
    /// Shown for an empty query with no results.
    static var typeToSearch: String { string("typeToSearch") }
    /// The heading of a kind.
    static func group(_ kind: MonarchAiMentionKind) -> String { string("group." + kind.rawValue) }
    /// "Nothing matches “report”".
    static func noMatch(_ query: String) -> String {
        MonarchStrings.string("monarch.aiMentionContext.noMatch", table: table, query)
    }
    /// "1 result" or "4 results".
    static func count(_ count: Int) -> String {
        let key = count == 1 ? "monarch.aiMentionContext.countOne" : "monarch.aiMentionContext.count"
        return MonarchStrings.string(key, table: table, count)
    }
}
