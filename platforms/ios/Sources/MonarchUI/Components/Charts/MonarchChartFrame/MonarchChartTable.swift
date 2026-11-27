/// The data behind a chart, as a table: one row per category and one
/// column per series.
///
/// ```swift
/// MonarchChartTable(
///     categoryTitle: "Month",
///     categories: ["Jan", "Feb", "Mar"],
///     series: [MonarchChartTableSeries("Revenue", values: [1200, 1450, 1390])],
///     valueTitle: "Revenue in dollars"
/// )
/// ```
///
/// ``MonarchChartFrame`` shows it with "View as table" and gives it to
/// VoiceOver as the chart's data (Audio Graphs and the data table in the
/// rotor), so every value the chart shows is also available as text.
public struct MonarchChartTable: Sendable, Hashable {
    /// The heading of the category column, such as "Month".
    public let categoryTitle: String
    /// The categories, in order: the rows of the table and the x axis.
    public let categories: [String]
    /// The series: the value columns.
    public let series: [MonarchChartTableSeries]
    /// What the values measure, such as "Revenue in dollars": the y axis.
    public let valueTitle: String?

    /// A table.
    ///
    /// - Parameters:
    ///   - categoryTitle: The heading of the category column, already localised.
    ///   - categories: The categories, in order.
    ///   - series: The series, each with one value per category.
    ///   - valueTitle: What the values measure, already localised.
    public init(
        categoryTitle: String,
        categories: [String],
        series: [MonarchChartTableSeries],
        valueTitle: String? = nil
    ) {
        self.categoryTitle = categoryTitle
        self.categories = categories
        self.series = series
        self.valueTitle = valueTitle
    }
}
