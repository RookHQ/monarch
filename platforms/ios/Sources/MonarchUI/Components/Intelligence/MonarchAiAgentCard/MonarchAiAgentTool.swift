/// A tool an AI agent can call, as shown to people by ``MonarchAiAgentCard``.
@_spi(Experimental) public struct MonarchAiAgentTool: Identifiable, Sendable, Hashable {
    /// The tool's name, such as "Search documents". It identifies the tool in the card.
    public let name: String
    /// What the tool does, in plain words.
    public let description: String?

    /// The identity of the tool, which is its name.
    public var id: String { name }

    /// A tool.
    ///
    /// - Parameters:
    ///   - name: The tool's name, already localised.
    ///   - description: What the tool does, in plain words.
    public init(_ name: String, description: String? = nil) {
        self.name = name
        self.description = description
    }
}
