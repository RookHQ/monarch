/// Replacements for the built-in strings of ``MonarchRecoveryCodes``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchRecoveryCodesLabels: Sendable {
    /// The spoken name of the code list. Built in: "Recovery codes".
    public var listLabel: String?
    /// The count line once a code is used, from the codes left and the total. Built in: "8 of 10 codes left".
    public var remaining: (@Sendable (Int, Int) -> String)?
    /// Read after a used code. Built in: "Used".
    public var used: String?
    /// The copy button. Built in: "Copy codes".
    public var copy: String?
    /// The copy button after copying. Built in: "Copied".
    public var copied: String?
    /// The button that opens the share sheet, where the codes can be saved to Files. Built in: "Save or share".
    public var save: String?
    /// The button that saves the codes as a text file through the system file exporter. Built in: "Save to Files".
    public var saveToFiles: String?
    /// Announced after the file is saved. Built in: "Recovery codes saved."
    public var saved: String?
    /// Announced when the file could not be saved. Built in: "Couldn’t save the recovery codes."
    public var saveFailed: String?
    /// The button that shows the codes. Built in: "Show codes".
    public var reveal: String?
    /// The button that hides the codes. Built in: "Hide codes".
    public var conceal: String?
    /// Shown instead of the codes while they are hidden.
    public var concealedHint: String?
    /// The regenerate button. Built in: "Generate new codes".
    public var regenerate: String?
    /// The title of the regenerate confirmation. Built in: "Generate new recovery codes?"
    public var regenerateTitle: String?
    /// The message of the regenerate confirmation.
    public var regenerateDescription: String?
    /// The confirming button of the regenerate confirmation. Built in: "Generate new codes".
    public var regenerateConfirm: String?
    /// The cancel button of the regenerate confirmation. Built in: "Cancel".
    public var cancel: String?
    /// Announced after new codes arrive.
    public var regenerated: String?
    /// The acknowledgement checkbox. Built in: "I have saved my recovery codes".
    public var acknowledge: String?
    /// Shown when Continue is pressed before the checkbox is ticked.
    public var acknowledgeMissing: String?
    /// The button that finishes the step. Built in: "Continue".
    public var continueTitle: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        listLabel: String? = nil,
        remaining: (@Sendable (Int, Int) -> String)? = nil,
        used: String? = nil,
        copy: String? = nil,
        copied: String? = nil,
        save: String? = nil,
        saveToFiles: String? = nil,
        saved: String? = nil,
        saveFailed: String? = nil,
        reveal: String? = nil,
        conceal: String? = nil,
        concealedHint: String? = nil,
        regenerate: String? = nil,
        regenerateTitle: String? = nil,
        regenerateDescription: String? = nil,
        regenerateConfirm: String? = nil,
        cancel: String? = nil,
        regenerated: String? = nil,
        acknowledge: String? = nil,
        acknowledgeMissing: String? = nil,
        continueTitle: String? = nil
    ) {
        self.listLabel = listLabel
        self.remaining = remaining
        self.used = used
        self.copy = copy
        self.copied = copied
        self.save = save
        self.saveToFiles = saveToFiles
        self.saved = saved
        self.saveFailed = saveFailed
        self.reveal = reveal
        self.conceal = conceal
        self.concealedHint = concealedHint
        self.regenerate = regenerate
        self.regenerateTitle = regenerateTitle
        self.regenerateDescription = regenerateDescription
        self.regenerateConfirm = regenerateConfirm
        self.cancel = cancel
        self.regenerated = regenerated
        self.acknowledge = acknowledge
        self.acknowledgeMissing = acknowledgeMissing
        self.continueTitle = continueTitle
    }
}
