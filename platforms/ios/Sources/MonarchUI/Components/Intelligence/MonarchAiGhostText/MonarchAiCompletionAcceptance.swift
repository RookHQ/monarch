/// What accepting a suggestion, or one word of it, changes.
public struct MonarchAiCompletionAcceptance: Sendable, Hashable {
    /// The text that was accepted and goes in at the cursor.
    public let inserted: String
    /// The whole text after the insertion.
    public let text: String
    /// The cursor after the insertion, as a UTF-16 offset into `text`.
    public let cursor: Int

    /// An acceptance.
    ///
    /// - Parameters:
    ///   - inserted: The text that was accepted.
    ///   - text: The whole text after the insertion.
    ///   - cursor: The cursor after the insertion, as a UTF-16 offset into `text`.
    public init(inserted: String, text: String, cursor: Int) {
        self.inserted = inserted
        self.text = text
        self.cursor = cursor
    }
}
