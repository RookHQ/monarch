/// Replacements for the spoken strings of ``MonarchProvenance``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchProvenanceLabels: Sendable {
    /// The spoken source. Receives the source. Built in: "Source: Registry".
    public var source: (@Sendable (String) -> String)?
    /// The spoken time. Receives the relative time. Built in:
    /// "checked 2 hours ago".
    public var checked: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        source: (@Sendable (String) -> String)? = nil,
        checked: (@Sendable (String) -> String)? = nil
    ) {
        self.source = source
        self.checked = checked
    }
}
