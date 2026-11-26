/// What a person in a ``MonarchAiSharedSession`` may do.
@_spi(Experimental) public enum MonarchAiSharedSessionRole: String, Sendable, Hashable, Codable, CaseIterable {
    /// Sends requests and manages the queue.
    case owner
    /// Sends requests. The default.
    case editor
    /// Only watches.
    case viewer
}
