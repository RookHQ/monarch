/// Built-in strings of the form field, from the `MonarchFormField` strings table.
enum MonarchFormFieldStrings {
    /// Spoken in the hint of a required field.
    static var required: String {
        MonarchStrings.string("monarch.formField.required", table: "MonarchFormField")
    }

    /// Spoken in the hint of a read-only field.
    static var readOnly: String {
        MonarchStrings.string("monarch.formField.readOnly", table: "MonarchFormField")
    }

    /// The spoken form of an error message.
    static func error(_ message: String) -> String {
        MonarchStrings.string("monarch.formField.error", table: "MonarchFormField", message)
    }
}
