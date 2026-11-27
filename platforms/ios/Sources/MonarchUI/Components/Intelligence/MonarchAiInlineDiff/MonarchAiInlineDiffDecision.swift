/// A decision on one suggested change of a ``MonarchAiInlineDiff``.
@_spi(Experimental) public enum MonarchAiInlineDiffDecision: String, Sendable, Hashable, CaseIterable {
    /// Keep the inserted text and drop the deleted text.
    case accept
    /// Keep the deleted text and drop the inserted text.
    case reject
}
