/// Replacements for the built-in strings of ``MonarchDonutChart``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchDonutChartLabels: Sendable, Hashable {
    /// The strings of the chart frame: "View as table", loading, empty and
    /// error text, "Legend", "No value".
    public var frame: MonarchChartFrameLabels
    /// The name of the slice the smallest segments fold into. Built in: "Other".
    public var other: String?
    /// The heading of the segment column of the table. Built in: "Segment".
    public var segment: String?
    /// The heading of the value column of the table. Built in: "Value".
    public var value: String?
    /// The heading of the share column of the table. Built in: "Share".
    public var share: String?
    /// The name of the sum, in the table and the summary. Built in: "Total".
    public var total: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
        other: String? = nil,
        segment: String? = nil,
        value: String? = nil,
        share: String? = nil,
        total: String? = nil
    ) {
        self.frame = frame
        self.other = other
        self.segment = segment
        self.value = value
        self.share = share
        self.total = total
    }
}
