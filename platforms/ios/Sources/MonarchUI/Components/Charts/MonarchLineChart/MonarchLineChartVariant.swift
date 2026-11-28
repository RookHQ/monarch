/// How a ``MonarchLineChart`` draws each series.
public enum MonarchLineChartVariant: Sendable, Hashable, CaseIterable {
    /// A line. The default.
    case line
    /// A line with a soft fill down to the baseline.
    case area
}
