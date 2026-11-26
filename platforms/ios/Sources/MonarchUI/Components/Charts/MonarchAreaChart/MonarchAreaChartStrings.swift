/// Built-in strings of the area chart, from the `MonarchAreaChart` strings table.
enum MonarchAreaChartStrings {
    private static let table = "MonarchAreaChart"

    /// The name of the total of a stack, in the popover and the data table.
    static var total: String { MonarchStrings.string("monarch.areaChart.total", table: table) }

    /// A value with its share of the stack, in the 100% stacked popover: "120 (40%)".
    static func share(_ value: String, _ share: String) -> String {
        MonarchStrings.string("monarch.areaChart.share", table: table, value, share)
    }
}
