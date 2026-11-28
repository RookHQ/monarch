/// Built-in strings of the bar chart, from the `MonarchBarChart` strings table.
enum MonarchBarChartStrings {
    private static let table = "MonarchBarChart"

    /// The heading of the category column of the table.
    static var category: String { MonarchStrings.string("monarch.barChart.category", table: table) }
    /// The name of a stack's total.
    static var total: String { MonarchStrings.string("monarch.barChart.total", table: table) }
    /// The summary of a chart or series without values.
    static var noData: String { MonarchStrings.string("monarch.barChart.noData", table: table) }
    /// The title of the button that opens an inspected category.
    static var details: String { MonarchStrings.string("monarch.barChart.details", table: table) }
    /// The title of the button that ends the inspection.
    static var clearSelection: String { MonarchStrings.string("monarch.barChart.clearSelection", table: table) }

    /// The summary's first sentence for one category.
    static func onePoint(_ category: String) -> String {
        MonarchStrings.string("monarch.barChart.onePoint", table: table, category)
    }

    /// The summary's first sentence for several categories.
    static func points(count: String, first: String, last: String) -> String {
        MonarchStrings.string("monarch.barChart.points", table: table, count, first, last)
    }

    /// The summary of one series.
    static func series(_ name: String, total: String, highest: String, value: String) -> String {
        MonarchStrings.string("monarch.barChart.series", table: table, name, total, highest, value)
    }
}
