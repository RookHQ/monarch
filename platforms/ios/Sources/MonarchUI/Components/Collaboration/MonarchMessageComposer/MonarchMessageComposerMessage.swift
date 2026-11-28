/// What ``MonarchMessageComposer`` sends.
public struct MonarchMessageComposerMessage: Sendable, Hashable {
    /// The text, without leading or trailing white space. Mentions are
    /// written as "@Label". Empty when only attachments are sent.
    public let text: String
    /// The suggestions picked from the mention picker that are still in
    /// the text, in the order they were picked.
    public let mentions: [MonarchMentionItem]
    /// The selected mode's value, when the composer has modes.
    public let mode: String?

    /// A message.
    ///
    /// - Parameters:
    ///   - text: The text.
    ///   - mentions: The mentions in the text.
    ///   - mode: The selected mode's value.
    public init(text: String, mentions: [MonarchMentionItem] = [], mode: String? = nil) {
        self.text = text
        self.mentions = mentions
        self.mode = mode
    }
}
