/// One mode of a ``MonarchMessageComposer``, such as a public reply or an
/// internal note. The modes are a segmented control above the text.
public struct MonarchMessageComposerMode: Identifiable, Sendable, Hashable {
    /// The value passed back in ``MonarchMessageComposerMessage/mode``.
    public let value: String
    /// The segment's title, already localised.
    public let label: String
    /// Gives the composer the private-note look: the warning tint.
    public let isNote: Bool
    /// The placeholder while this mode is selected.
    public let placeholder: String?
    /// The send button's title while this mode is selected, such as "Add note".
    public let sendLabel: String?

    /// The value, which is unique among the modes.
    public var id: String { value }

    /// A mode.
    ///
    /// - Parameters:
    ///   - value: The value passed back with the message.
    ///   - label: The segment's title.
    ///   - isNote: Gives the composer the private-note look.
    ///   - placeholder: The placeholder while selected.
    ///   - sendLabel: The send button's title while selected.
    public init(
        _ value: String,
        label: String,
        isNote: Bool = false,
        placeholder: String? = nil,
        sendLabel: String? = nil
    ) {
        self.value = value
        self.label = label
        self.isNote = isNote
        self.placeholder = placeholder
        self.sendLabel = sendLabel
    }
}
