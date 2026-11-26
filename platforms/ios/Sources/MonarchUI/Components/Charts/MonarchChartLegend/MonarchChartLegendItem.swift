/// One entry of a ``MonarchChartLegend``: a series name and its colour.
public struct MonarchChartLegendItem: Sendable, Hashable, Identifiable {
    /// The stable key of the series, used in the hidden set.
    public let key: String
    /// The visible name, already localised. Always shown, so a series is
    /// never told apart by colour alone.
    public let label: String
    /// The colour slot, or `nil` for the slot of the entry's position.
    public let color: MonarchChartColor?
    /// Whether the marker is dashed (line) or outlined (square and dot), to
    /// match a dashed series.
    public let isDashed: Bool
    /// A value after the label, such as the series total, already formatted.
    public let value: String?

    /// The key.
    public var id: String { key }

    /// An entry.
    ///
    /// - Parameters:
    ///   - key: The stable key of the series.
    ///   - label: The visible name, already localised.
    ///   - color: The colour slot, or `nil` for the slot of the entry's position.
    ///   - isDashed: Whether the marker is dashed or outlined.
    ///   - value: A value after the label, already formatted.
    public init(
        _ key: String,
        label: String,
        color: MonarchChartColor? = nil,
        isDashed: Bool = false,
        value: String? = nil
    ) {
        self.key = key
        self.label = label
        self.color = color
        self.isDashed = isDashed
        self.value = value
    }
}
