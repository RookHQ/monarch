/// Built-in strings of ``MonarchFileList``, from the `MonarchFileList` strings table.
enum MonarchFileListStrings {
    private static let table = "MonarchFileList"

    /// The label of the select-all checkbox.
    static var selectAll: String { MonarchStrings.string("monarch.fileList.selectAll", table: table) }
    /// The title shown when there are no files.
    static var empty: String { MonarchStrings.string("monarch.fileList.empty", table: table) }
}
