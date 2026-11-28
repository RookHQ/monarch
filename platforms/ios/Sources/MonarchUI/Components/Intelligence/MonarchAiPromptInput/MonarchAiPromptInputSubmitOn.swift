/// Which hardware-keyboard key sends a ``MonarchAiPromptInput``. The
/// on-screen keyboard's Return always adds a line; the Send button sends.
public enum MonarchAiPromptInputSubmitOn: Sendable, Hashable, CaseIterable {
    /// Return sends and Shift-Return adds a line. Command-Return also sends.
    case returnKey
    /// Only Command-Return sends; Return adds a line.
    case commandReturn
}
