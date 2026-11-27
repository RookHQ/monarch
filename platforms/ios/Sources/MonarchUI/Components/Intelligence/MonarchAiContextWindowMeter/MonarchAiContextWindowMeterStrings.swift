/// Built-in strings of the context window meter, from the `MonarchAiContextWindowMeter` strings table.
enum MonarchAiContextWindowMeterStrings {
    private static let table = "MonarchAiContextWindowMeter"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiContextWindowMeter." + key, table: table)
    }

    /// The heading and the spoken name of the bar.
    static var label: String { string("label") }
    /// The summary line.
    static func amount(_ used: String, _ limit: String, _ percent: String) -> String {
        MonarchStrings.string("monarch.aiContextWindowMeter.amount", table: table, used, limit, percent)
    }
    /// The summary of an estimate.
    static func approximate(_ amount: String) -> String {
        MonarchStrings.string("monarch.aiContextWindowMeter.approximate", table: table, amount)
    }
    /// The spoken name of the legend.
    static var legend: String { string("legend") }
    /// The legend entry for uncovered tokens.
    static var other: String { string("other") }
    /// A legend entry's amount.
    static func tokens(_ formatted: String) -> String {
        MonarchStrings.string("monarch.aiContextWindowMeter.tokens", table: table, formatted)
    }
    /// The notice near the limit.
    static var warning: String { string("warning") }
    /// The notice when full.
    static var full: String { string("full") }
    /// The summarize button.
    static var summarize: String { string("summarize") }
    /// The hint of the compact meter.
    static var showDetails: String { string("showDetails") }
}
