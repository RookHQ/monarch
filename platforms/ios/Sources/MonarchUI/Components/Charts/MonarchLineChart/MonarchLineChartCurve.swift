/// How a ``MonarchLineChart`` joins its points.
public enum MonarchLineChartCurve: Sendable, Hashable, CaseIterable {
    /// Straight lines between points. The default.
    case linear
    /// A smooth line that never overshoots the data.
    case monotone
}
