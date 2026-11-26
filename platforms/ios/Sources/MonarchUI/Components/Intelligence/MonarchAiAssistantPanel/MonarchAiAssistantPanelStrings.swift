/// Built-in strings of the assistant panel, from the `MonarchAiAssistantPanel` strings table.
enum MonarchAiAssistantPanelStrings {
    private static let table = "MonarchAiAssistantPanel"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiAssistantPanel.\(key)", table: table)
    }

    /// The collapse and expand button.
    static var collapse: String { string("collapse") }
    static var expand: String { string("expand") }
    /// The close button.
    static var close: String { string("close") }
    /// The prompt input.
    static var inputLabel: String { string("inputLabel") }
    /// The context chips.
    static var context: String { string("context") }
    /// The greeting of the empty panel.
    static var welcome: String { string("welcome") }
    /// The starter prompts.
    static var starters: String { string("starters") }
    /// The assistant without an agent.
    static var assistant: String { string("assistant") }
}
