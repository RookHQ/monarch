/// Built-in strings of ``MonarchLanguageSelect``, from the `MonarchLanguageSelect` strings table.
enum MonarchLanguageSelectStrings {
    private static let table = "MonarchLanguageSelect"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.languageSelect.\(key)", table: table)
    }

    /// The heading over the suggested languages.
    static var suggested: String { string("suggested") }
    /// The heading over the other languages.
    static var all: String { string("all") }
    /// Shown when no language matches the search.
    static var empty: String { string("empty") }
}
