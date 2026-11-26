/// One card of a ``MonarchRadioCardGroup``.
public struct MonarchRadioCardOption<Value: Hashable>: Identifiable {
    /// The value the selection takes when the card is chosen. Unique within the group.
    public let value: Value
    /// The heading of the card, already localised. It is the card's spoken name.
    public let title: String
    /// Supporting text under the title.
    public let description: String?
    /// A decorative SF Symbol at the start of the card.
    public let systemImage: String?
    /// A short detail at the end of the title row, such as a price.
    public let meta: String?
    /// Whether the card cannot be chosen.
    public let isDisabled: Bool
    /// Short secondary lines under the description, such as what a role can do. Each is read after the description.
    public let details: [String]

    /// The identity of the card, which is its value.
    public var id: Value { value }

    /// A card.
    ///
    /// - Parameters:
    ///   - value: The value the selection takes when the card is chosen.
    ///   - title: The heading of the card, already localised.
    ///   - description: Supporting text under the title.
    ///   - systemImage: A decorative SF Symbol at the start of the card.
    ///   - meta: A short detail at the end of the title row, such as a price.
    ///   - isDisabled: Whether the card cannot be chosen.
    ///   - details: Short secondary lines under the description, such as what a role can do.
    public init(
        _ value: Value,
        title: String,
        description: String? = nil,
        systemImage: String? = nil,
        meta: String? = nil,
        isDisabled: Bool = false,
        details: [String] = []
    ) {
        self.value = value
        self.title = title
        self.description = description
        self.systemImage = systemImage
        self.meta = meta
        self.isDisabled = isDisabled
        self.details = details
    }
}

extension MonarchRadioCardOption: Sendable where Value: Sendable {}
