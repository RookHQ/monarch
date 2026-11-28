/// What an event in an agent run was.
public enum MonarchAiRunEventKind: String, Sendable, Hashable, CaseIterable, Codable {
    /// A call to a model.
    case model
    /// A call to a tool.
    case tool
    /// A hand-off from one agent to another.
    case handoff
    /// An error.
    case error
}
