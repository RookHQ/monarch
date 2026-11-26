/// Replacements for the built-in strings of ``MonarchEmojiPicker``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchEmojiPickerLabels: Sendable {
    /// The spoken name of the picker. Built in: "Emoji picker".
    public var label: String?
    /// The search field's prompt and spoken name. Built in: "Search emoji".
    public var search: String?
    /// The clear-search button. Built in: "Clear search".
    public var clearSearch: String?
    /// The spoken name of the category bar. Built in: "Emoji categories".
    public var categoryBar: String?
    /// The name of a category by id. Built in: "Recently used", "Smileys &
    /// emotion", "People & body" and so on.
    public var categoryName: (@Sendable (String) -> String?)?
    /// The heading of the search results. Built in: "Search results".
    public var searchResults: String?
    /// Shown when nothing matches. Built in: "No emoji found".
    public var noResults: String?
    /// Announced after a search. Receives the count. Built in: "1 emoji
    /// found", "12 emoji found" (English plural rule only).
    public var resultsCount: (@Sendable (Int) -> String)?
    /// The skin tone menu. Built in: "Skin tone".
    public var skinTone: String?
    /// The name of a skin tone. Built in: "Default", "Light", "Medium-light",
    /// "Medium", "Medium-dark", "Dark".
    public var skinToneName: (@Sendable (MonarchEmojiSkinTone) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        search: String? = nil,
        clearSearch: String? = nil,
        categoryBar: String? = nil,
        categoryName: (@Sendable (String) -> String?)? = nil,
        searchResults: String? = nil,
        noResults: String? = nil,
        resultsCount: (@Sendable (Int) -> String)? = nil,
        skinTone: String? = nil,
        skinToneName: (@Sendable (MonarchEmojiSkinTone) -> String)? = nil
    ) {
        self.label = label
        self.search = search
        self.clearSearch = clearSearch
        self.categoryBar = categoryBar
        self.categoryName = categoryName
        self.searchResults = searchResults
        self.noResults = noResults
        self.resultsCount = resultsCount
        self.skinTone = skinTone
        self.skinToneName = skinToneName
    }
}
