/// The order of the slices of a ``MonarchDonutChart``.
public enum MonarchDonutChartSort: Sendable, Hashable, CaseIterable {
    /// The largest slice first, clockwise from the top. The default.
    case descending
    /// The order the segments were given in.
    case none
}
