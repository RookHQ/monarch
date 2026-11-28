/// Built-in strings of the inline edit text, from the `MonarchInlineEditText` strings table.
enum MonarchInlineEditTextStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.inlineEditText.\(key)", table: "MonarchInlineEditText")
    }

    /// The hint of the text in view mode.
    static var edit: String { string("edit") }
    /// The title of the save button.
    static var save: String { string("save") }
    /// The title of the cancel button.
    static var cancel: String { string("cancel") }
    /// The spoken value of the save button while saving.
    static var saving: String { string("saving") }
    /// The message of a failed save that has no description of its own.
    static var saveFailed: String { string("saveFailed") }

    /// The hint of the field while it has an error.
    static func error(_ message: String) -> String {
        MonarchStrings.string("monarch.inlineEditText.error", table: "MonarchInlineEditText", message)
    }
}
