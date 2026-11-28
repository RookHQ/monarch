/// What each legend entry of a ``MonarchDonutChart`` shows after its label.
public enum MonarchDonutChartLegendContent: Sendable, Hashable, CaseIterable {
    /// The value and the share: "412 · 48%". The default.
    case both
    /// The value only.
    case value
    /// The share only.
    case percent
    /// The label only.
    case none
}
