/// Built-in strings of the line chart, from the `MonarchLineChart` strings table.
enum MonarchLineChartStrings {
    private static let table = "MonarchLineChart"

    /// The heading of the date column of the data table.
    static var date: String { MonarchStrings.string("monarch.lineChart.date", table: table) }
    /// The heading of the category column of the data table.
    static var category: String { MonarchStrings.string("monarch.lineChart.category", table: table) }
    /// The name of the value axis when the app gives none.
    static var value: String { MonarchStrings.string("monarch.lineChart.value", table: table) }
}
