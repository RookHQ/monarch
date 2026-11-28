/// Built-in strings of ``MonarchEmojiPicker``, from the `MonarchEmojiPicker`
/// strings table.
enum MonarchEmojiPickerStrings {
    private static let table = "MonarchEmojiPicker"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.emojiPicker." + key, table: table)
    }

    /// The ids of the categories with a built-in name.
    static let categoryIDs: Set<String> = [
        "recent", "smileys", "people", "animals", "food", "travel", "activities", "objects", "symbols", "flags",
    ]

    /// The spoken name of the picker.
    static var label: String { string("label") }
    /// The search prompt.
    static var search: String { string("search") }
    /// The clear-search button.
    static var clearSearch: String { string("clearSearch") }
    /// The category bar.
    static var categoryBar: String { string("categoryBar") }
    /// The built-in name of a category id, or `nil` for another id.
    static func category(_ id: String) -> String? {
        categoryIDs.contains(id) ? string("category." + id) : nil
    }
    /// The heading of the search results.
    static var searchResults: String { string("searchResults") }
    /// Shown when nothing matches.
    static var noResults: String { string("noResults") }
    /// "1 emoji found", "12 emoji found".
    static func resultsCount(_ count: Int) -> String {
        count == 1
            ? string("resultsOne")
            : MonarchStrings.string("monarch.emojiPicker.resultsOther", table: table, count)
    }
    /// The skin tone menu.
    static var skinTone: String { string("skinTone") }
    /// The name of a skin tone.
    static func skinToneName(_ tone: MonarchEmojiSkinTone) -> String {
        switch tone {
        case .none: string("skinTone.none")
        case .light: string("skinTone.light")
        case .mediumLight: string("skinTone.mediumLight")
        case .medium: string("skinTone.medium")
        case .mediumDark: string("skinTone.mediumDark")
        case .dark: string("skinTone.dark")
        }
    }
}
