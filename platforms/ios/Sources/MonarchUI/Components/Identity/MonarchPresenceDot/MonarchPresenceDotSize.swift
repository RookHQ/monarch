/// The size of a presence dot before Dynamic Type is applied.
public enum MonarchPresenceDotSize: Sendable, Hashable, CaseIterable {
    /// 8 points. In lists and on small avatars.
    case sm
    /// 10 points. The default.
    case md
    /// 12 points.
    case lg
}
