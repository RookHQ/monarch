/// The kind of a ``MonarchEmbedResolution``.
public enum MonarchEmbedResolutionKind: Sendable, Hashable, CaseIterable {
    /// Shown in a web view.
    case frame
    /// Shown as a link card.
    case link
    /// Not shown.
    case invalid
}
