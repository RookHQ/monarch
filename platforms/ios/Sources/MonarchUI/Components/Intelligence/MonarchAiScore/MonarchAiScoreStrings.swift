/// Built-in strings of the AI score, from the `MonarchAiScore` strings table.
enum MonarchAiScoreStrings {
    private static let table = "MonarchAiScore"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiScore.\(key)", table: table)
    }

    /// What is scored when no label is given.
    static var label: String { string("label") }
    /// The AI provenance mark.
    static var aiGenerated: String { string("aiGenerated") }
    /// Shown while there is no score.
    static var unavailable: String { string("unavailable") }
    /// The heading of the factors.
    static var factors: String { string("factors") }
    /// The heading of the factor column.
    static var factorColumn: String { string("factorColumn") }
    /// The heading of the impact column.
    static var impactColumn: String { string("impactColumn") }
    /// The spoken effect of a factor with no impact.
    static var noImpact: String { string("noImpact") }
    /// The summary of the band legend.
    static var bands: String { string("bands") }
    /// Marks the current band in the legend.
    static var currentBand: String { string("currentBand") }
    /// The name of the low default band.
    static var low: String { string("band.low") }
    /// The name of the medium default band.
    static var medium: String { string("band.medium") }
    /// The name of the high default band.
    static var high: String { string("band.high") }

    /// The spoken value.
    static func valueText(_ value: String, max: String) -> String {
        MonarchStrings.string("monarch.aiScore.valueText", table: table, value, max)
    }

    /// The spoken effect of a factor.
    static func impact(_ amount: String, raised: Bool) -> String {
        MonarchStrings.string(raised ? "monarch.aiScore.raised" : "monarch.aiScore.lowered", table: table, amount)
    }
}
