/// Replacements for the built-in strings of ``MonarchAiAttachmentTray``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiAttachmentTrayLabels: Sendable {
    /// The spoken name of the tray. Built in: "Attachments".
    public var label: String?
    /// Announced when an upload finishes, from the file name. Built in: "report.pdf uploaded".
    public var ready: (@Sendable (String) -> String)?
    /// Announced after a file is removed with its button, from the file name. Built in: "report.pdf removed".
    public var removed: (@Sendable (String) -> String)?
    /// The strings of each file's chip: remove, retry, uploading and failed.
    public var chip: MonarchAttachmentChipLabels

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - label: The spoken name of the tray.
    ///   - ready: Announced when an upload finishes.
    ///   - removed: Announced after a file is removed with its button.
    ///   - chip: The strings of each file's chip.
    public init(
        label: String? = nil,
        ready: (@Sendable (String) -> String)? = nil,
        removed: (@Sendable (String) -> String)? = nil,
        chip: MonarchAttachmentChipLabels = MonarchAttachmentChipLabels()
    ) {
        self.label = label
        self.ready = ready
        self.removed = removed
        self.chip = chip
    }
}
