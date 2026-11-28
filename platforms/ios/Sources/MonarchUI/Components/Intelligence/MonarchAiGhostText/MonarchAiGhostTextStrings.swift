/// Built-in strings of the inline completion field, from the `MonarchAiGhostText` strings table.
enum MonarchAiGhostTextStrings {
    private static let table = "MonarchAiGhostText"

    /// Announced when a suggestion appears.
    static func announce(_ suggestion: String) -> String {
        MonarchStrings.string("monarch.aiGhostText.announce", table: table, suggestion)
    }

    /// The hint of the field while a suggestion is showing.
    static var instruction: String { MonarchStrings.string("monarch.aiGhostText.instruction", table: table) }
    /// The title of the accept button and of the VoiceOver action.
    static var accept: String { MonarchStrings.string("monarch.aiGhostText.accept", table: table) }
    /// The name of the VoiceOver action that dismisses the suggestion.
    static var dismiss: String { MonarchStrings.string("monarch.aiGhostText.dismiss", table: table) }
    /// Announced when the suggestion is accepted.
    static var accepted: String { MonarchStrings.string("monarch.aiGhostText.accepted", table: table) }
    /// Announced when the suggestion is dismissed.
    static var dismissed: String { MonarchStrings.string("monarch.aiGhostText.dismissed", table: table) }
}
