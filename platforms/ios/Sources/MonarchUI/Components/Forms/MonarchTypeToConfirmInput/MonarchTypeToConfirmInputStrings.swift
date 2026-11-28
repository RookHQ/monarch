/// Built-in strings of the type-to-confirm input, from the `MonarchTypeToConfirmInput` strings table.
enum MonarchTypeToConfirmInputStrings {
    /// The default label, which names the phrase to type.
    static func label(_ phrase: String) -> String {
        MonarchStrings.string("monarch.typeToConfirmInput.label", table: "MonarchTypeToConfirmInput", phrase)
    }

    /// The default error while the text does not match.
    static var mismatch: String {
        MonarchStrings.string("monarch.typeToConfirmInput.mismatch", table: "MonarchTypeToConfirmInput")
    }

    /// Announced when the text starts to match.
    static var matches: String {
        MonarchStrings.string("monarch.typeToConfirmInput.matches", table: "MonarchTypeToConfirmInput")
    }
}
