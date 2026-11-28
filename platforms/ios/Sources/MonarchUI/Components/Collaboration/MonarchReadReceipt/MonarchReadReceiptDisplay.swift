/// What a ``MonarchReadReceipt`` shows. Every display reads the full list of
/// names to VoiceOver.
public enum MonarchReadReceiptDisplay: Sendable, Hashable, CaseIterable {
    /// A check mark and "Seen". The usual choice in a one-to-one conversation.
    case text
    /// A check mark and "Seen by 3", for groups.
    case count
    /// The readers' avatars and "Seen".
    case avatars
}
