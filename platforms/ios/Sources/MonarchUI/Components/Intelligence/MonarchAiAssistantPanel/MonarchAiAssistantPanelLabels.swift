/// Replacements for the built-in strings of ``MonarchAiAssistantPanel``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiAssistantPanelLabels: Sendable {
    /// The close button. Built in: "Close assistant".
    public var close: String?
    /// The name of the prompt input. Built in: "Message the assistant".
    public var inputLabel: String?
    /// The name of the context chips. Built in: "Context".
    public var context: String?
    /// The greeting of the empty panel. Built in: "How can I help?".
    public var welcome: String?
    /// The name of the starter prompts. Built in: "Suggested prompts".
    public var starters: String?
    /// The name of the assistant when no agent is given. Built in: "Assistant".
    public var assistant: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        close: String? = nil,
        inputLabel: String? = nil,
        context: String? = nil,
        welcome: String? = nil,
        starters: String? = nil,
        assistant: String? = nil
    ) {
        self.close = close
        self.inputLabel = inputLabel
        self.context = context
        self.welcome = welcome
        self.starters = starters
        self.assistant = assistant
    }
}
