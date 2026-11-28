/// One measure of a ``MonarchBarChart``: its name, colour and one value per
/// category.
public struct MonarchBarChartSeries: Sendable, Hashable, Identifiable {
    /// The stable key of the series, used in the hidden set.
    public let key: String
    /// The name, already localised, shown in the legend, the table and the
    /// summary.
    public let label: String
    /// One value per category, in category order. `nil`, or a value that is
    /// not finite, draws no bar.
    public let values: [Double?]
    /// The colour slot, or `nil` for the slot of the series' position.
    public let color: MonarchChartColor?

    /// The key.
    public var id: String { key }

    /// A series.
    ///
    /// - Parameters:
    ///   - key: The stable key of the series.
    ///   - label: The name, already localised.
    ///   - values: One value per category, in category order; `nil` is a gap.
    ///   - color: The colour slot, or `nil` for the slot of the series' position.
    public init(_ key: String, label: String, values: [Double?], color: MonarchChartColor? = nil) {
        self.key = key
        self.label = label
        self.values = values
        self.color = color
    }
}
