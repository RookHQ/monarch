/// How the areas of a ``MonarchAreaChart`` combine.
public enum MonarchAreaChartStacking: Sendable, Hashable, CaseIterable {
    /// Each series fills from the baseline, so the areas overlap. The default.
    case overlap
    /// The series are piled up, so the top edge is the total.
    case stacked
    /// The series are piled up as shares of 100% at every x value.
    case expand
}
