/// One feature row of a ``MonarchPricingTable``, with a value per plan.
public struct MonarchPricingFeature: Sendable, Hashable, Identifiable {
    /// The stable id of the row.
    public let id: String
    /// The feature name, already localised.
    public let label: String
    /// A short explanation under the name.
    public let description: String?
    /// Rows with the same group are shown under one heading, in first-seen order.
    public let group: String?
    /// The value for each plan id. A plan that is missing is not included.
    public let values: [String: MonarchPricingFeatureValue]

    /// A feature row.
    ///
    /// - Parameters:
    ///   - id: The stable id of the row.
    ///   - label: The feature name, already localised.
    ///   - description: A short explanation under the name.
    ///   - group: The heading this row is listed under.
    ///   - values: The value for each plan id.
    public init(
        _ id: String,
        label: String,
        description: String? = nil,
        group: String? = nil,
        values: [String: MonarchPricingFeatureValue]
    ) {
        self.id = id
        self.label = label
        self.description = description
        self.group = group
        self.values = values
    }
}
