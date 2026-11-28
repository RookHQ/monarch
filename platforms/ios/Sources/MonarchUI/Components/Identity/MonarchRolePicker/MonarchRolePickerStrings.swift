/// Built-in strings of the role picker, from the `MonarchRolePicker` strings table.
enum MonarchRolePickerStrings {
    /// The label of the picker.
    static var label: String { MonarchStrings.string("monarch.rolePicker.label", table: "MonarchRolePicker") }
    /// The compact picker's text when no role is chosen.
    static var placeholder: String {
        MonarchStrings.string("monarch.rolePicker.placeholder", table: "MonarchRolePicker")
    }
}
