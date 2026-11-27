/// One emoji reaction with its count, for ``MonarchReactionBar``.
public struct MonarchReactionBarItem: Identifiable, Sendable, Hashable {
    /// The emoji on the chip, such as "👍". Also the value passed back when the chip is toggled.
    public let emoji: String
    /// How many people reacted. A reaction with a count of 0 is not shown.
    public let count: Int
    /// Whether the person using the app reacted. The chip shows selected.
    public let isReacted: Bool
    /// The spoken name of the emoji, such as "thumbs up". Default: the name
    /// in the emoji picker's built-in index, or the emoji itself.
    public let label: String?
    /// Who reacted, such as "Ana, Ben and 3 others". Shown on pointer hover
    /// and offered to VoiceOver as more content.
    public let reactedBy: String?

    /// The emoji, which is unique within a bar.
    public var id: String { emoji }

    /// A reaction.
    ///
    /// - Parameters:
    ///   - emoji: The emoji on the chip.
    ///   - count: How many people reacted.
    ///   - isReacted: Whether the person using the app reacted.
    ///   - label: The spoken name of the emoji.
    ///   - reactedBy: Who reacted, already localised.
    public init(_ emoji: String, count: Int, isReacted: Bool = false, label: String? = nil, reactedBy: String? = nil) {
        self.emoji = emoji
        self.count = count
        self.isReacted = isReacted
        self.label = label
        self.reactedBy = reactedBy
    }
}
