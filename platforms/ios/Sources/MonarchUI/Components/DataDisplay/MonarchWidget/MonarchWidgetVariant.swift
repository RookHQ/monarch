/// The frame of a ``MonarchWidget``.
public enum MonarchWidgetVariant: Sendable, Hashable, CaseIterable {
    /// A card with a surface and a border. The default.
    case card
    /// No surface or border, for a widget inside another surface.
    case plain
}
