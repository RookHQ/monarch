/// The mark beside each label of a ``MonarchChartLegend``.
public enum MonarchChartLegendMarker: Sendable, Hashable, CaseIterable {
    /// A short line, for line and area charts. The default.
    case line
    /// A square, for bar charts.
    case square
    /// A dot, for scatter plots and points.
    case dot
}
