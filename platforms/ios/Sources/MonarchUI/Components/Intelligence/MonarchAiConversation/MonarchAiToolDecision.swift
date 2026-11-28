/// What a tool policy decides about a tool call before anyone is asked.
public enum MonarchAiToolDecision: Sendable, Hashable, CaseIterable {
    /// Ask the person: the conversation waits in `awaitingApproval`.
    case ask
    /// Approve without asking, for example for a tool the person chose to always allow.
    case allow
    /// Deny without asking.
    case deny
}
