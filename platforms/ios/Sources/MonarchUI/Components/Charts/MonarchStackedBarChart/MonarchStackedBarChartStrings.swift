import Foundation

/// Built-in strings of the stacked bar chart, from the `MonarchStackedBarChart` strings table.
enum MonarchStackedBarChartStrings {
    /// The summary of one series with the 100% offset: series, average share,
    /// category of the largest share, that share.
    static var shareSeries: String {
        MonarchStrings.string("monarch.stackedBarChart.shareSeries", table: "MonarchStackedBarChart")
    }

    /// Fills a format's `%@` placeholders.
    static func fill(_ format: String, _ arguments: String...) -> String {
        String(format: format, arguments: arguments)
    }
}
