/// A decision announced by a ``MonarchAiEditProposal``.
@_spi(Experimental) public enum MonarchAiEditProposalEvent: Sendable, Hashable, CaseIterable {
    /// One change was accepted.
    case accepted
    /// One change was rejected.
    case rejected
    /// Every undecided change was accepted.
    case acceptedAll
    /// Every undecided change was rejected.
    case rejectedAll
}
