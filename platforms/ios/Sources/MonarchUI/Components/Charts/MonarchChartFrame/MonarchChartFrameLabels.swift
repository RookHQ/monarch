/// Replacements for the built-in strings of ``MonarchChartFrame``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchChartFrameLabels: Sendable, Hashable {
    /// The toggle title while the chart shows. Built in: "View as table".
    public var viewTable: String?
    /// The toggle title while the table shows. Built in: "View as chart".
    public var viewChart: String?
    /// Spoken while the data loads. Built in: "Loading chart".
    public var loading: String?
    /// The empty-state title. Built in: "No data yet".
    public var emptyTitle: String?
    /// The error message. Built in: "This chart didn't load."
    public var error: String?
    /// The spoken name of the legend. Built in: "Legend".
    public var legend: String?
    /// Spoken for a gap in the table. Built in: "No value".
    public var noValue: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        viewTable: String? = nil,
        viewChart: String? = nil,
        loading: String? = nil,
        emptyTitle: String? = nil,
        error: String? = nil,
        legend: String? = nil,
        noValue: String? = nil
    ) {
        self.viewTable = viewTable
        self.viewChart = viewChart
        self.loading = loading
        self.emptyTitle = emptyTitle
        self.error = error
        self.legend = legend
        self.noValue = noValue
    }
}
