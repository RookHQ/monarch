/// The size of a button. The touch target is at least 44 points at every size.
public enum MonarchButtonSize: Sendable, Hashable, CaseIterable {
    /// 28 points high, for a row action or a bucket action. Added after the first three: a `switch` over
    /// `MonarchButtonSize` in an app needs a case for it, or a `default`.
    case xs
    /// 32 points high.
    case sm
    /// 36 points high.
    case md
    /// 40 points high.
    case lg
}
