/// Replacements for the built-in strings of ``MonarchAiErrorMessage``.
///
/// Every property is `nil` by default, which keeps the built-in string. The
/// title and the message are parameters of the component itself.
public struct MonarchAiErrorMessageLabels: Sendable {
    /// The spoken name of the tone, "Warning" or "Error", read before the title.
    public var tone: String?
    /// The title of the retry action. Built in: "Try again".
    public var retry: String?
    /// The title of the retry action while it counts down, from the seconds left. Built in: "Try again in 12s".
    public var retryIn: (@Sendable (Int) -> String)?
    /// Announced when the countdown ends. Built in: "You can try again now."
    public var retryReady: String?
    /// The title of the action that puts the failed prompt back in the input. Built in: "Edit message".
    public var editPrompt: String?
    /// The title of the action that changes the model. Built in: "Switch model".
    public var switchModel: String?
    /// The title of the action that starts a new conversation. Built in: "New conversation".
    public var newConversation: String?
    /// The title of the action offered when the conversation is too long. Built in: "Summarize and continue".
    public var summarize: String?
    /// The title of the disclosure with technical details. Built in: "Technical details".
    public var details: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        tone: String? = nil,
        retry: String? = nil,
        retryIn: (@Sendable (Int) -> String)? = nil,
        retryReady: String? = nil,
        editPrompt: String? = nil,
        switchModel: String? = nil,
        newConversation: String? = nil,
        summarize: String? = nil,
        details: String? = nil
    ) {
        self.tone = tone
        self.retry = retry
        self.retryIn = retryIn
        self.retryReady = retryReady
        self.editPrompt = editPrompt
        self.switchModel = switchModel
        self.newConversation = newConversation
        self.summarize = summarize
        self.details = details
    }
}
