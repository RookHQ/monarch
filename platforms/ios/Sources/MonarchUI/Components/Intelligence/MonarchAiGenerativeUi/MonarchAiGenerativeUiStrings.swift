/// Built-in strings of generative UI, from the `MonarchAiGenerativeUi` strings table.
enum MonarchAiGenerativeUiStrings {
    private static let table = "MonarchAiGenerativeUi"

    /// The notice for a rejection.
    static func notice(_ reason: MonarchAiUiRejection) -> String {
        switch reason {
        case .unknownComponent: MonarchStrings.string("monarch.aiGenerativeUi.unknown", table: table)
        case .invalidProps: MonarchStrings.string("monarch.aiGenerativeUi.invalid", table: table)
        case .renderError: MonarchStrings.string("monarch.aiGenerativeUi.failed", table: table)
        }
    }

    /// The spoken name of the progress indicator.
    static var loading: String { MonarchStrings.string("monarch.aiGenerativeUi.loading", table: table) }
}
