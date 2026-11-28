/// Built-in strings of ``MonarchPreviewRows``, from the `MonarchPreviewRows` strings table.
enum MonarchPreviewRowsStrings {
    private static let table = "MonarchPreviewRows"

    /// The spoken name of the filter tabs.
    static var filter: String { MonarchStrings.string("monarch.previewRows.filter", table: table) }
    /// The tab with every row.
    static var all: String { MonarchStrings.string("monarch.previewRows.all", table: table) }
    /// The ready status.
    static var ready: String { MonarchStrings.string("monarch.previewRows.ready", table: table) }
    /// The warning status.
    static var warning: String { MonarchStrings.string("monarch.previewRows.warning", table: table) }
    /// The error status.
    static var error: String { MonarchStrings.string("monarch.previewRows.error", table: table) }
    /// The duplicate status.
    static var duplicate: String { MonarchStrings.string("monarch.previewRows.duplicate", table: table) }
    /// The label of the result cell of a row.
    static var result: String { MonarchStrings.string("monarch.previewRows.result", table: table) }
    /// The label of the issues cell of a row.
    static var issues: String { MonarchStrings.string("monarch.previewRows.issues", table: table) }
    /// "Row 4".
    static func row(_ number: Int) -> String {
        MonarchStrings.string("monarch.previewRows.row", table: table, number)
    }
    /// Read for a missing value.
    static var emptyValue: String { MonarchStrings.string("monarch.previewRows.emptyValue", table: table) }
    /// The title when the file has no rows.
    static var emptyTitle: String { MonarchStrings.string("monarch.previewRows.emptyTitle", table: table) }
    /// The message when the file has no rows.
    static var emptyDescription: String {
        MonarchStrings.string("monarch.previewRows.emptyDescription", table: table)
    }
    /// The title when the filter matches no row.
    static var filteredTitle: String { MonarchStrings.string("monarch.previewRows.filteredTitle", table: table) }
}
