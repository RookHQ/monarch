/// Built-in strings of the autofill field, from the `MonarchAiAutofillField` strings table.
enum MonarchAiAutofillFieldStrings {
    private static let table = "MonarchAiAutofillField"

    /// The badge text of a state.
    static func badge(_ state: MonarchAiAutofillState) -> String {
        MonarchStrings.string("monarch.aiAutofillField.badge.\(state.rawValue)", table: table)
    }

    /// The title of the accept button.
    static var accept: String { MonarchStrings.string("monarch.aiAutofillField.accept", table: table) }

    /// The title of the clear button.
    static var clear: String { MonarchStrings.string("monarch.aiAutofillField.clear", table: table) }

    /// The spoken name of the accept button.
    static func acceptField(_ field: String) -> String {
        MonarchStrings.string("monarch.aiAutofillField.acceptField", table: table, field)
    }

    /// The spoken name of the clear button.
    static func clearField(_ field: String) -> String {
        MonarchStrings.string("monarch.aiAutofillField.clearField", table: table, field)
    }

    /// The source line.
    static func source(_ source: String) -> String {
        MonarchStrings.string("monarch.aiAutofillField.source", table: table, source)
    }

    /// The line shown once edited.
    static func original(_ value: String) -> String {
        MonarchStrings.string("monarch.aiAutofillField.original", table: table, value)
    }

    /// Announced after Accept.
    static var accepted: String { MonarchStrings.string("monarch.aiAutofillField.accepted", table: table) }

    /// Announced after Clear.
    static var cleared: String { MonarchStrings.string("monarch.aiAutofillField.cleared", table: table) }
}
