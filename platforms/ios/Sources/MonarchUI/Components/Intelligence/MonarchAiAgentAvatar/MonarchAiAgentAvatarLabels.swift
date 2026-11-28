/// Replacements for the built-in strings of ``MonarchAiAgentAvatar``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiAgentAvatarLabels: Sendable {
    /// Read after the agent's name, so an agent is never taken for a person. Built in: "AI agent".
    public var kind: String?
    /// The name of a state, read after the kind and shown with `showsState`.
    /// Built in: "Idle", "Working", "Waiting for you", "Error".
    public var state: (@Sendable (MonarchAiAgentState) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - kind: Read after the agent's name.
    ///   - state: The name of a state.
    public init(kind: String? = nil, state: (@Sendable (MonarchAiAgentState) -> String)? = nil) {
        self.kind = kind
        self.state = state
    }
}
