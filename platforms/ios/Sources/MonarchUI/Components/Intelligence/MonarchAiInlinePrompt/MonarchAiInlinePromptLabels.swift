/// Replacements for the built-in strings of ``MonarchAiInlinePrompt``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchAiInlinePromptLabels: Sendable, Hashable {
    /// The sheet's title. Built in: "Ask AI".
    public var label: String?
    /// The field's placeholder. Built in: "Ask AI to edit or write…".
    public var placeholder: String?
    /// The field's spoken name. Built in: "Prompt".
    public var inputLabel: String?
    /// The Send button. Built in: "Send".
    public var submit: String?
    /// The label above the excerpt of the selection. Built in: "Selection".
    public var context: String?
    /// The spoken name of the quick prompts. Built in: "Suggestions".
    public var suggestions: String?
    /// Shown and announced while sending. Built in: "Sending…".
    public var pending: String?
    /// The button that closes the sheet. Built in: "Cancel".
    public var cancel: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        placeholder: String? = nil,
        inputLabel: String? = nil,
        submit: String? = nil,
        context: String? = nil,
        suggestions: String? = nil,
        pending: String? = nil,
        cancel: String? = nil
    ) {
        self.label = label
        self.placeholder = placeholder
        self.inputLabel = inputLabel
        self.submit = submit
        self.context = context
        self.suggestions = suggestions
        self.pending = pending
        self.cancel = cancel
    }
}
