/// Replacements for the built-in strings of ``MonarchAiMemoryChip``. Every
/// value is optional; leave one out to keep the built-in English text.
public struct MonarchAiMemoryChipLabels: Sendable {
    /// The visible prefix of the chip for what happened to the memory:
    /// "Used memory:", "Remembered:" or "Forgotten:".
    public var prefix: @Sendable (_ action: MonarchAiMemoryAction) -> String
    /// The spoken name of the options menu, given a shortened copy of the text.
    public var options: @Sendable (_ text: String) -> String
    /// The title of the edit command.
    public var edit: String
    /// The title of the forget command.
    public var forget: String
    /// The title of the edit dialog.
    public var editTitle: String
    /// The name of the text field in the edit dialog.
    public var editField: String
    /// The title of the save button in the edit dialog.
    public var save: String
    /// The title of the cancel button in the edit dialog.
    public var cancel: String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - prefix: Replaces the prefix of each memory action.
    ///   - options: Replaces "Options for memory: …".
    ///   - edit: Replaces "Edit".
    ///   - forget: Replaces "Forget".
    ///   - editTitle: Replaces "Edit memory".
    ///   - editField: Replaces "Memory".
    ///   - save: Replaces "Save".
    ///   - cancel: Replaces "Cancel".
    public init(
        prefix: (@Sendable (_ action: MonarchAiMemoryAction) -> String)? = nil,
        options: (@Sendable (_ text: String) -> String)? = nil,
        edit: String? = nil,
        forget: String? = nil,
        editTitle: String? = nil,
        editField: String? = nil,
        save: String? = nil,
        cancel: String? = nil
    ) {
        self.prefix = prefix ?? { MonarchAiMemoryChipStrings.prefix($0) }
        self.options = options ?? { MonarchAiMemoryChipStrings.options($0) }
        self.edit = edit ?? MonarchAiMemoryChipStrings.edit
        self.forget = forget ?? MonarchAiMemoryChipStrings.forget
        self.editTitle = editTitle ?? MonarchAiMemoryChipStrings.editTitle
        self.editField = editField ?? MonarchAiMemoryChipStrings.editField
        self.save = save ?? MonarchAiMemoryChipStrings.save
        self.cancel = cancel ?? MonarchAiMemoryChipStrings.cancel
    }
}
