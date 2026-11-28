/// Replacements for the built-in strings of ``MonarchWorldMap``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchWorldMapLabels: Sendable {
    /// The strings of the chart frame: table toggle, loading, empty, error, legend and "No value".
    public var frame: MonarchChartFrameLabels
    /// The table heading of the country column. Built in: "Country".
    public var country: String?
    /// The table heading of the value column. Built in: "Value".
    public var value: String?
    /// The spoken name of the tiles under the map. Built in: "Other values".
    public var extras: String?
    /// Before the lightest legend step. Built in: "Less".
    public var less: String?
    /// After the darkest legend step. Built in: "More".
    public var more: String?
    /// The spoken range of the lightest positive step: `%1$@` the first cut point. Built in: "Under %1$@".
    public var rangeBelow: String?
    /// The spoken range of a middle step: `%1$@` and `%2$@` its bounds. Built in: "%1$@ to %2$@".
    public var range: String?
    /// The spoken range of the darkest step: `%1$@` the last cut point. Built in: "%1$@ or more".
    public var rangeAbove: String?
    /// The note for codes that are not on the map. Receives their names and their formatted total.
    /// Built in: "Not on the map: <names> (<total>)".
    public var unplaced: (@Sendable (String, String) -> String)?
    /// The generated summary. Receives the number of countries and the top three as text.
    /// Built in: "5 countries. Highest: United States 1,200, Germany 300, India 150."
    public var summary: (@Sendable (Int, String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
        country: String? = nil,
        value: String? = nil,
        extras: String? = nil,
        less: String? = nil,
        more: String? = nil,
        rangeBelow: String? = nil,
        range: String? = nil,
        rangeAbove: String? = nil,
        unplaced: (@Sendable (String, String) -> String)? = nil,
        summary: (@Sendable (Int, String) -> String)? = nil
    ) {
        self.frame = frame
        self.country = country
        self.value = value
        self.extras = extras
        self.less = less
        self.more = more
        self.rangeBelow = rangeBelow
        self.range = range
        self.rangeAbove = rangeAbove
        self.unplaced = unplaced
        self.summary = summary
    }
}
