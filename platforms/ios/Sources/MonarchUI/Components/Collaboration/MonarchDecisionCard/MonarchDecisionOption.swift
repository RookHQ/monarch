/// One choice in a ``MonarchDecisionCard``, such as Keep, Review or Let lapse.
public struct MonarchDecisionOption: Sendable, Hashable, Identifiable {
    /// The stored value, passed to `onDecide`. Unique within a card.
    public let value: String
    /// The short label on the choice and on the read-only badge.
    public let label: String
    /// One line under the label.
    public let description: String?
    /// Shown under the choices while this one is selected: what choosing it
    /// will do.
    public let note: String?
    /// The colour of the badge in the read-only view.
    public let tone: MonarchTone
    /// Whether this choice needs a reason. `nil` follows the card.
    public let isReasonRequired: Bool?
    /// Whether the choice cannot be chosen.
    public let isDisabled: Bool

    /// The identity of the option, which is its value.
    public var id: String { value }

    /// A choice.
    ///
    /// - Parameters:
    ///   - label: The short label, already localised.
    ///   - value: The stored value. Unique within a card.
    ///   - description: One line under the label.
    ///   - note: What choosing it will do, shown while it is selected.
    ///   - tone: The colour of the badge in the read-only view.
    ///   - isReasonRequired: Whether this choice needs a reason. `nil`
    ///     follows the card.
    ///   - isDisabled: Whether the choice cannot be chosen.
    public init(
        _ label: String,
        value: String,
        description: String? = nil,
        note: String? = nil,
        tone: MonarchTone = .neutral,
        isReasonRequired: Bool? = nil,
        isDisabled: Bool = false
    ) {
        self.label = label
        self.value = value
        self.description = description
        self.note = note
        self.tone = tone
        self.isReasonRequired = isReasonRequired
        self.isDisabled = isDisabled
    }
}
