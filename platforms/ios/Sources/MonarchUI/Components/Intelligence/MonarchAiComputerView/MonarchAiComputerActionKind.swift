/// What kind of thing an agent did in ``MonarchAiComputerView``; picks the icon and the spoken kind.
@_spi(Experimental) public enum MonarchAiComputerActionKind: String, Sendable, Hashable, CaseIterable {
    /// A click or tap.
    case click
    /// Typing text.
    case type
    /// Scrolling.
    case scroll
    /// Opening an address.
    case navigate
    /// A key press.
    case key
    /// Waiting.
    case wait
    /// Reading the screen.
    case read
    /// Anything else.
    case other
}
