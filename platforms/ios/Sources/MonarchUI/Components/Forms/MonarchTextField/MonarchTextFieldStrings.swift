/// Built-in strings of the text field, from the `MonarchTextField` strings table.
enum MonarchTextFieldStrings {
    /// Spoken as the hint of a text field that has an error.
    static func error(_ message: String) -> String {
        MonarchStrings.string("monarch.textField.error", table: "MonarchTextField", message)
    }
}
