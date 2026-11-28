/// The look of the "Sign in with Rook ID" button.
public enum MonarchSignInWithRookButtonVariant: Sendable, Hashable, CaseIterable {
    /// A light surface with a border, the same on every screen whatever its theme. The default.
    case light
    /// A dark surface with a border, the same on every screen whatever its theme.
    case dark
    /// A light grey surface with no border, the same on every screen whatever its theme.
    case neutral
    /// Follows the theme of the screen.
    case auto
}
