/// A card under the comparison that says who an option suits.
public struct MonarchComparisonBestFor: Sendable, Hashable, Identifiable {
    /// A stable id, normally the column's id.
    public let id: String
    /// The card title, such as "Product A suits".
    public let title: String
    /// Who it suits.
    public let description: String?
    /// Whether the card is emphasised like its column.
    public let isHighlighted: Bool

    /// A best-for card.
    ///
    /// - Parameters:
    ///   - id: A stable id, normally the column's id.
    ///   - title: The card title, already localised.
    ///   - description: Who the option suits.
    ///   - isHighlighted: Whether the card is emphasised.
    public init(_ id: String, title: String, description: String? = nil, isHighlighted: Bool = false) {
        self.id = id
        self.title = title
        self.description = description
        self.isHighlighted = isHighlighted
    }
}
