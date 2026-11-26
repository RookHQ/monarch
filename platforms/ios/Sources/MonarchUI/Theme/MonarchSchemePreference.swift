/// Which colour scheme a Monarch theme uses.
public enum MonarchSchemePreference: Sendable, Hashable, CaseIterable {
    /// Follow the device setting.
    case system
    /// Always light.
    case light
    /// Always dark.
    case dark
}
