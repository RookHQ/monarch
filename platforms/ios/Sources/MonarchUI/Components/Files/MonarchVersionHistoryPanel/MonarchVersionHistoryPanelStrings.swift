/// Built-in strings of ``MonarchVersionHistoryPanel``, from the `MonarchVersionHistoryPanel` strings table.
enum MonarchVersionHistoryPanelStrings {
    private static let table = "MonarchVersionHistoryPanel"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.versionHistoryPanel.\(key)", table: table)
    }

    /// The title of the version history panel.
    static var title: String { string("title") }
    /// The button that closes the version history panel.
    static var close: String { string("close") }
    /// The spoken name of the field where a version is named before saving.
    static var noteLabel: String { string("noteLabel") }
    /// The placeholder of the field where a version is named before saving.
    static var notePlaceholder: String { string("notePlaceholder") }
    /// The button that saves the current content as a version.
    static var save: String { string("save") }
    /// Announced after a version is saved.
    static var saved: String { string("saved") }
    /// Shown when saving a version fails.
    static var saveFailed: String { string("saveFailed") }
    /// Shown when there are no versions.
    static var empty: String { string("empty") }
    /// Marks the version that matches the content now.
    static var current: String { string("current") }
    /// Shown after the author of a version saved automatically.
    static var automatic: String { string("automatic") }
    /// The heading of the list of versions.
    static var versions: String { string("versions") }
    /// The tab that shows what changed between a version and now.
    static var changes: String { string("changes") }
    /// The tab that shows the whole text of a version.
    static var fullText: String { string("fullText") }
    /// The name of the content now, in the comparison with a version.
    static var now: String { string("now") }
    /// Shown when a version has no changes compared with now.
    static var identical: String { string("identical") }
    /// The button that replaces the content with the version.
    static var restore: String { string("restore") }
    /// The title of the confirmation before restoring a version.
    static var restoreTitle: String { string("restoreTitle") }
    /// The message of the confirmation before restoring a version.
    static var restoreDescription: String { string("restoreDescription") }
    /// The button in the confirmation that restores the version.
    static var restoreConfirm: String { string("restoreConfirm") }
    /// Announced after a version is restored.
    static var restored: String { string("restored") }
    /// Shown when restoring a version fails.
    static var restoreFailed: String { string("restoreFailed") }
    /// The spoken name of the placeholder shown while versions or their text load.
    static var loading: String { string("loading") }
    /// The name of the control that switches between the changes and the full text.
    static var view: String { string("view") }
}
