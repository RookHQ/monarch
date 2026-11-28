/// Replacements for the built-in strings of ``MonarchMessageComposer``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchMessageComposerLabels: Sendable {
    /// The send button. Built in: "Send".
    public var send: String?
    /// The send button while editing. Built in: "Save".
    public var save: String?
    /// The send button while sending. Built in: "Sending…".
    public var sending: String?
    /// The cancel button while editing. Built in: "Cancel".
    public var cancel: String?
    /// The attach button. Built in: "Attach files".
    public var attach: String?
    /// The emoji button. Built in: "Add emoji".
    public var emoji: String?
    /// The name of the mode control. Built in: "Message type".
    public var modes: String?
    /// The reply context. Receives the author. Built in: "Replying to Ana".
    public var replyingTo: (@Sendable (String) -> String)?
    /// The button that removes the reply context. Built in: "Stop replying".
    public var dismissReply: String?
    /// The edit context. Built in: "Editing message".
    public var editing: String?
    /// The spoken character count. Receives the characters left, below zero
    /// when over the limit. Built in: "12 characters left", "3 characters over the limit".
    public var remaining: (@Sendable (Int) -> String)?
    /// The draft states. Built in: "Saving draft…", "Draft saved", "Draft not saved".
    public var draftSaving: String?
    /// The draft saved state.
    public var draftSaved: String?
    /// The draft error state.
    public var draftError: String?
    /// The line shown under the composer when `onSend` throws an error that has no description of its own.
    /// Built in: "Message not sent. Try again."
    public var sendFailed: String?
    /// Replacements for the strings of the mention picker.
    public var mentionPicker: MonarchMentionPickerLabels
    /// Replacements for the strings of the emoji picker.
    public var emojiPicker: MonarchEmojiPickerLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        send: String? = nil,
        save: String? = nil,
        sending: String? = nil,
        cancel: String? = nil,
        attach: String? = nil,
        emoji: String? = nil,
        modes: String? = nil,
        replyingTo: (@Sendable (String) -> String)? = nil,
        dismissReply: String? = nil,
        editing: String? = nil,
        remaining: (@Sendable (Int) -> String)? = nil,
        draftSaving: String? = nil,
        draftSaved: String? = nil,
        draftError: String? = nil,
        sendFailed: String? = nil,
        mentionPicker: MonarchMentionPickerLabels = MonarchMentionPickerLabels(),
        emojiPicker: MonarchEmojiPickerLabels = MonarchEmojiPickerLabels()
    ) {
        self.send = send
        self.save = save
        self.sending = sending
        self.cancel = cancel
        self.attach = attach
        self.emoji = emoji
        self.modes = modes
        self.replyingTo = replyingTo
        self.dismissReply = dismissReply
        self.editing = editing
        self.remaining = remaining
        self.draftSaving = draftSaving
        self.draftSaved = draftSaved
        self.draftError = draftError
        self.sendFailed = sendFailed
        self.mentionPicker = mentionPicker
        self.emojiPicker = emojiPicker
    }
}
