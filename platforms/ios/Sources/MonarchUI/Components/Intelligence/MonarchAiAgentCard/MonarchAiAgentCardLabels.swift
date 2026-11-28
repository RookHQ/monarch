/// Replacements for the built-in strings of ``MonarchAiAgentCard``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiAgentCardLabels: Sendable {
    /// Shown after the role, so the card never reads as a person's profile. Built in: "AI agent".
    public var kind: String?
    /// The heading of the capabilities. Built in: "What it can do".
    public var capabilities: String?
    /// The heading of the tools. Built in: "Tools".
    public var tools: String?
    /// The heading of the data access list. Built in: "Data it can see".
    public var dataAccess: String?
    /// The label of the owner. Built in: "Owner".
    public var owner: String?
    /// The tool count of the compact variant. Built in: "1 tool", "3 tools".
    public var toolCount: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        kind: String? = nil,
        capabilities: String? = nil,
        tools: String? = nil,
        dataAccess: String? = nil,
        owner: String? = nil,
        toolCount: (@Sendable (Int) -> String)? = nil
    ) {
        self.kind = kind
        self.capabilities = capabilities
        self.tools = tools
        self.dataAccess = dataAccess
        self.owner = owner
        self.toolCount = toolCount
    }
}
