/// Replacements for the built-in strings of ``MonarchStackedBarChart``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchStackedBarChartLabels: Sendable, Hashable {
    /// The strings of the bar chart it draws: the frame, the table's category
    /// heading, "Total", "No data", Details and Clear.
    public var bar: MonarchBarChartLabels
    /// The summary of one series with the 100% offset: `%1$@` the series,
    /// `%2$@` its average share, `%3$@` the category of its largest share and
    /// `%4$@` that share. Built in: "%1$@: average share %2$@, largest %3$@ at %4$@".
    public var shareSeries: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(bar: MonarchBarChartLabels = MonarchBarChartLabels(), shareSeries: String? = nil) {
        self.bar = bar
        self.shareSeries = shareSeries
    }
}
