/// Who wrote a message.
public enum MonarchAiRole: String, Sendable, Hashable, Codable, CaseIterable {
    /// The person.
    case user
    /// The model.
    case assistant
    /// Instructions to the model.
    case system
    /// The result of a tool.
    case tool
}
