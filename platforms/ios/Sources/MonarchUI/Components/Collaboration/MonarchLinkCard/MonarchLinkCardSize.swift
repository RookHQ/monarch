/// The layout of a ``MonarchLinkCard``.
public enum MonarchLinkCardSize: Sendable, Hashable, CaseIterable {
    /// The picture as a small square beside the text, for threads and narrow places.
    case sm
    /// The picture as a banner above the text. The default, for the main timeline.
    case md
}
