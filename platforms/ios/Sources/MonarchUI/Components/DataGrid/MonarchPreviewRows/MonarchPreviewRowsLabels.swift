/// Replacements for the built-in strings of ``MonarchPreviewRows``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchPreviewRowsLabels: Sendable {
    /// The spoken name of the filter tabs. Built in: "Show rows".
    public var filter: String?
    /// The tab with every row. Built in: "All".
    public var all: String?
    /// The ready status. Built in: "Ready".
    public var ready: String?
    /// The warning status. Built in: "Warning".
    public var warning: String?
    /// The error status. Built in: "Error".
    public var error: String?
    /// The duplicate status. Built in: "Duplicate".
    public var duplicate: String?
    /// The label of the result cell of a row. Built in: "Result".
    public var result: String?
    /// The label of the issues cell of a row. Built in: "Issues".
    public var issues: String?
    /// The title of a row. Built in: "Row 4".
    public var row: (@Sendable (Int) -> String)?
    /// What VoiceOver reads for a missing value. Built in: "Empty".
    public var emptyValue: String?
    /// The title when the file has no rows. Built in: "No rows to preview".
    public var emptyTitle: String?
    /// The message when the file has no rows. Built in: "The file has a header row but no data rows."
    public var emptyDescription: String?
    /// The title when the filter matches no row. Built in: "No rows with this result".
    public var filteredTitle: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        filter: String? = nil,
        all: String? = nil,
        ready: String? = nil,
        warning: String? = nil,
        error: String? = nil,
        duplicate: String? = nil,
        result: String? = nil,
        issues: String? = nil,
        row: (@Sendable (Int) -> String)? = nil,
        emptyValue: String? = nil,
        emptyTitle: String? = nil,
        emptyDescription: String? = nil,
        filteredTitle: String? = nil
    ) {
        self.filter = filter
        self.all = all
        self.ready = ready
        self.warning = warning
        self.error = error
        self.duplicate = duplicate
        self.result = result
        self.issues = issues
        self.row = row
        self.emptyValue = emptyValue
        self.emptyTitle = emptyTitle
        self.emptyDescription = emptyDescription
        self.filteredTitle = filteredTitle
    }
}
