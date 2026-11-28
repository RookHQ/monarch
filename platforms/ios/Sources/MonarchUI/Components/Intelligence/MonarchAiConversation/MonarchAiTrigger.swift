/// Why the conversation is asking the transport for a turn.
public enum MonarchAiTrigger: String, Sendable, Hashable, CaseIterable {
    /// A new user message.
    case send
    /// The reply is asked for again.
    case regenerate
    /// A user message was edited and resent.
    case edit
    /// The reply continues after the client sent tool results back.
    case toolResult = "tool-result"
    /// Reconnecting to a run that was already going.
    case resume
}
