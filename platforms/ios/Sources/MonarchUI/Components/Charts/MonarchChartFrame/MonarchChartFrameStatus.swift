/// What the body of a ``MonarchChartFrame`` shows.
public enum MonarchChartFrameStatus: Sendable, Hashable, CaseIterable {
    /// The chart, or its data table. The default.
    case ready
    /// A placeholder of the chart's height while the data loads.
    case loading
    /// An empty state: there is no data yet.
    case empty
    /// An error, with Try again when the frame has `onRetry`.
    case error
}
