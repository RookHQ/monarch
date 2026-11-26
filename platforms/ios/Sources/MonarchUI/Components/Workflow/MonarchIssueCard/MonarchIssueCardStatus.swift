/// A workflow status of an issue, such as "In progress", for ``MonarchIssueCard``.
@_spi(Experimental) public struct MonarchIssueCardStatus: Identifiable, Sendable, Hashable {
    /// A stable id, passed back when the status is changed from the card.
    public let id: String
    /// The name, already localised.
    public let name: String
    /// The kind of status, which picks the symbol and colour.
    public let category: MonarchIssueCardStatusCategory

    /// A status.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - name: The name, already localised.
    ///   - category: The kind of status.
    public init(id: String, name: String, category: MonarchIssueCardStatusCategory) {
        self.id = id
        self.name = name
        self.category = category
    }
}
