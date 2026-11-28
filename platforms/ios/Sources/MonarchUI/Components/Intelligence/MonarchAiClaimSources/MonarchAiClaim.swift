/// One statement of an AI answer and the passages that back or contradict
/// it, shown by ``MonarchAiClaimSources``.
@_spi(Experimental) public struct MonarchAiClaim: Identifiable, Sendable, Hashable {
    /// A stable id, passed to the flag and verify handlers.
    public let id: String
    /// The claim as the answer states it.
    public let text: String
    /// How well the sources back it.
    public let support: MonarchAiClaimSupport
    /// The passages found for it, in the order to show them.
    public let evidence: [MonarchAiClaimEvidence]

    /// Whether the claim is not fully supported: the ones "Needs checking" shows.
    public var needsChecking: Bool { support != .supported }

    /// A claim.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - text: The claim as the answer states it.
    ///   - support: How well the sources back it.
    ///   - evidence: The passages found for it.
    public init(id: String, text: String, support: MonarchAiClaimSupport, evidence: [MonarchAiClaimEvidence] = []) {
        self.id = id
        self.text = text
        self.support = support
        self.evidence = evidence
    }
}
