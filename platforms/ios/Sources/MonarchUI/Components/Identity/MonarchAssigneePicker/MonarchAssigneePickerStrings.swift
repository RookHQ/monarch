/// Built-in strings of ``MonarchAssigneePicker``, from the `MonarchAssigneePicker` strings table.
enum MonarchAssigneePickerStrings {
    private static let table = "MonarchAssigneePicker"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.assigneePicker.\(key)", table: table)
    }

    /// The row that clears the assignee.
    static var unassigned: String { string("unassigned") }
    /// The shortcut that assigns the current user.
    static var assignToMe: String { string("assignToMe") }
    /// Announced after Assign to me.
    static var assigned: String { string("assigned") }
    /// The search field's placeholder.
    static var search: String { string("search") }
    /// "Ana Silva (you)".
    static func you(_ name: String) -> String {
        MonarchStrings.string("monarch.assigneePicker.you", table: table, name)
    }
}
