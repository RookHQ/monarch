/// Built-in strings of ``MonarchMetric``, from the `MonarchMetric` strings table.
enum MonarchMetricStrings {
    private static let table = "MonarchMetric"

    /// Spoken for a missing value, which is shown as a dash.
    static var empty: String { MonarchStrings.string("monarch.metric.empty", table: table) }
}
