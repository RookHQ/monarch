/// Built-in strings of the snippet tabs, from the `MonarchCodeSnippetTabs` strings table.
enum MonarchCodeSnippetTabsStrings {
    private static let table = "MonarchCodeSnippetTabs"

    /// The spoken name of the tab strip.
    static var label: String { MonarchStrings.string("monarch.codeSnippetTabs.label", table: table) }
}
