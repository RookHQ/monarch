/// Built-in strings of the saved views menu, from the `MonarchSavedViews` strings table.
enum MonarchSavedViewsStrings {
    private static let table = "MonarchSavedViews"

    /// The string for a key without placeholders, such as `noView`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.savedViews.\(key)", table: table)
    }

    /// "Save changes to “Open deals”".
    static func saveChanges(_ name: String) -> String {
        MonarchStrings.string("monarch.savedViews.saveChanges", table: table, name)
    }

    /// "Delete “Open deals”?".
    static func deleteTitle(_ name: String) -> String {
        MonarchStrings.string("monarch.savedViews.deleteTitle", table: table, name)
    }
}
