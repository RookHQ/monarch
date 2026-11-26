/// Replacements for the built-in strings of ``MonarchFileList``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchFileListLabels: Sendable {
    /// The label of the select-all checkbox. Built in: "Select all files".
    public var selectAll: String?
    /// The title shown when there are no files. Built in: "No files yet".
    public var empty: String?
    /// Replacements for the strings of each file card.
    public var card: MonarchFileCardLabels
    /// Replacements for the strings of the bulk action bar.
    public var bulkActionBar: MonarchBulkActionBarLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        selectAll: String? = nil,
        empty: String? = nil,
        card: MonarchFileCardLabels = MonarchFileCardLabels(),
        bulkActionBar: MonarchBulkActionBarLabels = MonarchBulkActionBarLabels()
    ) {
        self.selectAll = selectAll
        self.empty = empty
        self.card = card
        self.bulkActionBar = bulkActionBar
    }
}
