/// Built-in strings of the donut chart, from the `MonarchDonutChart` strings table.
enum MonarchDonutChartStrings {
    private static let table = "MonarchDonutChart"

    /// The name of the slice the smallest segments fold into.
    static var other: String { MonarchStrings.string("monarch.donutChart.other", table: table) }
    /// The heading of the segment column of the table.
    static var segment: String { MonarchStrings.string("monarch.donutChart.segment", table: table) }
    /// The heading of the value column of the table.
    static var value: String { MonarchStrings.string("monarch.donutChart.value", table: table) }
    /// The heading of the share column of the table.
    static var share: String { MonarchStrings.string("monarch.donutChart.share", table: table) }
    /// The name of the sum.
    static var total: String { MonarchStrings.string("monarch.donutChart.total", table: table) }
}
