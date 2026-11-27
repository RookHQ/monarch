/// One heading in a ``MonarchPageOutline``.
public struct MonarchPageOutlineHeading: Sendable, Hashable, Identifiable {
    /// A stable id, such as the id the heading has in a `ScrollViewReader`.
    public let id: String
    /// The heading level, 1 through 6.
    public let level: Int
    /// The heading text, already localised.
    public let text: String

    /// A heading.
    ///
    /// - Parameters:
    ///   - text: The heading text.
    ///   - level: The heading level, 1 through 6. Values outside are clamped.
    ///   - id: A stable id, used to scroll to the heading.
    public init(_ text: String, level: Int, id: String) {
        self.text = text
        self.level = min(max(level, 1), 6)
        self.id = id
    }
}
