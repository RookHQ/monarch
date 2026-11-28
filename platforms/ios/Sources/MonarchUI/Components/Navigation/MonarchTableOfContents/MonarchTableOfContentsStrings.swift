/// Built-in strings of the table of contents, from the `MonarchTableOfContents` strings table.
enum MonarchTableOfContentsStrings {
    private static let table = "MonarchTableOfContents"

    /// The visible title and the spoken name of the list.
    static var title: String { MonarchStrings.string("monarch.tableOfContents.title", table: table) }
    /// The value of the entry of the section being read.
    static var current: String { MonarchStrings.string("monarch.tableOfContents.current", table: table) }
}
