/// Built-in strings of the tag input, from the `MonarchTagInput` strings table.
enum MonarchTagInputStrings {
    /// Shown when a tag is already in the list.
    static func duplicate(_ tag: String) -> String {
        MonarchStrings.string("monarch.tagInput.duplicate", table: "MonarchTagInput", tag)
    }

    /// Shown when the list is full.
    static func maximum(_ count: Int) -> String {
        MonarchStrings.string("monarch.tagInput.maximum", table: "MonarchTagInput", count)
    }

    /// Announced after a tag is added.
    static func added(_ tag: String) -> String {
        MonarchStrings.string("monarch.tagInput.added", table: "MonarchTagInput", tag)
    }

    /// Announced after a tag is removed.
    static func removed(_ tag: String) -> String {
        MonarchStrings.string("monarch.tagInput.removed", table: "MonarchTagInput", tag)
    }

    /// The spoken name of a suggestion button.
    static func addSuggestion(_ tag: String) -> String {
        MonarchStrings.string("monarch.tagInput.addSuggestion", table: "MonarchTagInput", tag)
    }
}
