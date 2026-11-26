/// One likely duplicate of a record, shown by ``MonarchDuplicatePanel``.
public struct MonarchDuplicateMatch: Identifiable, Hashable, Sendable {
    /// A stable identifier of the record.
    public let id: String
    /// The name of the record.
    public let title: String
    /// What kind of record it is, for example "Client" or "Open lead".
    public let kind: String?
    /// Why it matched, in words: "Same email", "Same company domain".
    public let reasons: [String]
    /// A short extra fact, for example "Archived" or "Created 3 Mar".
    public let detail: String?
    /// How confident the match is, from 0 to 1. Shown as a percentage.
    public let score: Double?

    /// A likely duplicate.
    ///
    /// - Parameters:
    ///   - id: A stable identifier of the record.
    ///   - title: The name of the record, already localised.
    ///   - kind: What kind of record it is.
    ///   - reasons: Why it matched, in words.
    ///   - detail: A short extra fact.
    ///   - score: How confident the match is, from 0 to 1.
    public init(
        id: String,
        title: String,
        kind: String? = nil,
        reasons: [String] = [],
        detail: String? = nil,
        score: Double? = nil
    ) {
        self.id = id
        self.title = title
        self.kind = kind
        self.reasons = reasons
        self.detail = detail
        self.score = score
    }
}
