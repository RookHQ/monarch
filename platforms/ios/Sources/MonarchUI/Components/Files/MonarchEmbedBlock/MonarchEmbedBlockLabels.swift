/// Replacements for the built-in strings of ``MonarchEmbedBlock``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchEmbedBlockLabels: Sendable {
    /// The spoken name of the web view when there is no title or caption.
    /// Receives the provider's name. Built in: "YouTube content".
    public var frameTitle: (@Sendable (String) -> String)?
    /// The button that loads the content when loading waits for a tap.
    /// Receives the provider's name. Built in: "Load YouTube content".
    public var load: (@Sendable (String) -> String)?
    /// Shown for a URL that cannot be shown. Built in: "This link can't be shown."
    public var invalid: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        frameTitle: (@Sendable (String) -> String)? = nil,
        load: (@Sendable (String) -> String)? = nil,
        invalid: String? = nil
    ) {
        self.frameTitle = frameTitle
        self.load = load
        self.invalid = invalid
    }
}
