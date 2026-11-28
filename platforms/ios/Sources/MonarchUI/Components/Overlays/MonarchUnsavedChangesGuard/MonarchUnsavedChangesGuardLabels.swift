/// Replacements for the built-in strings of ``MonarchUnsavedChangesGuard``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchUnsavedChangesGuardLabels: Sendable {
    /// The question. Built in: "Discard unsaved changes?".
    public var title: String?
    /// What happens on leaving. Built in: "Your changes have not been saved. If you leave now,
    /// they will be lost."
    public var message: String?
    /// The safe choice. Built in: "Keep editing".
    public var keepEditing: String?
    /// The destructive choice. Built in: "Discard changes".
    public var discard: String?
    /// Shown when there is a save action. Built in: "Save and leave".
    public var save: String?
    /// Shown when saving fails without a readable reason. Built in: "Your changes could not be
    /// saved. Try again."
    public var saveFailed: String?
    /// The back button that replaces the system one while there are changes. Built in: "Back".
    public var back: String?
    /// Shown and announced while saving. Built in: "Saving".
    public var saving: String?

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - title: The question.
    ///   - message: What happens on leaving.
    ///   - keepEditing: The safe choice.
    ///   - discard: The destructive choice.
    ///   - save: The save choice.
    ///   - saveFailed: Shown when saving fails without a readable reason.
    ///   - back: The guarding back button.
    ///   - saving: Shown and announced while saving.
    public init(
        title: String? = nil,
        message: String? = nil,
        keepEditing: String? = nil,
        discard: String? = nil,
        save: String? = nil,
        saveFailed: String? = nil,
        back: String? = nil,
        saving: String? = nil
    ) {
        self.title = title
        self.message = message
        self.keepEditing = keepEditing
        self.discard = discard
        self.save = save
        self.saveFailed = saveFailed
        self.back = back
        self.saving = saving
    }
}
