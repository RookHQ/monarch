/// Replacements for the built-in strings of ``MonarchCalendarHeatmap``.
/// A property that is `nil` keeps the built-in English string.
///
/// Sentences with values are formats: `%1$@`, `%2$@`… stand for the values
/// named in each property's description, already formatted.
public struct MonarchCalendarHeatmapLabels: Sendable, Hashable {
    /// The strings of the chart frame: the table toggle, loading, empty,
    /// error, the legend's spoken name and the text of a day without data.
    public var frame: MonarchChartFrameLabels
    /// Before the lightest step of the legend. Built in: "Less".
    public var less: String?
    /// After the darkest step of the legend. Built in: "More".
    public var more: String?
    /// The legend step for days without data. Built in: "No data".
    public var noData: String?
    /// The legend range of the lightest positive step: `%1$@` the first cut point. Built in: "Under %1$@".
    public var rangeBelow: String?
    /// The legend range of a middle step: `%1$@` from, `%2$@` to. Built in: "%1$@ to %2$@".
    public var range: String?
    /// The legend range of the darkest step: `%1$@` the last cut point. Built in: "%1$@ or more".
    public var rangeAbove: String?
    /// The table heading of the dates. Built in: "Date".
    public var date: String?
    /// The table heading of the values, and the series VoiceOver reads. Built in: "Value".
    public var value: String?
    /// The table heading of the notes. Built in: "Note".
    public var note: String?
    /// Read after today's value. Built in: "Today".
    public var today: String?
    /// One day: `%1$@` the count. Built in: "%1$@ day".
    public var dayCountOne: String?
    /// Several days: `%1$@` the count. Built in: "%1$@ days".
    public var dayCount: String?
    /// The generated summary: `%1$@` the days with their noun, `%2$@` the
    /// first and `%3$@` the last day, `%4$@` the total. Built in:
    /// "%1$@ from %2$@ to %3$@. Total %4$@".
    public var summary: String?
    /// The highest day in the summary: `%1$@` the day as VoiceOver reads it.
    /// Built in: "Highest: %1$@".
    public var highest: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
        less: String? = nil,
        more: String? = nil,
        noData: String? = nil,
        rangeBelow: String? = nil,
        range: String? = nil,
        rangeAbove: String? = nil,
        date: String? = nil,
        value: String? = nil,
        note: String? = nil,
        today: String? = nil,
        dayCountOne: String? = nil,
        dayCount: String? = nil,
        summary: String? = nil,
        highest: String? = nil
    ) {
        self.frame = frame
        self.less = less
        self.more = more
        self.noData = noData
        self.rangeBelow = rangeBelow
        self.range = range
        self.rangeAbove = rangeAbove
        self.date = date
        self.value = value
        self.note = note
        self.today = today
        self.dayCountOne = dayCountOne
        self.dayCount = dayCount
        self.summary = summary
        self.highest = highest
    }
}
