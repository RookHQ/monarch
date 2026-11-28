/// How many items have one decision in a ``MonarchSavingsPanel``, such as
/// keep, review or let lapse.
@_spi(Experimental) public struct MonarchSavingsDecisionCount: Identifiable, Sendable, Hashable {
    /// A stable id.
    public let id: String
    /// The visible label, already localised.
    public let label: String
    /// How many items have this decision.
    public let count: Int

    /// A decision count.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - label: The visible label.
    ///   - count: How many items have this decision.
    public init(id: String, label: String, count: Int) {
        self.id = id
        self.label = label
        self.count = count
    }
}
