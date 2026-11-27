/// The kind of a workflow status, which picks its symbol and colour in ``MonarchIssueCard``.
@_spi(Experimental) public enum MonarchIssueCardStatusCategory: Sendable, Hashable, CaseIterable {
    /// Not planned yet.
    case backlog
    /// Planned, not started.
    case unstarted
    /// In progress or in review.
    case started
    /// Finished.
    case completed
    /// Closed without being done.
    case cancelled
}
