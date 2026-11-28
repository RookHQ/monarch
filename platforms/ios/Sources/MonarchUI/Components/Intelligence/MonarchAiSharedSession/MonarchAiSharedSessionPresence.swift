/// How present a participant of a ``MonarchAiSharedSession`` is.
@_spi(Experimental) public enum MonarchAiSharedSessionPresence: String, Sendable, Hashable, Codable, CaseIterable {
    /// Here and engaged.
    case active
    /// Here but quiet.
    case idle
    /// Stepped away.
    case away
}
