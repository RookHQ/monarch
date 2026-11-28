/// How the bars of a ``MonarchFunnelChart`` line up.
public enum MonarchFunnelChartAlignment: Sendable, Hashable, CaseIterable {
    /// Each bar is centred, so the stack reads as a funnel. The default.
    case center
    /// Each bar starts at the leading edge, which makes lengths easier to compare.
    case leading
}
