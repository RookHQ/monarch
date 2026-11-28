/// How VoiceOver reaches the chart inside a ``MonarchChartFrame`` that has
/// a table.
public enum MonarchChartFrameContentAccessibility: Sendable, Hashable, CaseIterable {
    /// The chart is one element named by the title, with the table as its
    /// accessibility chart descriptor (Audio Graphs). The default.
    case chart
    /// The chart's own elements stay reachable: a legend with values, a
    /// selected bar, a control drawn on the plot. The chart gives its plot
    /// the descriptor itself. "View as table" still works.
    case contain
}
