/// What a lightbox item is.
public enum MonarchLightboxItemKind: Sendable, Hashable, CaseIterable {
    /// A picture. It can be zoomed.
    case image
    /// A video, played in the system player. It cannot be zoomed.
    case video
}
