/// The direction of the bars of a ``MonarchBarChart``.
public enum MonarchBarChartOrientation: Sendable, Hashable, CaseIterable {
    /// Columns: categories along the bottom, values up. The default.
    case vertical
    /// Bars: categories down the leading side, values toward the trailing
    /// edge. For long category names and rankings.
    case horizontal
}
