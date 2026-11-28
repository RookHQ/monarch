/// Replacements for the built-in strings of ``MonarchBreadcrumb``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchBreadcrumbLabels: Sendable {
    /// The spoken name of the trail. Built in: "Breadcrumb".
    public var label: String?
    /// The spoken name of the button that shows the collapsed levels. Receives the number of
    /// levels, already formatted. Built in: "Show 3 more levels".
    public var more: (@Sendable (String) -> String)?
    /// Read as the value of the last level. Built in: "Current page".
    public var current: String?

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - label: The spoken name of the trail.
    ///   - more: The spoken name of the collapsed-levels button, from the formatted count.
    ///   - current: Read as the value of the last level.
    public init(
        label: String? = nil,
        more: (@Sendable (String) -> String)? = nil,
        current: String? = nil
    ) {
        self.label = label
        self.more = more
        self.current = current
    }
}
