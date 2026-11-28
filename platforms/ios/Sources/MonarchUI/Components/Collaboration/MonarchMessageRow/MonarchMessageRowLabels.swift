/// Replacements for the built-in strings of ``MonarchMessageRow``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchMessageRowLabels: Sendable {
    /// Shown while the message is pending. Built in: "Sending…".
    public var sending: String?
    /// Shown when the message failed without an error message. Built in: "Not sent".
    public var failed: String?
    /// The retry button of a failed message. Built in: "Retry".
    public var retry: String?
    /// The discard button of a failed message. Built in: "Discard".
    public var discard: String?
    /// The edited marker. Built in: "(edited)".
    public var edited: String?
    /// The VoiceOver action that opens the edit history. Built in: "Show edit history".
    public var editHistory: String?
    /// The body of a deleted message. Built in: "This message was deleted."
    public var deleted: String?
    /// The thread button. Receives the reply count. Built in: "1 reply", "4 replies".
    public var replies: (@Sendable (Int) -> String)?
    /// Replacements for the strings of the reaction bar.
    public var reactions: MonarchReactionBarLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        sending: String? = nil,
        failed: String? = nil,
        retry: String? = nil,
        discard: String? = nil,
        edited: String? = nil,
        editHistory: String? = nil,
        deleted: String? = nil,
        replies: (@Sendable (Int) -> String)? = nil,
        reactions: MonarchReactionBarLabels = MonarchReactionBarLabels()
    ) {
        self.sending = sending
        self.failed = failed
        self.retry = retry
        self.discard = discard
        self.edited = edited
        self.editHistory = editHistory
        self.deleted = deleted
        self.replies = replies
        self.reactions = reactions
    }
}
