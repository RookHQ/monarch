/// Built-in strings of the inline prompt, from the `MonarchAiInlinePrompt` strings table.
enum MonarchAiInlinePromptStrings {
    private static let table = "MonarchAiInlinePrompt"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiInlinePrompt.\(key)", table: table)
    }

    /// The sheet's title and the text-menu item: "Ask AI".
    static var label: String { string("label") }
    /// The field's placeholder.
    static var placeholder: String { string("placeholder") }
    /// The field's spoken name.
    static var inputLabel: String { string("inputLabel") }
    /// The Send button.
    static var submit: String { string("submit") }
    /// The label above the excerpt.
    static var context: String { string("context") }
    /// The name of the quick prompts.
    static var suggestions: String { string("suggestions") }
    /// Shown while sending.
    static var pending: String { string("pending") }
    /// The Cancel button.
    static var cancel: String { string("cancel") }
    /// The keyboard hint under the field of the inline card.
    static var keyboardHint: String { string("keyboardHint") }
}
