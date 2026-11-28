/// How much room an empty or error state takes.
public enum MonarchEmptyStateSize: Sendable, Hashable, CaseIterable {
    /// A tight block for a table body, a side panel or a small card.
    case sm
    /// A block inside a card or a list. The default.
    case md
    /// Fills an empty screen and centres itself in it.
    case lg
}
