/// A place in the text that cites a source.
public struct MonarchAiCitationPart: Identifiable, Sendable, Hashable, Codable {
    /// The id of the citation.
    public var id: String
    /// The id of the source part it cites.
    public var sourceId: String
    /// The passage of the source that backs the text.
    public var quote: String?

    /// A citation.
    ///
    /// - Parameters:
    ///   - id: The id of the citation.
    ///   - sourceId: The id of the source part it cites.
    ///   - quote: The passage of the source that backs the text.
    public init(id: String, sourceId: String, quote: String? = nil) {
        self.id = id
        self.sourceId = sourceId
        self.quote = quote
    }
}
