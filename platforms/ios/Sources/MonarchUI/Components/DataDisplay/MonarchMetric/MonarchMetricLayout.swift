/// How a ``MonarchMetric`` arranges its label, value and change.
public enum MonarchMetricLayout: Sendable, Hashable, CaseIterable {
    /// The label above the value, the change under it. The default.
    case stacked
    /// The label, value and change on one line, wrapping when they do not
    /// fit. For toolbars and table footers.
    case inline
}
