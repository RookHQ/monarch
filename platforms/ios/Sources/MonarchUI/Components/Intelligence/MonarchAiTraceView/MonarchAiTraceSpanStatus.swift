/// How a span ended, or `running` while it has not.
@_spi(Experimental) public enum MonarchAiTraceSpanStatus: String, Sendable, Hashable, Codable, CaseIterable {
    /// It finished.
    case ok
    /// It failed.
    case error
    /// It was stopped.
    case cancelled
    /// It has not ended.
    case running
}
