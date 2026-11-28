/// One slice of a ``MonarchDonutChart``: a named part of the whole.
public struct MonarchDonutChartSegment: Sendable, Hashable, Identifiable {
    /// The stable key of the segment.
    public let key: String
    /// The name, already localised, shown in the legend, the table and the
    /// summary.
    public let label: String
    /// The amount. A negative value, or one that is not finite, counts as 0.
    public let value: Double
    /// The colour slot, or `nil` for the slot of the segment's position after
    /// sorting. Pin it so a segment keeps its colour when filters change.
    public let color: MonarchChartColor?

    /// The key.
    public var id: String { key }

    /// A segment.
    ///
    /// - Parameters:
    ///   - key: The stable key of the segment.
    ///   - label: The name, already localised.
    ///   - value: The amount; negative and non-finite values count as 0.
    ///   - color: The colour slot, or `nil` for the slot of its position.
    public init(_ key: String, label: String, value: Double, color: MonarchChartColor? = nil) {
        self.key = key
        self.label = label
        self.value = value
        self.color = color
    }
}
