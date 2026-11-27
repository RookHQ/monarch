/// How much a ``MonarchAiAgentCard`` shows.
@_spi(Experimental) public enum MonarchAiAgentCardVariant: Sendable, Hashable, CaseIterable {
    /// Every section: capabilities, tools, data access, owner and actions.
    case full
    /// One summary: the avatar, name, role, description, capabilities and a tool count, for lists and pickers.
    case compact
}
