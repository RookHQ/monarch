/// Replacements for the built-in strings of ``MonarchHeatmap``.
/// A property that is `nil` keeps the built-in English string.
///
/// Sentences with values are formats: `%1$@`, `%2$@`… stand for the values
/// named in each property's description, already formatted.
public struct MonarchHeatmapLabels: Sendable, Hashable {
    /// The strings of the chart frame: the table toggle, loading, empty, error,
    /// the legend's spoken name and the spoken text for a cell without data.
    public var frame: MonarchChartFrameLabels
    /// The table heading of the row names. Built in: "Row".
    public var rowHeader: String?
    /// Before the lightest step of the legend. Built in: "Less".
    public var less: String?
    /// After the darkest step of the legend. Built in: "More".
    public var more: String?
    /// The legend step for cells without data. Built in: "No data".
    public var noData: String?
    /// The legend range of the lightest positive step: `%1$@` the first cut point. Built in: "Under %1$@".
    public var rangeBelow: String?
    /// The legend range of a middle step: `%1$@` from, `%2$@` to. Built in: "%1$@ to %2$@".
    public var range: String?
    /// The legend range of the darkest step: `%1$@` the last cut point. Built in: "%1$@ or more".
    public var rangeAbove: String?
    /// The size of the matrix in the summary: `%1$@` the rows, `%2$@` the columns, each with its noun.
    /// Built in: "%1$@ by %2$@".
    public var size: String?
    /// One row: `%1$@` the count. Built in: "%1$@ row".
    public var rowCountOne: String?
    /// Several rows: `%1$@` the count. Built in: "%1$@ rows".
    public var rowCount: String?
    /// One column: `%1$@` the count. Built in: "%1$@ column".
    public var columnCountOne: String?
    /// Several columns: `%1$@` the count. Built in: "%1$@ columns".
    public var columnCount: String?
    /// The highest cell in the summary: `%1$@` the cell as VoiceOver reads it. Built in: "Highest: %1$@".
    public var highest: String?
    /// What VoiceOver reads for a cell: `%1$@` the row, `%2$@` the column, `%3$@` the value.
    /// Built in: "%1$@, %2$@: %3$@".
    public var cell: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
        rowHeader: String? = nil,
        less: String? = nil,
        more: String? = nil,
        noData: String? = nil,
        rangeBelow: String? = nil,
        range: String? = nil,
        rangeAbove: String? = nil,
        size: String? = nil,
        rowCountOne: String? = nil,
        rowCount: String? = nil,
        columnCountOne: String? = nil,
        columnCount: String? = nil,
        highest: String? = nil,
        cell: String? = nil
    ) {
        self.frame = frame
        self.rowHeader = rowHeader
        self.less = less
        self.more = more
        self.noData = noData
        self.rangeBelow = rangeBelow
        self.range = range
        self.rangeAbove = rangeAbove
        self.size = size
        self.rowCountOne = rowCountOne
        self.rowCount = rowCount
        self.columnCountOne = columnCountOne
        self.columnCount = columnCount
        self.highest = highest
        self.cell = cell
    }
}
