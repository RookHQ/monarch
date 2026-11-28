/// Built-in strings of the cursor list, from the `MonarchCursorList` strings table.
enum MonarchCursorListStrings {
    private static let table = "MonarchCursorList"

    /// The spoken name of the first-page placeholder.
    static var loading: String { MonarchStrings.string("monarch.cursorList.loading", table: table) }
    /// The title of the empty state.
    static var emptyTitle: String { MonarchStrings.string("monarch.cursorList.emptyTitle", table: table) }
    /// The message of the empty state.
    static var emptyMessage: String { MonarchStrings.string("monarch.cursorList.emptyMessage", table: table) }
    /// The title of the error state.
    static var errorTitle: String { MonarchStrings.string("monarch.cursorList.errorTitle", table: table) }
}
