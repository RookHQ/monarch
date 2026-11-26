import SwiftUI

/// One template in a ``MonarchTemplatePicker``, or its "start from scratch" card.
public struct MonarchTemplatePickerItem: Sendable, Identifiable {
    /// The stable id, written to the picker's selection.
    public let id: String
    /// The short name people see, already localised.
    public let name: String
    /// One or two lines on what it is for.
    public let description: String?
    /// The id of a ``MonarchTemplatePickerCategory``.
    public let category: String?
    /// A short marker such as "New", shown before the description.
    public let badge: String?
    /// An SF Symbol shown on the preview when there is no image.
    public let systemImage: String?
    /// A preview image drawn by the app.
    public let image: Image?
    /// More words the search matches, besides the name and description.
    public let keywords: [String]
    /// Whether the template is shown but cannot be chosen.
    public let isDisabled: Bool

    /// A template.
    ///
    /// - Parameters:
    ///   - id: The stable id.
    ///   - name: The short name, already localised.
    ///   - description: One or two lines on what it is for.
    ///   - category: The id of its category.
    ///   - badge: A short marker such as "New".
    ///   - systemImage: An SF Symbol for the preview.
    ///   - image: A preview image. It takes the place of the symbol.
    ///   - keywords: More words the search matches.
    ///   - isDisabled: Whether it is shown but cannot be chosen.
    public init(
        id: String,
        name: String,
        description: String? = nil,
        category: String? = nil,
        badge: String? = nil,
        systemImage: String? = nil,
        image: Image? = nil,
        keywords: [String] = [],
        isDisabled: Bool = false
    ) {
        self.id = id
        self.name = name
        self.description = description
        self.category = category
        self.badge = badge
        self.systemImage = systemImage
        self.image = image
        self.keywords = keywords
        self.isDisabled = isDisabled
    }

    /// The "start from scratch" card, shown first whatever the filters.
    ///
    /// - Parameters:
    ///   - name: The name. Built in: "Blank".
    ///   - description: One line on what it gives.
    ///   - id: The id written to the selection.
    /// - Returns: An item with a plus symbol and no category, so no filter hides it.
    public static func blank(name: String? = nil, description: String? = nil, id: String = "blank") -> Self {
        Self(
            id: id,
            name: name ?? MonarchTemplatePickerStrings.blank,
            description: description,
            systemImage: MonarchTemplatePickerTokens.blankSymbol
        )
    }
}
