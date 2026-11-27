/// One row of an import file as the import will treat it.
public struct MonarchPreviewRow: Sendable, Hashable, Identifiable {
    /// The 1-based data row number in the file; the header row is not counted.
    public var row: Int
    /// The cell values by column id, already formatted. A missing or empty value shows a dash.
    public var values: [String: String]
    /// Overrides the status worked out from the errors and warnings, for example ``MonarchPreviewRowStatus/duplicate``.
    public var status: MonarchPreviewRowStatus?
    /// Problems that make the import skip the row.
    public var errors: [MonarchPreviewRowIssue]
    /// Problems the import accepts.
    public var warnings: [MonarchPreviewRowIssue]
    /// Extra text under the issues, for example the record a duplicate matches.
    public var note: String?

    /// The row number, which identifies the row.
    public var id: Int { row }

    /// The status: ``status`` if set, otherwise error when there are errors,
    /// warning when there are warnings, else ready.
    public var resolvedStatus: MonarchPreviewRowStatus {
        if let status { return status }
        if !errors.isEmpty { return .error }
        if !warnings.isEmpty { return .warning }
        return .ready
    }

    /// A row.
    ///
    /// - Parameters:
    ///   - row: The 1-based data row number in the file.
    ///   - values: The cell values by column id, already formatted.
    ///   - status: Overrides the status worked out from the errors and warnings.
    ///   - errors: Problems that make the import skip the row.
    ///   - warnings: Problems the import accepts.
    ///   - note: Extra text under the issues.
    public init(
        row: Int,
        values: [String: String],
        status: MonarchPreviewRowStatus? = nil,
        errors: [MonarchPreviewRowIssue] = [],
        warnings: [MonarchPreviewRowIssue] = [],
        note: String? = nil
    ) {
        self.row = row
        self.values = values
        self.status = status
        self.errors = errors
        self.warnings = warnings
        self.note = note
    }
}
