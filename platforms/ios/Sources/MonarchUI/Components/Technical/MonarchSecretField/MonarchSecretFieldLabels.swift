/// Replacements for the built-in strings of ``MonarchSecretField``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchSecretFieldLabels: Sendable {
    /// The spoken name of the reveal toggle. Receives the field label.
    /// Built in: "Show <label>", the same in both states.
    public var show: (@Sendable (String) -> String)?
    /// The spoken name of the copy button. Receives the field label.
    /// Built in: "Copy <label>".
    public var copy: (@Sendable (String) -> String)?
    /// The spoken name of the copy button for a short time after a copy.
    /// Built in: "Copied".
    public var copied: String?
    /// Announced by VoiceOver after a copy. Built in: "Copied to clipboard".
    public var copiedAnnouncement: String?
    /// The placeholder of a field whose value is saved but not shown, while
    /// it is empty. Built in: "Saved. Enter a new value to replace it".
    public var setPrompt: String?
    /// Spoken in the hint of a field whose value is saved but not shown.
    /// Built in: "A value is saved and hidden. Leave blank to keep it."
    public var setDescription: String?
    /// The spoken value of a read-only secret while it is masked. Built in:
    /// "Hidden".
    public var hidden: String?
    /// The spoken form of an error message. Receives the message. Built in:
    /// "Error: <message>".
    public var error: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        show: (@Sendable (String) -> String)? = nil,
        copy: (@Sendable (String) -> String)? = nil,
        copied: String? = nil,
        copiedAnnouncement: String? = nil,
        setPrompt: String? = nil,
        setDescription: String? = nil,
        hidden: String? = nil,
        error: (@Sendable (String) -> String)? = nil
    ) {
        self.show = show
        self.copy = copy
        self.copied = copied
        self.copiedAnnouncement = copiedAnnouncement
        self.setPrompt = setPrompt
        self.setDescription = setDescription
        self.hidden = hidden
        self.error = error
    }

    /// The spoken name of the reveal toggle for a field label.
    func showName(_ label: String) -> String {
        show?(label) ?? MonarchSecretFieldStrings.show(label)
    }

    /// The spoken name of the copy button for a field label and state.
    func copyName(_ label: String, isCopied: Bool) -> String {
        if isCopied { return copied ?? MonarchSecretFieldStrings.copied }
        return copy?(label) ?? MonarchSecretFieldStrings.copy(label)
    }

    /// The announcement after a copy.
    var resolvedCopiedAnnouncement: String {
        copiedAnnouncement ?? MonarchSecretFieldStrings.announcement
    }

    /// The placeholder while a saved value is not shown.
    var resolvedSetPrompt: String { setPrompt ?? MonarchSecretFieldStrings.setPrompt }

    /// The hint while a saved value is not shown.
    var resolvedSetDescription: String { setDescription ?? MonarchSecretFieldStrings.setDescription }

    /// The spoken value of a masked read-only secret.
    var resolvedHidden: String { hidden ?? MonarchSecretFieldStrings.hidden }

    /// The spoken form of an error message.
    func errorText(_ message: String) -> String {
        error?(message) ?? MonarchSecretFieldStrings.error(message)
    }
}
