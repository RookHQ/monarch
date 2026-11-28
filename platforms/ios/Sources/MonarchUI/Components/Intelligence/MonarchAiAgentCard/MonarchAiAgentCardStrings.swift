/// Built-in strings of the agent card, from the `MonarchAiAgentCard` strings table.
enum MonarchAiAgentCardStrings {
    private static let table = "MonarchAiAgentCard"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiAgentCard.\(key)", table: table)
    }

    /// The heading of the capabilities.
    static var capabilities: String { string("capabilities") }
    /// The heading of the tools.
    static var tools: String { string("tools") }
    /// The heading of the data access list.
    static var dataAccess: String { string("dataAccess") }
    /// The label of the owner.
    static var owner: String { string("owner") }
    /// The tool count of the compact variant.
    static func toolCount(_ count: Int) -> String {
        count == 1
            ? string("toolCountOne")
            : MonarchStrings.string("monarch.aiAgentCard.toolCountOther", table: table, count)
    }
}
