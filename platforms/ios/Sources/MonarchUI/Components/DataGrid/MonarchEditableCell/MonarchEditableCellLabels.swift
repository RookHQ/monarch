/// Replacements for the built-in strings of ``MonarchEditableCell``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchEditableCellLabels: Sendable {
    /// The hint of the cell in view mode. Built in: "Edit".
    public var edit: String?
    /// The spoken value while a save runs. Built in: "Saving".
    public var saving: String?
    /// The name of the button that leaves the editor. Built in: "Cancel".
    public var cancel: String?
    /// The name of the button that saves the editor. Built in: "Save".
    public var save: String?
    /// The message of a number draft that is not a number. Built in: "Enter a number.".
    public var invalidNumber: String?
    /// The message of an empty required cell. Built in: "Enter a value.".
    public var required: String?
    /// The message of a value below the minimum. Receives the formatted minimum.
    /// Built in: "Enter 1 or more.".
    public var belowMinimum: (@Sendable (String) -> String)?
    /// The message of a value above the maximum. Receives the formatted maximum.
    /// Built in: "Enter 99 or less.".
    public var aboveMaximum: (@Sendable (String) -> String)?
    /// The choice that clears a select cell that is not required, and the text of
    /// an empty date cell without a placeholder. Built in: "None".
    public var noValue: String?
    /// The message of a failed save whose error has no description. Built in:
    /// "Couldn’t save. Try again.".
    public var saveFailed: String?
    /// The spoken hint of an editor with an error. Receives the message. Built in: "Error: …".
    public var error: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - edit: The hint of the cell in view mode.
    ///   - saving: The spoken value while a save runs.
    ///   - cancel: The name of the button that leaves the editor.
    ///   - save: The name of the button that saves the editor.
    ///   - invalidNumber: The message of a draft that is not a number.
    ///   - required: The message of an empty required cell.
    ///   - belowMinimum: The message of a value below the minimum, from the formatted minimum.
    ///   - aboveMaximum: The message of a value above the maximum, from the formatted maximum.
    ///   - noValue: The choice that clears a select cell, and the text of an empty date cell.
    ///   - saveFailed: The message of a failed save without a description.
    ///   - error: The spoken hint of an editor with an error, from the message.
    public init(
        edit: String? = nil,
        saving: String? = nil,
        cancel: String? = nil,
        save: String? = nil,
        invalidNumber: String? = nil,
        required: String? = nil,
        belowMinimum: (@Sendable (String) -> String)? = nil,
        aboveMaximum: (@Sendable (String) -> String)? = nil,
        noValue: String? = nil,
        saveFailed: String? = nil,
        error: (@Sendable (String) -> String)? = nil
    ) {
        self.edit = edit
        self.saving = saving
        self.cancel = cancel
        self.save = save
        self.invalidNumber = invalidNumber
        self.required = required
        self.belowMinimum = belowMinimum
        self.aboveMaximum = aboveMaximum
        self.noValue = noValue
        self.saveFailed = saveFailed
        self.error = error
    }
}
