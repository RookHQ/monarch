/// Which contrast level a Monarch theme uses.
public enum MonarchContrastPreference: Sendable, Hashable, CaseIterable {
    /// Follow the Increase Contrast accessibility setting.
    case system
    /// Always the standard themes.
    case standard
    /// Always the high-contrast themes.
    case more
}
