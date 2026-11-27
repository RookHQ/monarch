/// Built-in strings of the key created dialog, from the `MonarchKeyCreatedDialog` strings table.
enum MonarchKeyCreatedDialogStrings {
    private static let table = "MonarchKeyCreatedDialog"

    /// A string without arguments.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.keyCreatedDialog.\(key)", table: table)
    }

    /// The acknowledgement for what the value is.
    static func acknowledge(_ label: String) -> String {
        MonarchStrings.string("monarch.keyCreatedDialog.acknowledge", table: table, label)
    }
}
