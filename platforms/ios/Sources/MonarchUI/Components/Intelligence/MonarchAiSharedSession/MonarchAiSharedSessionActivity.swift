/// What a participant of a ``MonarchAiSharedSession`` is doing right now.
@_spi(Experimental) public enum MonarchAiSharedSessionActivity: String, Sendable, Hashable, Codable, CaseIterable {
    /// Typing a request.
    case typing
    /// Speaking in voice mode.
    case speaking
    /// Reviewing an answer or an edit.
    case reviewing
}
