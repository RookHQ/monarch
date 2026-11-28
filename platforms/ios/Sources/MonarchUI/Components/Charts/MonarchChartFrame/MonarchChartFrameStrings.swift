/// Built-in strings of the chart frame, from the `MonarchChartFrame` strings table.
enum MonarchChartFrameStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.chartFrame.\(key)", table: "MonarchChartFrame")
    }

    /// The toggle title while the chart shows.
    static var viewTable: String { string("viewTable") }
    /// The toggle title while the table shows.
    static var viewChart: String { string("viewChart") }
    /// Spoken while the data loads.
    static var loading: String { string("loading") }
    /// The empty-state title.
    static var emptyTitle: String { string("emptyTitle") }
    /// The error message.
    static var error: String { string("error") }
    /// Spoken for a gap in the table.
    static var noValue: String { string("noValue") }
}
