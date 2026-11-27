/// Built-in strings of the AI disclaimer, from the `MonarchAiDisclaimer` strings table.
enum MonarchAiDisclaimerStrings {
    private static let table = "MonarchAiDisclaimer"

    /// The default wording for a risk.
    static func text(_ risk: MonarchAiDisclaimerRisk) -> String {
        switch risk {
        case .low: MonarchStrings.string("monarch.aiDisclaimer.low", table: table)
        case .medium: MonarchStrings.string("monarch.aiDisclaimer.medium", table: table)
        case .high: MonarchStrings.string("monarch.aiDisclaimer.high", table: table)
        }
    }
}
