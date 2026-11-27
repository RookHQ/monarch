/// How a ``MonarchAiMultiAgentRoster`` lays out its agents.
@_spi(Experimental) public enum MonarchAiMultiAgentRosterVariant: Sendable, Hashable, CaseIterable {
    /// A row per agent with its role, task and state. The default.
    case list
    /// Overlapping agent avatars beside the summary, for headers and toolbars.
    case stack
}
