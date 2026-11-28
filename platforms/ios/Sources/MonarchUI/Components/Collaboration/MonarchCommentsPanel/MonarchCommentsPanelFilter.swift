/// Which threads ``MonarchCommentsPanel`` lists.
@_spi(Experimental) public enum MonarchCommentsPanelFilter: String, Sendable, Hashable, CaseIterable {
    /// Threads that are not resolved.
    case open
    /// Resolved threads.
    case resolved
}
