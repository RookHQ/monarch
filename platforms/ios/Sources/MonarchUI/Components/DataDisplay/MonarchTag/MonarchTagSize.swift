/// The size of a tag. The touch target of a selectable or removable tag is
/// at least 44 points at every size.
public enum MonarchTagSize: Sendable, Hashable, CaseIterable {
    /// 24 points high. For dense rows.
    case sm
    /// 28 points high. The default.
    case md
}
