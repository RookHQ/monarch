/// Built-in strings of the welcome, from the `MonarchAiWelcome` strings table.
enum MonarchAiWelcomeStrings {
    private static let table = "MonarchAiWelcome"

    /// The spoken name of the capability list.
    static var capabilities: String { MonarchStrings.string("monarch.aiWelcome.capabilities", table: table) }

    /// Read before the agent's name.
    static var agentPrefix: String { MonarchStrings.string("monarch.aiWelcome.agentPrefix", table: table) }

    /// The spoken agent line: the prefix, then the name and role.
    static func agentLabel(prefix: String, name: String) -> String {
        MonarchStrings.string("monarch.aiWelcome.agentLabel", table: table, prefix, name)
    }
}
