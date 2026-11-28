/// The size of an avatar before Dynamic Type is applied.
///
/// `xs` and `xl` were added after the first three: a `switch` over
/// `MonarchAvatarSize` in an app needs a case for each, or a `default`.
public enum MonarchAvatarSize: Sendable, Hashable, CaseIterable {
    /// 20 points. Inline with caption text, such as a byline or a reaction.
    /// An avatar is not a touch target; give a tappable one 44 points of hit area.
    case xs
    /// 24 points. In dense lists and beside small text.
    case sm
    /// 32 points. The default.
    case md
    /// 40 points. In list rows with two lines and in headers.
    case lg
    /// 56 points. On a profile header or a call screen.
    case xl
}
