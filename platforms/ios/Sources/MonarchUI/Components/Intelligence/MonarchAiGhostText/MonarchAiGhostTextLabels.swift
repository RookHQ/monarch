/// Replacements for the built-in strings of ``MonarchAiGhostText``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiGhostTextLabels: Sendable {
    /// Announced when a suggestion appears, from the suggestion. Say how to accept it.
    public var announce: (@Sendable (String) -> String)?
    /// The hint of the field while a suggestion is showing.
    public var instruction: String?
    /// The title of the accept button and of the VoiceOver action. Built in: "Accept suggestion".
    public var accept: String?
    /// The name of the VoiceOver action that dismisses the suggestion. Built in: "Dismiss suggestion".
    public var dismiss: String?
    /// Announced when the suggestion is accepted. Built in: "Suggestion accepted."
    public var accepted: String?
    /// Announced when the suggestion is dismissed. Built in: "Suggestion dismissed."
    public var dismissed: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        announce: (@Sendable (String) -> String)? = nil,
        instruction: String? = nil,
        accept: String? = nil,
        dismiss: String? = nil,
        accepted: String? = nil,
        dismissed: String? = nil
    ) {
        self.announce = announce
        self.instruction = instruction
        self.accept = accept
        self.dismiss = dismiss
        self.accepted = accepted
        self.dismissed = dismissed
    }
}
