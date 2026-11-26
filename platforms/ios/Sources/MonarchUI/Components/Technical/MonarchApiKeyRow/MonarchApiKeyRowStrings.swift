/// Built-in strings of the API key row, from the `MonarchApiKeyRow` strings table.
enum MonarchApiKeyRowStrings {
    private static let table = "MonarchApiKeyRow"

    /// The string for a key, without the `monarch.apiKeyRow.` prefix.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.apiKeyRow.\(key)", table: table)
    }

    /// The name of a status.
    static func status(_ status: MonarchApiKeyStatus) -> String {
        string("status.\(status.rawValue)")
    }

    /// "+2", the badge for scopes beyond the visible ones.
    static func moreScopes(_ count: Int) -> String {
        MonarchStrings.string("monarch.apiKeyRow.moreScopes", table: table, count)
    }
}
