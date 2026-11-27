/// Built-in strings of the mode switcher, from the `MonarchAiModeSwitcher` strings table.
enum MonarchAiModeSwitcherStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiModeSwitcher." + key, table: "MonarchAiModeSwitcher")
    }

    /// The name of the choice.
    static var label: String { string("label") }
    /// The name of the Ask mode.
    static var ask: String { string("ask") }
    /// What the Ask mode does.
    static var askDetail: String { string("askDetail") }
    /// The name of the Edit mode.
    static var edit: String { string("edit") }
    /// What the Edit mode does.
    static var editDetail: String { string("editDetail") }
    /// The name of the Agent mode.
    static var agent: String { string("agent") }
    /// What the Agent mode does.
    static var agentDetail: String { string("agentDetail") }
}
