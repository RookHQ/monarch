/// Built-in strings of the Space switcher, from the `MonarchSpaceSwitcher` strings table.
enum MonarchSpaceSwitcherStrings {
    private static let table = "MonarchSpaceSwitcher"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.spaceSwitcher.\(key)", table: table)
    }

    /// The spoken name of the button and the sheet title.
    static var label: String { string("label") }
    /// The all-spaces choice.
    static var all: String { string("all") }
    /// The button text when nothing is chosen and all spaces is not offered.
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
