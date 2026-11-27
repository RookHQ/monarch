/// Built-in strings of the unsaved-changes guard, from the `MonarchUnsavedChangesGuard` strings table.
enum MonarchUnsavedChangesGuardStrings {
    private static let table = "MonarchUnsavedChangesGuard"

    /// The question.
    static var title: String { MonarchStrings.string("monarch.unsavedChangesGuard.title", table: table) }
    /// What happens on leaving.
    static var message: String { MonarchStrings.string("monarch.unsavedChangesGuard.message", table: table) }
    /// The safe choice.
    static var keepEditing: String { MonarchStrings.string("monarch.unsavedChangesGuard.keepEditing", table: table) }
    /// The destructive choice.
    static var discard: String { MonarchStrings.string("monarch.unsavedChangesGuard.discard", table: table) }
    /// The save choice.
    static var save: String { MonarchStrings.string("monarch.unsavedChangesGuard.save", table: table) }
    /// Shown when saving fails without a readable reason.
    static var saveFailed: String { MonarchStrings.string("monarch.unsavedChangesGuard.saveFailed", table: table) }
    /// The guarding back button.
    static var back: String { MonarchStrings.string("monarch.unsavedChangesGuard.back", table: table) }
    /// Shown while saving.
    static var saving: String { MonarchStrings.string("monarch.unsavedChangesGuard.saving", table: table) }
}
