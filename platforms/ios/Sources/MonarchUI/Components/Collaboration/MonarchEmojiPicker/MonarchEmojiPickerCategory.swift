/// A group of emoji in ``MonarchEmojiPicker``, with a button in the
/// category bar.
public struct MonarchEmojiPickerCategory: Sendable, Hashable, Identifiable {
    /// A stable id. The built-in ids (`smileys`, `people`, `animals`,
    /// `food`, `travel`, `activities`, `objects`, `symbols`, `flags`) have
    /// built-in names.
    public let id: String
    /// The name, already localised. Default: the built-in name of the id,
    /// then the id.
    public let label: String?
    /// The emoji on the category button. Default: the first emoji.
    public let symbol: String?
    /// The emoji, in order.
    public let emojis: [MonarchEmojiPickerItem]

    /// A category.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - label: The name, already localised.
    ///   - symbol: The emoji on the category button.
    ///   - emojis: The emoji, in order.
    public init(id: String, label: String? = nil, symbol: String? = nil, emojis: [MonarchEmojiPickerItem]) {
        self.id = id
        self.label = label
        self.symbol = symbol
        self.emojis = emojis
    }

    /// Monarch's compact built-in set: about 500 common emoji in nine
    /// categories, with English names and keywords. Pass a full dataset
    /// instead when the app needs every emoji or names in another language.
    public static let defaults: [MonarchEmojiPickerCategory] = MonarchEmojiPickerData.categories.map { category in
        MonarchEmojiPickerCategory(
            id: category.id,
            symbol: category.symbol,
            emojis: category.rows.split(separator: "\n").compactMap(MonarchEmojiPickerTokens.item(fromRow:))
        )
    }
}
