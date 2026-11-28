/// One agent ``MonarchAiAgentPicker`` offers.
@_spi(Experimental) public struct MonarchAiAgentPickerOption: Identifiable, Sendable {
    /// The agent: name, role, codename and avatar.
    public let agent: MonarchAiAgentRef
    /// One line on what the agent is for.
    public let description: String?
    /// Short capability phrases, shown as tags.
    public let capabilities: [String]
    /// The agent's tools; the option shows how many.
    public let tools: [MonarchAiAgentTool]
    /// What the agent is doing now, shown on its avatar.
    public let state: MonarchAiAgentState
    /// Whether the agent cannot be chosen now, for example because it is not enabled in this space.
    public let isDisabled: Bool

    /// The option's value: the agent's id, or its name when it has none.
    public var id: String { MonarchAiAgentPicker.key(for: agent) }

    /// An agent option.
    ///
    /// - Parameters:
    ///   - agent: The agent.
    ///   - description: One line on what the agent is for.
    ///   - capabilities: Short capability phrases, shown as tags.
    ///   - tools: The agent's tools; the option shows how many.
    ///   - state: What the agent is doing now.
    ///   - isDisabled: Whether the agent cannot be chosen now.
    public init(
        _ agent: MonarchAiAgentRef,
        description: String? = nil,
        capabilities: [String] = [],
        tools: [MonarchAiAgentTool] = [],
        state: MonarchAiAgentState = .idle,
        isDisabled: Bool = false
    ) {
        self.agent = agent
        self.description = description
        self.capabilities = capabilities
        self.tools = tools
        self.state = state
        self.isDisabled = isDisabled
    }
}
