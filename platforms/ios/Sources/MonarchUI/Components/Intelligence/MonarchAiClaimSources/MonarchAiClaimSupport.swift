/// How well the sources back a claim in ``MonarchAiClaimSources``.
@_spi(Experimental) public enum MonarchAiClaimSupport: Sendable, Hashable, CaseIterable {
    /// At least one passage backs it and none contradicts it.
    case supported
    /// Passages back only part of it.
    case partial
    /// No source backs it.
    case unsupported
    /// Sources disagree about it.
    case conflicting
    /// It has not been compared with the sources yet.
    case unchecked
}
