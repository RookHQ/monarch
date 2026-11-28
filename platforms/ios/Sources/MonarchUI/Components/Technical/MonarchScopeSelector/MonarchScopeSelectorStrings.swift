/// Built-in strings of ``MonarchScopeSelector``, from the `MonarchScopeSelector` strings table.
enum MonarchScopeSelectorStrings {
    private static let table = "MonarchScopeSelector"

    /// The string for a key without placeholders, such as `selectAll`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.scopeSelector.\(key)", table: table)
    }

    /// "1 of 2 scopes".
    static func count(_ selected: Int, _ total: Int) -> String {
        MonarchStrings.string("monarch.scopeSelector.count", table: table, selected, total)
    }

    /// "Show 10 more of 25".
    static func showMore(_ next: Int, _ remaining: Int) -> String {
        MonarchStrings.string("monarch.scopeSelector.showMore", table: table, next, remaining)
    }
}
