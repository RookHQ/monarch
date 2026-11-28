/// One step of a ``MonarchFunnelChart``: its name and how many reached it.
public struct MonarchFunnelChartStage: Sendable, Hashable, Identifiable {
    /// The name of the step, already localised, such as "Visited pricing".
    public let label: String
    /// How many reached this step. Negative and non-finite values count as zero.
    public let value: Double
    /// A stable key, passed back with the stage from `onSelect`. The label by default.
    public let key: String
    /// The colour slot of the bar, or `nil` for the first data slot: a funnel is one measure.
    public let color: MonarchChartColor?

    /// The key.
    public var id: String { key }

    /// A stage.
    ///
    /// - Parameters:
    ///   - label: The name of the step, already localised.
    ///   - value: How many reached this step.
    ///   - key: A stable key. The label when `nil`; give one when two stages share a label.
    ///   - color: The colour slot of the bar. The first data slot when `nil`.
    public init(_ label: String, value: Double, key: String? = nil, color: MonarchChartColor? = nil) {
        self.label = label
        self.value = value
        self.key = key ?? label
        self.color = color
    }
}
