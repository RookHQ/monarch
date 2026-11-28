/// The size of a favicon before Dynamic Type is applied.
public enum MonarchFaviconSize: Sendable, Hashable, CaseIterable {
    /// 16 points. Beside small text and in lists.
    case sm
    /// 20 points. Beside body text.
    case md
    /// 24 points. Standalone.
    case lg
}
