/// Where a ``MonarchAiSuggestedAction`` is in its run.
@_spi(Experimental) public enum MonarchAiSuggestedActionStatus: Sendable, Hashable, CaseIterable {
    /// It can run.
    case ready
    /// It is running.
    case running
    /// It ran.
    case done
    /// Its run failed; it can run again.
    case failed
}
