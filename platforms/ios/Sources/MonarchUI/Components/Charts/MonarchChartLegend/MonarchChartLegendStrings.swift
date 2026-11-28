/// Built-in strings of the chart legend, from the `MonarchChartLegend` strings table.
enum MonarchChartLegendStrings {
    /// The spoken name of the legend.
    static var legend: String { MonarchStrings.string("monarch.chartLegend.legend", table: "MonarchChartLegend") }
    /// The title of the button that shows every hidden series again.
    static var showAll: String { MonarchStrings.string("monarch.chartLegend.showAll", table: "MonarchChartLegend") }
}
