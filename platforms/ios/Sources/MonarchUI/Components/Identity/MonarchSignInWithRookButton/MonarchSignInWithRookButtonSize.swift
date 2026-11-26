/// The size of the "Sign in with Rook ID" button. The touch target is at least
/// 44 points at every size.
public enum MonarchSignInWithRookButtonSize: Sendable, Hashable, CaseIterable {
    /// 32 points high.
    case sm
    /// 40 points high. The default.
    case md
    /// 48 points high.
    case lg
}
