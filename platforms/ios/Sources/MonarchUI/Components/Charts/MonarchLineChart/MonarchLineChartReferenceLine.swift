/// A dashed horizontal line across a ``MonarchLineChart``: a target, a
/// baseline or a service level.
public struct MonarchLineChartReferenceLine: Sendable, Hashable {
    /// Where the line is drawn, on the value axis.
    public let value: Double
    /// The name written above the line with its value, already localised.
    public let label: String?

    /// A reference line.
    ///
    /// - Parameters:
    ///   - value: Where the line is drawn, on the value axis.
    ///   - label: The name written above the line, already localised.
    public init(_ value: Double, label: String? = nil) {
        self.value = value
        self.label = label
    }
}
