/// Replacements for the built-in strings of ``MonarchKeyCreatedDialog``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchKeyCreatedDialogLabels: Sendable {
    /// The title when the dialog is given none. Built in: "Copy your new key".
    public var title: String?
    /// What the value is when the dialog is given no `label`. Built in: "API key".
    public var label: String?
    /// The heading of the warning for a secret. Built in: "Copy it now. It
    /// will not be shown again."
    public var secretNoticeTitle: String?
    /// The body of the warning for a secret.
    public var secretNotice: String?
    /// The heading of the notice for a public value. Built in: "You can view
    /// this again later."
    public var publicNoticeTitle: String?
    /// The body of the notice for a public value.
    public var publicNotice: String?
    /// The acknowledgement checkbox. Receives what the value is. Built in:
    /// "I have saved this API key somewhere safe."
    public var acknowledge: (@Sendable (String) -> String)?
    /// Shown and announced when Done is pressed too early. Built in:
    /// "Confirm that you have saved it before you close this dialog."
    public var acknowledgeRequired: String?
    /// The button that closes the sheet. Built in: "Done".
    public var done: String?
    /// The title of the copy button. Built in: "Copy".
    public var copy: String?
    /// The title of the copy button after a copy. Built in: "Copied".
    public var copied: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        label: String? = nil,
        secretNoticeTitle: String? = nil,
        secretNotice: String? = nil,
        publicNoticeTitle: String? = nil,
        publicNotice: String? = nil,
        acknowledge: (@Sendable (String) -> String)? = nil,
        acknowledgeRequired: String? = nil,
        done: String? = nil,
        copy: String? = nil,
        copied: String? = nil
    ) {
        self.title = title
        self.label = label
        self.secretNoticeTitle = secretNoticeTitle
        self.secretNotice = secretNotice
        self.publicNoticeTitle = publicNoticeTitle
        self.publicNotice = publicNotice
        self.acknowledge = acknowledge
        self.acknowledgeRequired = acknowledgeRequired
        self.done = done
        self.copy = copy
        self.copied = copied
    }

    var resolvedTitle: String { title ?? MonarchKeyCreatedDialogStrings.string("title") }
    var resolvedLabel: String { label ?? MonarchKeyCreatedDialogStrings.string("label") }
    var resolvedSecretNoticeTitle: String {
        secretNoticeTitle ?? MonarchKeyCreatedDialogStrings.string("secretNoticeTitle")
    }
    var resolvedSecretNotice: String { secretNotice ?? MonarchKeyCreatedDialogStrings.string("secretNotice") }
    var resolvedPublicNoticeTitle: String {
        publicNoticeTitle ?? MonarchKeyCreatedDialogStrings.string("publicNoticeTitle")
    }
    var resolvedPublicNotice: String { publicNotice ?? MonarchKeyCreatedDialogStrings.string("publicNotice") }
    var resolvedAcknowledgeRequired: String {
        acknowledgeRequired ?? MonarchKeyCreatedDialogStrings.string("acknowledgeRequired")
    }
    var resolvedDone: String { done ?? MonarchKeyCreatedDialogStrings.string("done") }
    var resolvedCopy: String { copy ?? MonarchKeyCreatedDialogStrings.string("copy") }
    var resolvedCopied: String { copied ?? MonarchKeyCreatedDialogStrings.string("copied") }

    func acknowledgeText(_ label: String) -> String {
        acknowledge?(label) ?? MonarchKeyCreatedDialogStrings.acknowledge(label)
    }
}
