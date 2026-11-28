/// One AI quota to show in ``MonarchAiUsageMeter``: this turn, this
/// conversation or this month.
public struct MonarchAiUsageScope: Sendable, Hashable, Identifiable {
    /// A stable id. The position in the list when none is given.
    public let id: String?
    /// What the scope covers, already localised, such as "This month".
    public let label: String
    /// The amount used so far, in `unit`.
    public let used: Double
    /// The quota. Without it the scope shows the amount alone, with no meter
    /// and no warning.
    public let limit: Double?
    /// What is counted.
    public let unit: MonarchAiUsageUnit

    /// A usage scope.
    ///
    /// - Parameters:
    ///   - label: What the scope covers, already localised.
    ///   - id: A stable id. The position in the list when `nil`, so labels
    ///     may repeat.
    ///   - used: The amount used so far.
    ///   - limit: The quota, or `nil` for none.
    ///   - unit: What is counted.
    public init(
        _ label: String,
        id: String? = nil,
        used: Double,
        limit: Double? = nil,
        unit: MonarchAiUsageUnit = .tokens
    ) {
        self.id = id
        self.label = label
        self.used = used
        self.limit = limit
        self.unit = unit
    }
}
