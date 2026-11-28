/// The outline of an avatar.
public enum MonarchAvatarShape: Sendable, Hashable, CaseIterable {
    /// A circle, for people. The default.
    case circle
    /// A rounded square, for workspaces, organisations and brands.
    case square
}
