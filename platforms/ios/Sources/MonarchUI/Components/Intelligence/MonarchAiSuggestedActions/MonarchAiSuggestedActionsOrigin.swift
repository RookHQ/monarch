/// Where the suggestions of ``MonarchAiSuggestedActions`` came from. It is always shown.
@_spi(Experimental) public enum MonarchAiSuggestedActionsOrigin: Sendable, Hashable, CaseIterable {
    /// An AI model suggested them: "Suggested by AI" in the assistant colours.
    case ai
    /// Rules the product defines suggested them: "Rule-based" in neutral colours.
    case rule
}
