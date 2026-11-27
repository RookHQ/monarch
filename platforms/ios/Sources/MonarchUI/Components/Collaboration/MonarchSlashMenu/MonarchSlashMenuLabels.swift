/// Replacements for the built-in strings of ``MonarchSlashMenu``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchSlashMenuLabels: Sendable {
    /// The spoken name of the list. Built in: "Insert block".
    public var label: String?
    /// Shown when nothing matches. Receives what was typed, with the
    /// trigger. Built in: "Nothing matches “/xyz”".
    public var empty: (@Sendable (String) -> String)?
    /// Announced when the list changes. Receives the count. Built in:
    /// "1 option", "4 options".
    public var count: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        empty: (@Sendable (String) -> String)? = nil,
        count: (@Sendable (Int) -> String)? = nil
    ) {
        self.label = label
        self.empty = empty
        self.count = count
    }
}
