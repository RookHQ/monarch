/// What happened to a run of text in a ``MonarchAiDiffSegment``.
@_spi(Experimental) public enum MonarchAiDiffSegmentKind: String, Sendable, Hashable, CaseIterable {
    /// The text is unchanged.
    case equal
    /// The text is suggested as an addition.
    case insert
    /// The text is suggested for removal.
    case delete
}
