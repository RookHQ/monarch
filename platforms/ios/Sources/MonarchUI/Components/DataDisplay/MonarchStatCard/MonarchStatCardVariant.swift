/// The surface of a ``MonarchStatCard``.
public enum MonarchStatCardVariant: Sendable, Hashable, CaseIterable {
    /// A card with a border and the surface colour. The default.
    case card
    /// No surface or border, for a stat inside another surface.
    case plain
}
