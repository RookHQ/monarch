/// How a sparkline draws its values.
public enum MonarchSparklineVariant: Sendable, Hashable, CaseIterable {
    /// A line. The default.
    case line
    /// A line with a soft fill under it.
    case area
    /// One bar per value, from zero.
    case bar
}
