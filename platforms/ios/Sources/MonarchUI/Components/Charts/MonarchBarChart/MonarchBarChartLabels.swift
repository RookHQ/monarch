/// Replacements for the built-in strings of ``MonarchBarChart``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchBarChartLabels: Sendable, Hashable {
    /// The strings of the chart frame: "View as table", loading, empty and
    /// error text, "No value".
    public var frame: MonarchChartFrameLabels
    /// The heading of the category column of the table. Built in: "Category".
    public var category: String?
    /// The name of a stack's total. Built in: "Total".
    public var total: String?
    /// The summary of a chart or series without values. Built in: "No data".
    public var noData: String?
    /// The title of the button that opens an inspected category, and the
    /// start of the matching VoiceOver action. Built in: "Details".
    public var details: String?
    /// The title of the button that ends the inspection. Built in: "Clear".
    public var clearSelection: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
        category: String? = nil,
        total: String? = nil,
        noData: String? = nil,
        details: String? = nil,
        clearSelection: String? = nil
    ) {
        self.frame = frame
        self.category = category
        self.total = total
        self.noData = noData
        self.details = details
        self.clearSelection = clearSelection
    }
}
