/// Replacements for the built-in strings of ``MonarchReactionBar``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchReactionBarLabels: Sendable {
    /// The spoken name of the bar. Built in: "Reactions".
    public var label: String?
    /// The spoken name of a chip. Receives the emoji's name and the count.
    /// Built in: "thumbs up, 3 reactions".
    public var reaction: (@Sendable (String, Int) -> String)?
    /// The add button. Built in: "Add reaction".
    public var add: String?
    /// The heading of who reacted, in VoiceOver's more content. Built in: "Reacted by".
    public var reactedBy: String?
    /// Replacements for the emoji picker opened by the add button.
    public var picker: MonarchEmojiPickerLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        reaction: (@Sendable (String, Int) -> String)? = nil,
        add: String? = nil,
        reactedBy: String? = nil,
        picker: MonarchEmojiPickerLabels = MonarchEmojiPickerLabels()
    ) {
        self.label = label
        self.reaction = reaction
        self.add = add
        self.reactedBy = reactedBy
        self.picker = picker
    }
}
