/// Replacements for the built-in strings of ``MonarchPageOutline``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchPageOutlineLabels: Sendable {
    /// The title above the list and the name of the outline. Built in: "On this page".
    public var title: String?
    /// Shown when there are no headings. Built in: "Headings you add appear here."
    public var empty: String?
    /// The stats line. Built in: "1,234 words · 6 min read".
    public var stats: (@Sendable (MonarchPageOutlineStats) -> String)?
    /// Read after the heading of the section being read. Built in: "Current section".
    public var current: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        title: String? = nil,
        empty: String? = nil,
        stats: (@Sendable (MonarchPageOutlineStats) -> String)? = nil,
        current: String? = nil
    ) {
        self.title = title
        self.empty = empty
        self.stats = stats
        self.current = current
    }
}
