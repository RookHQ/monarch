/// Built-in strings of the AI usage meter, from the `MonarchAiUsageMeter` strings table.
enum MonarchAiUsageMeterStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiUsageMeter." + key, table: "MonarchAiUsageMeter")
    }

    /// The spoken name of the meter group.
    static var label: String { string("label") }

    /// The status line of an item at or above its warning level: "This month: 87% of the limit used".
    static func limitUsed(label: String, percent: String, isOver: Bool) -> String {
        String(format: string(isOver ? "overLimit" : "limitUsed"), label, percent)
    }

    /// The unit name for a count: singular for exactly one.
    static func unit(_ unit: MonarchAiUsageUnit, count: Double) -> String {
        let one = count == 1
        return switch unit {
        case .tokens: string(one ? "token" : "tokens")
        case .credits: string(one ? "credit" : "credits")
        }
    }
}
