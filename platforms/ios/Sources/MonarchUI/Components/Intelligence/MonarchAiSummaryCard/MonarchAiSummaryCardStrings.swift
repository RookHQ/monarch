/// Built-in strings of ``MonarchAiSummaryCard``, from the `MonarchAiSummaryCard` strings table.
enum MonarchAiSummaryCardStrings {
    private static let table = "MonarchAiSummaryCard"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiSummaryCard." + key, table: table)
    }

    /// The provenance label.
    static var aiSummary: String { string("aiSummary") }
    /// The refresh button.
    static var refresh: String { string("refresh") }
    /// "Generated 3 hours ago".
    static func generated(_ time: String) -> String {
        MonarchStrings.string("monarch.aiSummaryCard.generated", table: table, time)
    }
    /// Shown and announced while generating.
    static var generating: String { string("generating") }
    /// Announced when generating ends.
    static var updated: String { string("updated") }
    /// The stale warning.
    static var stale: String { string("stale") }
    /// The error title.
    static var error: String { string("error") }
    /// The empty text.
    static var empty: String { string("empty") }
    /// The heading of the sources.
    static var sources: String { string("sources") }
}
