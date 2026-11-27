/// A row of a ``MonarchAppearancePanel``.
public enum MonarchAppearancePanelRow: Sendable, Hashable, CaseIterable {
    /// Light, dark or system, with ``MonarchThemeToggle``.
    case theme
    /// Standard, more or system contrast.
    case contrast
    /// The brand accent, chosen from a menu.
    case accent
    /// System or reduced motion.
    case motion
    /// Comfortable or compact spacing.
    case density

    /// The rows the web panel shows, in its order: theme, contrast, motion
    /// and density. The accent row is added only when asked for.
    public static let standard: [MonarchAppearancePanelRow] = [.theme, .contrast, .motion, .density]
}
