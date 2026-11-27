/// Built-in strings of the sparkline, from the `MonarchSparkline` strings table.
enum MonarchSparklineStrings {
    /// Shown when there are too few values to draw.
    static var notEnoughData: String {
        MonarchStrings.string("monarch.sparkline.notEnoughData", table: "MonarchSparkline")
    }

    /// The spoken value of a labelled sparkline.
    static func summary(last: String, minimum: String, maximum: String) -> String {
        MonarchStrings.string("monarch.sparkline.summary", table: "MonarchSparkline", last, minimum, maximum)
    }
}
