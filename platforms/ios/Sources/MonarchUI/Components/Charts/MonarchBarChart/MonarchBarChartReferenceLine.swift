/// A dashed line across a ``MonarchBarChart`` at one value, for a target or
/// an average.
public struct MonarchBarChartReferenceLine: Sendable, Hashable {
    /// Where the line is drawn, in the units of the values.
    public let value: Double
    /// The text beside the line, already localised, such as "Target".
    public let label: String?

    /// A reference line.
    ///
    /// - Parameters:
    ///   - value: Where the line is drawn.
    ///   - label: The text beside the line, already localised.
    public init(_ value: Double, label: String? = nil) {
        self.value = value
        self.label = label
    }
}
