/// Built-in strings of the permission matrix, from the `MonarchPermissionMatrix` strings table.
enum MonarchPermissionMatrixStrings {
    private static let table = "MonarchPermissionMatrix"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.permissionMatrix.\(key)", table: table)
    }

    /// The heading of the first column.
    static var permissionHeader: String { string("permissionHeader") }
    /// The spoken name of the role picker.
    static var rolePicker: String { string("rolePicker") }
    /// The spoken name of a cell.
    static func cellLabel(permission: String, role: String) -> String {
        MonarchStrings.string("monarch.permissionMatrix.cellLabel", table: table, permission, role)
    }
    /// Read for a granted cell.
    static var allowed: String { string("allowed") }
    /// Read for a cell that is not granted.
    static var notAllowed: String { string("notAllowed") }
    /// The default reason of a locked cell.
    static var locked: String { string("locked") }
}
