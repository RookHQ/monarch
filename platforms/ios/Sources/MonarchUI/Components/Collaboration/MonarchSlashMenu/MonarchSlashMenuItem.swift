/// One entry of a ``MonarchSlashMenu``, such as "Heading" or "Checklist".
public struct MonarchSlashMenuItem: Identifiable, Sendable, Hashable {
    /// A stable id.
    public let id: String
    /// The name shown in the menu, already localised.
    public let label: String
    /// What it inserts or does, shown under the label and read with it.
    public let description: String?
    /// An SF Symbol shown before the label. Decorative.
    public let systemImage: String?
    /// The heading the item sits under, such as "Text" or "Lists". Items
    /// without one form a group without a heading.
    public let group: String?
    /// Extra words the filter matches, such as `["h1", "title"]`.
    public let keywords: [String]
    /// Shown but cannot be chosen, for example an item that needs a selection.
    public let isDisabled: Bool

    /// A slash menu item.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - label: The name shown in the menu.
    ///   - description: What it inserts or does.
    ///   - systemImage: An SF Symbol shown before the label.
    ///   - group: The heading the item sits under.
    ///   - keywords: Extra words the filter matches.
    ///   - isDisabled: Shown but cannot be chosen.
    public init(
        id: String,
        label: String,
        description: String? = nil,
        systemImage: String? = nil,
        group: String? = nil,
        keywords: [String] = [],
        isDisabled: Bool = false
    ) {
        self.id = id
        self.label = label
        self.description = description
        self.systemImage = systemImage
        self.group = group
        self.keywords = keywords
        self.isDisabled = isDisabled
    }
}
