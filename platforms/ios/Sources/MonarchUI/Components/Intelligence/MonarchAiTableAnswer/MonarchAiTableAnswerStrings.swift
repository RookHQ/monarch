/// Built-in strings of the AI table, from the `MonarchAiTableAnswer` strings table.
enum MonarchAiTableAnswerStrings {
    private static let table = "MonarchAiTableAnswer"

    /// The provenance label.
    static var aiTable: String { MonarchStrings.string("monarch.aiTableAnswer.aiTable", table: table) }
    /// The row count.
    static func rowCount(_ count: Int) -> String {
        count == 1
            ? MonarchStrings.string("monarch.aiTableAnswer.rowCount.one", table: table)
            : MonarchStrings.string("monarch.aiTableAnswer.rowCount.other", table: table, count)
    }
    /// The copy button.
    static var copyCSV: String { MonarchStrings.string("monarch.aiTableAnswer.copyCSV", table: table) }
    /// The copy button after a copy.
    static var copied: String { MonarchStrings.string("monarch.aiTableAnswer.copied", table: table) }
    /// The open-as-artifact button.
    static var openAsArtifact: String { MonarchStrings.string("monarch.aiTableAnswer.openAsArtifact", table: table) }
    /// Announced after a sort.
    static func sorted(_ column: String, ascending: Bool) -> String {
        MonarchStrings.string(
            ascending ? "monarch.aiTableAnswer.sorted.ascending" : "monarch.aiTableAnswer.sorted.descending",
            table: table,
            column
        )
    }
    /// The spoken value of a sorted header.
    static func sortValue(ascending: Bool) -> String {
        MonarchStrings.string(
            ascending ? "monarch.aiTableAnswer.sortValue.ascending" : "monarch.aiTableAnswer.sortValue.descending",
            table: table
        )
    }
    /// Read for an empty cell.
    static var empty: String { MonarchStrings.string("monarch.aiTableAnswer.empty", table: table) }
    /// Shown when there are no rows.
    static var noRows: String { MonarchStrings.string("monarch.aiTableAnswer.noRows", table: table) }
    /// Read for the streaming placeholder.
    static var loadingRows: String { MonarchStrings.string("monarch.aiTableAnswer.loadingRows", table: table) }
    /// Announced when streaming ends.
    static var complete: String { MonarchStrings.string("monarch.aiTableAnswer.complete", table: table) }
    /// The button that shows every row.
    static func showAllRows(_ count: Int) -> String {
        MonarchStrings.string("monarch.aiTableAnswer.showAllRows", table: table, count)
    }
}
