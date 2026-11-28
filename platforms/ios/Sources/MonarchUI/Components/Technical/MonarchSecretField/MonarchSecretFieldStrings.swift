/// Built-in strings of the secret field, from the `MonarchSecretField` strings table.
enum MonarchSecretFieldStrings {
    private static let table = "MonarchSecretField"

    /// The spoken name of the reveal toggle.
    static func show(_ label: String) -> String {
        MonarchStrings.string("monarch.secretField.show", table: table, label)
    }

    /// The spoken name of the copy button.
    static func copy(_ label: String) -> String {
        MonarchStrings.string("monarch.secretField.copy", table: table, label)
    }

    /// The spoken name of the copy button after a copy.
    static var copied: String { MonarchStrings.string("monarch.secretField.copied", table: table) }

    /// Announced after a copy.
    static var announcement: String { MonarchStrings.string("monarch.secretField.announcement", table: table) }

    /// The placeholder while a saved value is not shown.
    static var setPrompt: String { MonarchStrings.string("monarch.secretField.setPrompt", table: table) }

    /// The hint while a saved value is not shown.
    static var setDescription: String { MonarchStrings.string("monarch.secretField.setDescription", table: table) }

    /// The spoken value of a masked read-only secret.
    static var hidden: String { MonarchStrings.string("monarch.secretField.hidden", table: table) }

    /// The spoken form of an error message.
    static func error(_ message: String) -> String {
        MonarchStrings.string("monarch.secretField.error", table: table, message)
    }
}
