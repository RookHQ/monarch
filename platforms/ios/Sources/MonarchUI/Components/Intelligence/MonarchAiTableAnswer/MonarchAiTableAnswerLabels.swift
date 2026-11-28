/// Replacements for the built-in strings of ``MonarchAiTableAnswer``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiTableAnswerLabels: Sendable {
    /// The provenance label above the title. Built in: "AI table".
    public var aiTable: String?
    /// The row count, from the number of rows. Built in: "12 rows".
    public var rowCount: (@Sendable (Int) -> String)?
    /// The copy button. Built in: "Copy as CSV".
    public var copyCSV: String?
    /// The copy button after a copy. Built in: "Copied".
    public var copied: String?
    /// The button that opens the table as an artifact. Built in: "Open as artifact".
    public var openAsArtifact: String?
    /// Announced after a sort, from the column and whether it is ascending. Built in: "Sorted by Revenue, ascending."
    public var sorted: (@Sendable (String, Bool) -> String)?
    /// The spoken value of a sorted header, from whether it is ascending. Built in: "Sorted ascending".
    public var sortValue: (@Sendable (Bool) -> String)?
    /// Read for an empty cell. Built in: "Empty".
    public var empty: String?
    /// Shown when there are no rows and nothing is streaming. Built in: "No rows."
    public var noRows: String?
    /// Read for the placeholder row while rows stream in. Built in: "More rows are being generated".
    public var loadingRows: String?
    /// Announced when streaming ends. Built in: "Table complete."
    public var complete: String?
    /// The button under a long table, from the number of rows. Built in: "Show all 250 rows".
    public var showAllRows: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        aiTable: String? = nil,
        rowCount: (@Sendable (Int) -> String)? = nil,
        copyCSV: String? = nil,
        copied: String? = nil,
        openAsArtifact: String? = nil,
        sorted: (@Sendable (String, Bool) -> String)? = nil,
        sortValue: (@Sendable (Bool) -> String)? = nil,
        empty: String? = nil,
        noRows: String? = nil,
        loadingRows: String? = nil,
        complete: String? = nil,
        showAllRows: (@Sendable (Int) -> String)? = nil
    ) {
        self.aiTable = aiTable
        self.rowCount = rowCount
        self.copyCSV = copyCSV
        self.copied = copied
        self.openAsArtifact = openAsArtifact
        self.sorted = sorted
        self.sortValue = sortValue
        self.empty = empty
        self.noRows = noRows
        self.loadingRows = loadingRows
        self.complete = complete
        self.showAllRows = showAllRows
    }
}
