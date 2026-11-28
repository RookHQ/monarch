/// Built-in strings of the AI code block, from the `MonarchAiCodeBlock` strings table.
enum MonarchAiCodeBlockStrings {
    private static let table = "MonarchAiCodeBlock"

    /// The title of the apply button.
    static var apply: String { MonarchStrings.string("monarch.aiCodeBlock.apply", table: table) }

    /// The title of the insert button.
    static var insert: String { MonarchStrings.string("monarch.aiCodeBlock.insert", table: table) }

    /// The hint of the actions while the code is still arriving.
    static var incompleteHint: String { MonarchStrings.string("monarch.aiCodeBlock.incompleteHint", table: table) }

    /// The spoken name of an action.
    static func actionLabel(_ title: String, target: String) -> String {
        MonarchStrings.string("monarch.aiCodeBlock.actionLabel", table: table, title, target)
    }
}
