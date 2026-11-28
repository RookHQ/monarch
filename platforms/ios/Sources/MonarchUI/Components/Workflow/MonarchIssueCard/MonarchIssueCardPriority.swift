/// How urgent an issue is, for ``MonarchIssueCard``. Every level has its own symbol and name.
@_spi(Experimental) public enum MonarchIssueCardPriority: Sendable, Hashable, CaseIterable {
    /// Needs attention now.
    case urgent
    /// Next in line.
    case high
    /// Normal.
    case medium
    /// When there is time.
    case low
    /// No priority set.
    case none
}
