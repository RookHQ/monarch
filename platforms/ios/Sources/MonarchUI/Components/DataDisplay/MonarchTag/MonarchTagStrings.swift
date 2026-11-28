/// Built-in strings of the tag, from the `MonarchTag` strings table.
enum MonarchTagStrings {
    /// The spoken name of the remove button of a tag.
    static func remove(_ text: String) -> String {
        MonarchStrings.string("monarch.tag.remove", table: "MonarchTag", text)
    }
}
