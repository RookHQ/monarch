/// Built-in strings of the code block, from the `MonarchCodeBlock` strings table.
enum MonarchCodeBlockStrings {
    private static let table = "MonarchCodeBlock"

    /// The title of the wrap toggle.
    static var wrap: String { MonarchStrings.string("monarch.codeBlock.wrap", table: table) }
    /// Spoken before an added line of a diff.
    static var added: String { MonarchStrings.string("monarch.codeBlock.added", table: table) }
    /// Spoken before a removed line of a diff.
    static var removed: String { MonarchStrings.string("monarch.codeBlock.removed", table: table) }
    /// The name of code in no known language.
    static var code: String { MonarchStrings.string("monarch.codeBlock.code", table: table) }

    /// The name of code in a language, such as "TypeScript code".
    static func languageCode(_ language: String) -> String {
        MonarchStrings.string("monarch.codeBlock.languageCode", table: table, language)
    }

    /// A diff line as VoiceOver reads it, such as "Added: let x = 1".
    static func diffLine(_ change: String, _ text: String) -> String {
        MonarchStrings.string("monarch.codeBlock.diffLine", table: table, change, text)
    }
}
