/// Built-in strings of the reasoning disclosure, from the `MonarchAiReasoning` strings table.
enum MonarchAiReasoningStrings {
    private static let table = "MonarchAiReasoning"

    /// Spoken before the summary.
    static var prefix: String { MonarchStrings.string("monarch.aiReasoning.prefix", table: table) }

    /// The summary while the model reasons.
    static var thinking: String { MonarchStrings.string("monarch.aiReasoning.thinking", table: table) }

    /// The summary once done, with a duration.
    static func thoughtFor(_ duration: String) -> String {
        MonarchStrings.string("monarch.aiReasoning.thoughtFor", table: table, duration)
    }

    /// The summary once done, without a duration.
    static var thought: String { MonarchStrings.string("monarch.aiReasoning.thought", table: table) }

    /// The summary of withheld reasoning.
    static var reasoningHidden: String { MonarchStrings.string("monarch.aiReasoning.reasoningHidden", table: table) }
}
