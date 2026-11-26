/// How two resizable panels are arranged.
public enum MonarchResizablePanelsAxis: Sendable, Hashable, CaseIterable {
    /// Side by side, the first panel at the leading edge. The default.
    case horizontal
    /// Stacked, the first panel on top.
    case vertical
}
