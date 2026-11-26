/// How much room a card leaves around its content.
public enum MonarchCardPadding: Sendable, Hashable, CaseIterable {
    /// The card's padding on every side. The default.
    case standard
    /// No padding, for content that runs to the edges: a list, an image, a
    /// table. The card still clips it to its rounded corners.
    case flush
}
