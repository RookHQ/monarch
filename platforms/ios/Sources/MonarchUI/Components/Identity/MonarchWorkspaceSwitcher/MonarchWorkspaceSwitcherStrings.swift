/// Built-in strings of the workspace switcher, from the `MonarchWorkspaceSwitcher` strings table.
enum MonarchWorkspaceSwitcherStrings {
    private static let table = "MonarchWorkspaceSwitcher"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.workspaceSwitcher.\(key)", table: table)
    }

    /// The spoken name of the button and the sheet title.
    static var label: String { string("label") }
    /// The button text when nothing is chosen.
    static var placeholder: String { string("placeholder") }
    /// The search field prompt.
    static var search: String { string("search") }
    /// Shown when nothing matches the search.
    static var empty: String { string("empty") }
    /// Shown while loading.
    static var loading: String { string("loading") }
    /// The create action.
    static var create: String { string("create") }
    /// The manage link.
    static var manage: String { string("manage") }
    /// The cancel button.
    static var cancel: String { string("cancel") }
}
