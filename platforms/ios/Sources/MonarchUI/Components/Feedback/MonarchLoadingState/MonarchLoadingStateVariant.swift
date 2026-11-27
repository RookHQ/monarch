/// The placeholder layout of a loading state.
public enum MonarchLoadingStateVariant: Sendable, Hashable, CaseIterable {
    /// Rows with an avatar and two lines of text. The default.
    case list
    /// Rows with one line of text and a short value, for tables.
    case rows
    /// A grid of card blocks.
    case cards
    /// A title and paragraphs, for a detail page.
    case detail
    /// A spinner with the label shown under it.
    case spinner
}
