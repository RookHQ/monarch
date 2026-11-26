/// Replacements for the built-in strings of ``MonarchVersionHistoryPanel``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchVersionHistoryPanelLabels: Sendable {
    /// The button that closes the version history panel. Built in: "Close"
    public var close: String?
    /// The spoken name of the field where a version is named before saving. Built in: "Version name"
    public var noteLabel: String?
    /// The placeholder of the field where a version is named before saving. Built in: "Name this version (optional)"
    public var notePlaceholder: String?
    /// The button that saves the current content as a version. Built in: "Save version"
    public var save: String?
    /// Announced after a version is saved. Built in: "Version saved"
    public var saved: String?
    /// Shown when saving a version fails. Built in: "The version could not be saved. Try again."
    public var saveFailed: String?
    /// Shown when there are no versions. Built in: "No versions yet. Save one to mark this moment."
    public var empty: String?
    /// Marks the version that matches the content now. Built in: "Current"
    public var current: String?
    /// Shown after the author of a version saved automatically. Built in: "Saved automatically"
    public var automatic: String?
    /// The heading of the list of versions. Built in: "Versions"
    public var versions: String?
    /// The tab that shows what changed between a version and now. Built in: "Changes"
    public var changes: String?
    /// The tab that shows the whole text of a version. Built in: "Full text"
    public var fullText: String?
    /// The name of the content now, in the comparison with a version. Built in: "Now"
    public var now: String?
    /// Shown when a version has no changes compared with now. Built in: "This version is identical to the current one."
    public var identical: String?
    /// The button that replaces the content with the version. Built in: "Restore this version"
    public var restore: String?
    /// The title of the confirmation before restoring a version. Built in: "Restore this version?"
    public var restoreTitle: String?
    /// The message of the confirmation before restoring a version. Built in: "The current content is replaced with this version."
    public var restoreDescription: String?
    /// The button in the confirmation that restores the version. Built in: "Restore"
    public var restoreConfirm: String?
    /// Announced after a version is restored. Built in: "Version restored"
    public var restored: String?
    /// Shown when restoring a version fails. Built in: "The version could not be restored. Try again."
    public var restoreFailed: String?
    /// The spoken name of the placeholder shown while versions or their text load. Built in: "Loading"
    public var loading: String?
    /// The name of the control that switches between the changes and the full text. Built in: "View"
    public var view: String?
    /// Replacements for the diff's built-in strings.
    public var diff: MonarchDiffViewerLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        close: String? = nil,
        noteLabel: String? = nil,
        notePlaceholder: String? = nil,
        save: String? = nil,
        saved: String? = nil,
        saveFailed: String? = nil,
        empty: String? = nil,
        current: String? = nil,
        automatic: String? = nil,
        versions: String? = nil,
        changes: String? = nil,
        fullText: String? = nil,
        now: String? = nil,
        identical: String? = nil,
        restore: String? = nil,
        restoreTitle: String? = nil,
        restoreDescription: String? = nil,
        restoreConfirm: String? = nil,
        restored: String? = nil,
        restoreFailed: String? = nil,
        loading: String? = nil,
        view: String? = nil,
        diff: MonarchDiffViewerLabels = MonarchDiffViewerLabels()
    ) {
        self.close = close
        self.noteLabel = noteLabel
        self.notePlaceholder = notePlaceholder
        self.save = save
        self.saved = saved
        self.saveFailed = saveFailed
        self.empty = empty
        self.current = current
        self.automatic = automatic
        self.versions = versions
        self.changes = changes
        self.fullText = fullText
        self.now = now
        self.identical = identical
        self.restore = restore
        self.restoreTitle = restoreTitle
        self.restoreDescription = restoreDescription
        self.restoreConfirm = restoreConfirm
        self.restored = restored
        self.restoreFailed = restoreFailed
        self.loading = loading
        self.view = view
        self.diff = diff
    }
}
