/// One measure drawn by a ``MonarchLineChart``: a name, a colour and one
/// value per x value.
public struct MonarchLineChartSeries: Sendable, Hashable, Identifiable {
    /// The stable key of the series, used by the legend and the hidden set.
    public let key: String
    /// The visible name, already localised. Shown in the legend, the
    /// inspection popover and the data table.
    public let label: String
    /// One value per x value, in x order. `nil` or a value that is not
    /// finite is a gap: the line breaks there.
    public let values: [Double?]
    /// The colour slot, or `nil` for the slot of the series' position.
    public let color: MonarchChartColor?
    /// Whether the line is dashed: a second encoding besides colour, for a
    /// previous period or a forecast.
    public let isDashed: Bool

    /// The key.
    public var id: String { key }

    /// A series.
    ///
    /// - Parameters:
    ///   - key: The stable key of the series.
    ///   - label: The visible name, already localised.
    ///   - values: One value per x value; `nil` is a gap.
    ///   - color: The colour slot, or `nil` for the slot of the series' position.
    ///   - isDashed: Whether the line is dashed.
    public init(
        _ key: String,
        label: String,
        values: [Double?],
        color: MonarchChartColor? = nil,
        isDashed: Bool = false
    ) {
        self.key = key
        self.label = label
        self.values = values
        self.color = color
        self.isDashed = isDashed
    }
}
