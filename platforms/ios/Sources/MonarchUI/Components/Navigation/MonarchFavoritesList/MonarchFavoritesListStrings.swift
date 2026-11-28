/// Built-in strings of the favorites list, from the `MonarchFavoritesList` strings table.
enum MonarchFavoritesListStrings {
    private static let table = "MonarchFavoritesList"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.favoritesList.\(key)", table: table)
    }

    /// The heading.
    static var title: String { string("title") }
    /// The visible title of the edit button.
    static var edit: String { string("edit") }
    /// The spoken name of the edit button.
    static var editLabel: String { string("editLabel") }
    /// The button that stops editing.
    static var done: String { string("done") }
    /// The removal announcement, with `%@` for the name.
    static var removed: String { string("removed") }
    /// The accessibility action that moves a row up.
    static var moveUp: String { string("moveUp") }
    /// The accessibility action that moves a row down.
    static var moveDown: String { string("moveDown") }
}
