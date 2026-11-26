/// Built-in strings of ``MonarchAiMarkdown``, from the `MonarchAiMarkdown` strings table.
enum MonarchAiMarkdownStrings {
    private static let tableName = "MonarchAiMarkdown"

    /// The spoken name of a table.
    static var table: String { MonarchStrings.string("monarch.aiMarkdown.table", table: tableName) }
    /// The spoken size of a table.
    static func tableSize(rows: Int, columns: Int) -> String {
        MonarchStrings.string("monarch.aiMarkdown.tableSize", table: tableName, rows, columns)
    }
    /// The heading of the footnotes.
    static var footnotes: String { MonarchStrings.string("monarch.aiMarkdown.footnotes", table: tableName) }
    /// The spoken name of a task check mark.
    static var task: String { MonarchStrings.string("monarch.aiMarkdown.task", table: tableName) }
    /// The host shown after a link.
    static func linkHost(_ host: String) -> String {
        MonarchStrings.string("monarch.aiMarkdown.linkHost", table: tableName, host)
    }
}
