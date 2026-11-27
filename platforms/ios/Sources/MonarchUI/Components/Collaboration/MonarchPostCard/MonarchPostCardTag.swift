/// A marker on a ``MonarchPostCard`` after the author line, such as the
/// category or a review status.
public struct MonarchPostCardTag: Identifiable, Sendable, Hashable {
    /// The text, already localised.
    public let label: String
    /// The meaning, which picks the badge colours.
    public let tone: MonarchTone

    /// The label, which is unique on a card.
    public var id: String { label }

    /// A marker.
    ///
    /// - Parameters:
    ///   - label: The text, already localised.
    ///   - tone: The meaning, which picks the badge colours.
    public init(_ label: String, tone: MonarchTone = .neutral) {
        self.label = label
        self.tone = tone
    }
}
