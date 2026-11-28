/// Built-in strings of the file type icon, from the `MonarchFileTypeIcon` strings table.
enum MonarchFileTypeIconStrings {
    /// The spoken name of a family.
    static func name(_ family: MonarchFileTypeFamily) -> String {
        MonarchStrings.string("monarch.fileTypeIcon.\(family.rawValue)", table: "MonarchFileTypeIcon")
    }
}
