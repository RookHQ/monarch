/// One passage from a source, tied to a claim of ``MonarchAiClaimSources``.
@_spi(Experimental) public struct MonarchAiClaimEvidence: Identifiable, Sendable, Hashable {
    /// A stable id, unique within the claim.
    public let id: String
    /// The id of the source the passage comes from.
    public let sourceID: String
    /// The exact passage, word for word. Shown as a quotation and copied by Copy quote.
    public let quote: String
    /// Where the passage is in the source, such as "Page 4".
    public let location: String?
    /// Whether the passage backs the claim or speaks against it. `nil` when not stated.
    public let stance: MonarchAiClaimEvidenceStance?

    /// A passage.
    ///
    /// - Parameters:
    ///   - id: A stable id, unique within the claim.
    ///   - sourceID: The id of the source the passage comes from.
    ///   - quote: The exact passage.
    ///   - location: Where the passage is in the source.
    ///   - stance: Whether the passage backs the claim or speaks against it.
    public init(
        id: String,
        sourceID: String,
        quote: String,
        location: String? = nil,
        stance: MonarchAiClaimEvidenceStance? = nil
    ) {
        self.id = id
        self.sourceID = sourceID
        self.quote = quote
        self.location = location
        self.stance = stance
    }
}
