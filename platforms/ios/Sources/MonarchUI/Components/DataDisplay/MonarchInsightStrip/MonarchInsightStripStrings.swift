/// Built-in strings of the insight strip, from the `MonarchInsightStrip`
/// strings table.
enum MonarchInsightStripStrings {
    private static let table = "MonarchInsightStrip"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.insightStrip." + key, table: table)
    }

    /// The title of the button that reveals the hidden insights.
    static func showMore(_ hidden: Int) -> String {
        MonarchStrings.string("monarch.insightStrip.showMore", table: table, hidden)
    }

    /// The title of the button that hides them again.
    static var showLess: String { string("showLess") }
    /// Shown when there are no insights.
    static var empty: String { string("empty") }
    /// The spoken name of the loading placeholder.
    static var loading: String { string("loading") }
    /// The title of the retry button.
    static var retry: String { string("retry") }
    /// The spoken name of the dismiss button.
    static var dismiss: String { string("dismiss") }
    /// The label of the evidence section.
    static var evidence: String { string("evidence") }
    /// The visible label of a rule-based insight.
    static var ruleOrigin: String { string("origin.rule") }
    /// The visible label of an insight from a model.
    static var aiOrigin: String { string("origin.ai") }
}
