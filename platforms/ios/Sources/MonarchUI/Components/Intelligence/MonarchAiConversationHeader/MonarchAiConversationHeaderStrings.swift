/// Built-in strings of the conversation header, from the `MonarchAiConversationHeader` strings table.
enum MonarchAiConversationHeaderStrings {
    private static let table = "MonarchAiConversationHeader"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiConversationHeader.\(key)", table: table)
    }

    /// Shown when the title is empty.
    static var untitled: String { string("untitled") }
    /// The rename menu item and prompt title.
    static var rename: String { string("rename") }
    /// The title field.
    static var titleLabel: String { string("titleLabel") }
    /// Saves a rename.
    static var save: String { string("save") }
    /// Cancels a rename.
    static var cancel: String { string("cancel") }
    /// Shown while the title is generated.
    static var naming: String { string("naming") }
    /// Read before the agent's name.
    static var agentPrefix: String { string("agentPrefix") }
    /// Read before the model.
    static var modelPrefix: String { string("modelPrefix") }
    /// Shown when a rename fails.
    static var renameFailed: String { string("renameFailed") }
    /// The hint of the title menu.
    static var menuHint: String { string("menuHint") }
}
