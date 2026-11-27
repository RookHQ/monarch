/// Whether a passage backs a claim or speaks against it.
@_spi(Experimental) public enum MonarchAiClaimEvidenceStance: Sendable, Hashable, CaseIterable {
    /// The passage backs the claim.
    case supports
    /// The passage speaks against the claim.
    case contradicts
}
