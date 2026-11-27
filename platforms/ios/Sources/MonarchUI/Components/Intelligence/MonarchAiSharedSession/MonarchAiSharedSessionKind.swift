/// Whether a participant of a ``MonarchAiSharedSession`` is a person or an AI agent.
@_spi(Experimental) public enum MonarchAiSharedSessionKind: String, Sendable, Hashable, Codable, CaseIterable {
    /// A person: a round avatar, a role, and a place in the people group.
    case person
    /// An AI agent: the square agent avatar and a place in the agents group.
    case agent
}
