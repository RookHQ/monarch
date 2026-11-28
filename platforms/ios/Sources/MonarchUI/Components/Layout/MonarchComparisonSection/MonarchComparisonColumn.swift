/// One column of a ``MonarchComparisonSection``: the product or an alternative.
public struct MonarchComparisonColumn: Sendable, Hashable, Identifiable {
    /// A stable id, used by the row values.
    public let id: String
    /// The name shown at the top of the column.
    public let name: String
    /// A short label under the name, such as "This product".
    public let badge: String?
    /// Whether the column is emphasised, normally the product itself.
    public let isHighlighted: Bool

    /// A column.
    ///
    /// - Parameters:
    ///   - id: A stable id, used by the row values.
    ///   - name: The name shown at the top of the column, already localised.
    ///   - badge: A short label under the name, such as "This product".
    ///   - isHighlighted: Whether the column is emphasised.
    public init(_ id: String, name: String, badge: String? = nil, isHighlighted: Bool = false) {
        self.id = id
        self.name = name
        self.badge = badge
        self.isHighlighted = isHighlighted
    }
}
