/// How a ``MonarchThemeToggle`` shows the three choices.
public enum MonarchThemeToggleVariant: Sendable, Hashable, CaseIterable {
    /// Three visible segments, for a settings screen. The default.
    case segmented
    /// One button that opens a menu with the three choices, for a toolbar or an account menu.
    case menu
}
