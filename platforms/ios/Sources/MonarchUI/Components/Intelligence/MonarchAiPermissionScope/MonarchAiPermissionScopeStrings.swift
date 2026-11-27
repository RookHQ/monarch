/// Built-in strings of the permission scope, from the `MonarchAiPermissionScope` strings table.
enum MonarchAiPermissionScopeStrings {
    private static let table = "MonarchAiPermissionScope"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiPermissionScope." + key, table: table)
    }

    /// The spoken name of the list.
    static var label: String { string("label") }
    /// The title of the manage button.
    static var manage: String { string("manage") }
    /// Shown when there are no apps.
    static var empty: String { string("empty") }

    /// The full description of a level.
    static func access(_ level: MonarchAiAccessLevel) -> String {
        switch level {
        case .none: string("access.none")
        case .read: string("access.read")
        case .write: string("access.write")
        }
    }

    /// The short name of a level on the control.
    static func option(_ level: MonarchAiAccessLevel) -> String {
        switch level {
        case .none: string("option.none")
        case .read: string("option.read")
        case .write: string("option.write")
        }
    }

    /// The spoken name of a row's control.
    static func control(app: String) -> String {
        MonarchStrings.string("monarch.aiPermissionScope.control", table: table, app)
    }
}
