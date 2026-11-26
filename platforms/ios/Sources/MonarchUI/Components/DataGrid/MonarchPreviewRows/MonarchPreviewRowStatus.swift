/// What an import will do with one row of a file.
public enum MonarchPreviewRowStatus: Sendable, Hashable, CaseIterable {
    /// The row is imported as it is.
    case ready
    /// The row is imported, with warnings.
    case warning
    /// The row is skipped because of errors.
    case error
    /// The row matches a record that already exists.
    case duplicate
}
