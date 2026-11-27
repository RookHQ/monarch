/// Which hardware-keyboard shortcut sends from a ``MonarchMessageComposer``.
///
/// The on-screen keyboard's Return key always adds a line; the send button
/// sends. Command-Return always sends from a hardware keyboard.
public enum MonarchMessageComposerSendShortcut: Sendable, Hashable, CaseIterable {
    /// Return sends and Shift-Return adds a line, as in chat apps.
    case returnKey
    /// Only Command-Return sends; Return adds a line, as in email and tickets.
    case commandReturn
}
