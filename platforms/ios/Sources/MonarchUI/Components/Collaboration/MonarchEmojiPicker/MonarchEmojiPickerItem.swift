/// One emoji offered by ``MonarchEmojiPicker``.
public struct MonarchEmojiPickerItem: Sendable, Hashable, Identifiable {
    /// The emoji, inserted as it is.
    public let emoji: String
    /// The name, read by VoiceOver and used by search, such as "thumbs up".
    public let name: String
    /// More words that find the emoji, such as "like" and "yes".
    public let keywords: [String]
    /// Whether the emoji takes a skin tone. Only for emoji whose first
    /// character is a person or a body part.
    public let acceptsSkinTone: Bool

    /// The emoji itself.
    public var id: String { emoji }

    /// An emoji.
    ///
    /// - Parameters:
    ///   - emoji: The emoji, inserted as it is.
    ///   - name: The name, already localised.
    ///   - keywords: More words that find it, already localised.
    ///   - acceptsSkinTone: Whether it takes a skin tone.
    public init(_ emoji: String, name: String, keywords: [String] = [], acceptsSkinTone: Bool = false) {
        self.emoji = emoji
        self.name = name
        self.keywords = keywords
        self.acceptsSkinTone = acceptsSkinTone
    }
}
