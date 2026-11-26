/// The state of the summary on a ``MonarchAiSummaryCard``.
@_spi(Experimental) public enum MonarchAiSummaryCardStatus: Sendable, Hashable, CaseIterable {
    /// The summary is current.
    case ready
    /// A summary is being written: a placeholder without one, a busy Refresh with one.
    case generating
    /// The data changed since the summary was written: a warning asks for a refresh.
    case stale
    /// The summary could not be written: an error banner, and no body.
    case error
}
