/// The size of a ``MonarchThemeToggle``. The menu button has a 44-point touch target at both sizes; the segments keep the system control height.
public enum MonarchThemeToggleSize: Sendable, Hashable, CaseIterable {
    /// The regular system segmented control, or a small button.
    case sm
    /// The large system segmented control, or a medium button. The default.
    case md
}
