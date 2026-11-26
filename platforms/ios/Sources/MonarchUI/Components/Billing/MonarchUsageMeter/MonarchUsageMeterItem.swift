/// One metered quantity of a ``MonarchUsageMeter``: requests, storage, seats.
public struct MonarchUsageMeterItem: Identifiable, Sendable, Hashable {
    /// A stable identity. The label unless another is given.
    public let id: String
    /// What is measured, such as "Requests". Already localised.
    public let label: String
    /// The amount used in the period.
    public let used: Double
    /// The limit, or `nil` for a plain count with no meter.
    public let limit: Double?
    /// The unit written after the figures, such as "GB".
    public let unit: String?
    /// One line under the meter, such as what counts as a unit.
    public let description: String?

    /// A metered item.
    ///
    /// - Parameters:
    ///   - label: What is measured, already localised.
    ///   - id: A stable identity. The label when `nil`.
    ///   - used: The amount used in the period.
    ///   - limit: The limit. `nil`, zero or less means there is no limit.
    ///   - unit: The unit written after the figures.
    ///   - description: One line under the meter.
    public init(
        _ label: String,
        id: String? = nil,
        used: Double,
        limit: Double? = nil,
        unit: String? = nil,
        description: String? = nil
    ) {
        self.id = id ?? label
        self.label = label
        self.used = used
        self.limit = limit
        self.unit = unit
        self.description = description
    }
}
