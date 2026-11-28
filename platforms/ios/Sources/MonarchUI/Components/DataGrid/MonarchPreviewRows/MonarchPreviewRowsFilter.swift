/// Which rows ``MonarchPreviewRows`` shows.
public enum MonarchPreviewRowsFilter: Sendable, Hashable {
    /// Every row.
    case all
    /// Only the rows with this status.
    case status(MonarchPreviewRowStatus)
}
