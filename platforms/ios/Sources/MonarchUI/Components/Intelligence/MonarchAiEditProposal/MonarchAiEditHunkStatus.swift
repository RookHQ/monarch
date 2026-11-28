/// A person's decision on one change of a ``MonarchAiEditProposal``.
@_spi(Experimental) public enum MonarchAiEditHunkStatus: String, Sendable, Hashable, Codable, CaseIterable {
    /// Not decided yet.
    case proposed
    /// Accepted.
    case accepted
    /// Rejected.
    case rejected
}
