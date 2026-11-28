/// One agent in a ``MonarchAiMultiAgentRoster`` and what it is doing.
@_spi(Experimental) public struct MonarchAiMultiAgentRosterMember: Identifiable, Sendable, Hashable {
    /// The agent. Its `role` is shown under the name in the list.
    public var agent: MonarchAiAgentRef
    /// What the agent is doing now.
    public var state: MonarchAiAgentState
    /// What it is working on, for example "Comparing three sources". Already localised.
    public var task: String?

    /// The agent's id, or its name when it has no id. Keep it stable between
    /// updates: changes are announced by matching it.
    public var id: String { agent.id ?? agent.name }

    /// A roster member.
    ///
    /// - Parameters:
    ///   - agent: The agent.
    ///   - state: What the agent is doing now.
    ///   - task: What it is working on.
    public init(_ agent: MonarchAiAgentRef, state: MonarchAiAgentState = .idle, task: String? = nil) {
        self.agent = agent
        self.state = state
        self.task = task
    }
}
