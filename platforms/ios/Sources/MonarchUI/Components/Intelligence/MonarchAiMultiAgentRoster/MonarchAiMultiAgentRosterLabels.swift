/// Replacements for the built-in strings of ``MonarchAiMultiAgentRoster``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiMultiAgentRosterLabels: Sendable {
    /// Names the list or the stack. Built in: "Agents".
    public var label: String?
    /// Read after each agent's name. Built in: "AI agent".
    public var agentKind: String?
    /// The name of a state. Built in: the agent avatar's "Idle", "Working",
    /// "Waiting for you", "Error".
    public var state: (@Sendable (MonarchAiAgentState) -> String)?
    /// The summary line from the total and the count of each state.
    /// Built in: "3 agents · 2 working · 1 waiting for you".
    public var summary: (@Sendable (Int, [MonarchAiAgentState: Int]) -> String)?
    /// Announced when an agent's state changes, from its name and the state
    /// name. Built in: "Research agent: Error".
    public var changed: (@Sendable (String, String) -> String)?
    /// Shown when there are no agents. Built in: "No agents are working on this."
    public var empty: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - label: Names the list or the stack.
    ///   - agentKind: Read after each agent's name.
    ///   - state: The name of a state.
    ///   - summary: The summary line.
    ///   - changed: The announcement of a state change.
    ///   - empty: Shown when there are no agents.
    public init(
        label: String? = nil,
        agentKind: String? = nil,
        state: (@Sendable (MonarchAiAgentState) -> String)? = nil,
        summary: (@Sendable (Int, [MonarchAiAgentState: Int]) -> String)? = nil,
        changed: (@Sendable (String, String) -> String)? = nil,
        empty: String? = nil
    ) {
        self.label = label
        self.agentKind = agentKind
        self.state = state
        self.summary = summary
        self.changed = changed
        self.empty = empty
    }
}
