/// Built-in strings of the confidence meter, from the `MonarchAiConfidenceMeter` strings table.
enum MonarchAiConfidenceMeterStrings {
    private static let table = "MonarchAiConfidenceMeter"

    /// The visible label of a band.
    static func label(_ level: MonarchAiConfidenceLevel) -> String {
        switch level {
        case .low: MonarchStrings.string("monarch.aiConfidenceMeter.low", table: table)
        case .medium: MonarchStrings.string("monarch.aiConfidenceMeter.medium", table: table)
        case .high: MonarchStrings.string("monarch.aiConfidenceMeter.high", table: table)
        }
    }

    /// The spoken name of the bar.
    static var name: String { MonarchStrings.string("monarch.aiConfidenceMeter.name", table: table) }

    /// The spoken value: the band label with the percentage.
    static func valueText(_ label: String, _ percent: String) -> String {
        MonarchStrings.string("monarch.aiConfidenceMeter.valueText", table: table, label, percent)
    }
}
