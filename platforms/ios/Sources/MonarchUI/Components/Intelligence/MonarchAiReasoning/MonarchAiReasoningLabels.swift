/// Replacements for the built-in strings of ``MonarchAiReasoning``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiReasoningLabels: Sendable {
    /// Spoken before the summary, naming the region. Built in: "Assistant reasoning".
    public var prefix: String?
    /// The summary while the model reasons. Built in: "Thinking…".
    public var thinking: String?
    /// The summary once done, from the formatted duration. Built in: "Thought for 12s".
    public var thoughtFor: (@Sendable (String) -> String)?
    /// The summary once done when no duration is known. Built in: "Thought".
    public var thought: String?
    /// The summary of reasoning whose text is withheld. Built in: "Reasoning hidden".
    public var reasoningHidden: String?
    /// Formats a duration in whole seconds, for the summary and the live timer.
    /// Built in: "12s", "1m 5s" in the environment's locale.
    public var duration: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        prefix: String? = nil,
        thinking: String? = nil,
        thoughtFor: (@Sendable (String) -> String)? = nil,
        thought: String? = nil,
        reasoningHidden: String? = nil,
        duration: (@Sendable (Int) -> String)? = nil
    ) {
        self.prefix = prefix
        self.thinking = thinking
        self.thoughtFor = thoughtFor
        self.thought = thought
        self.reasoningHidden = reasoningHidden
        self.duration = duration
    }
}
