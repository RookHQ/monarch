/// Replacements for the built-in strings of ``MonarchTableToolbar``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchTableToolbarLabels: Sendable {
    /// The spoken name of the toolbar. Built in: "Table controls".
    public var toolbar: String?
    /// The result count, shown and announced. Built in: "1 result", "1,204 results".
    public var results: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(toolbar: String? = nil, results: (@Sendable (Int) -> String)? = nil) {
        self.toolbar = toolbar
        self.results = results
    }
}
