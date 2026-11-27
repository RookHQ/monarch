/// Replacements for the built-in strings of ``MonarchBulkActionBar``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchBulkActionBarLabels: Sendable {
    /// The visible count, also announced. Receives the count and the total,
    /// if known. Built in: "3 selected", or "3 of 120 selected".
    public var selected: (@Sendable (Int, Int?) -> String)?
    /// Announced when the selection is emptied. Built in: "Selection cleared".
    public var cleared: String?
    /// The clear button. Built in: "Clear selection".
    public var clear: String?
    /// The overflow menu button. Built in: "More".
    public var more: String?
    /// The spoken name of the bar. Built in: "Bulk actions".
    public var bar: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        selected: (@Sendable (Int, Int?) -> String)? = nil,
        cleared: String? = nil,
        clear: String? = nil,
        more: String? = nil,
        bar: String? = nil
    ) {
        self.selected = selected
        self.cleared = cleared
        self.clear = clear
        self.more = more
        self.bar = bar
    }
}
