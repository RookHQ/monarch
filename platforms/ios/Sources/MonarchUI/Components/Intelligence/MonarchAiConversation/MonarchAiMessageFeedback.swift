/// The feedback a person gave on a message.
public enum MonarchAiMessageFeedback: String, Sendable, Hashable, Codable, CaseIterable {
    /// Thumbs up.
    case up
    /// Thumbs down.
    case down
}
