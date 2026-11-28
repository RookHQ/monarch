/// Replacements for the built-in strings of ``MonarchPostCard``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchPostCardLabels: Sendable {
    /// The pinned marker. Built in: "Pinned".
    public var pinned: String?
    /// The answered badge. Built in: "Answered".
    public var answered: String?
    /// The spoken reaction count. Built in: "1 reaction", "12 reactions".
    public var reactions: (@Sendable (Int) -> String)?
    /// The spoken comment count. Built in: "1 comment", "4 comments".
    public var comments: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        pinned: String? = nil,
        answered: String? = nil,
        reactions: (@Sendable (Int) -> String)? = nil,
        comments: (@Sendable (Int) -> String)? = nil
    ) {
        self.pinned = pinned
        self.answered = answered
        self.reactions = reactions
        self.comments = comments
    }
}
