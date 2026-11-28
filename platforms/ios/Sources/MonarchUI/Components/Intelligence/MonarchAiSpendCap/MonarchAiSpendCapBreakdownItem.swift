/// One line of a spending breakdown: what an agent or a feature has spent this period.
@_spi(Experimental) public struct MonarchAiSpendCapBreakdownItem: Identifiable, Sendable, Hashable {
    /// A stable id.
    public var id: String
    /// The agent or feature, such as "Research agent", already localised.
    public var label: String
    /// Spent so far this period, as an integer in the cap's unit.
    public var spent: Int

    /// A breakdown line.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - label: The agent or feature.
    ///   - spent: Spent so far this period, as an integer in the cap's unit.
    public init(id: String, label: String, spent: Int) {
        self.id = id
        self.label = label
        self.spent = spent
    }
}
