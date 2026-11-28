/// The corner shape of a favicon.
public enum MonarchFaviconShape: Sendable, Hashable, CaseIterable {
    /// 4-point corners. The default.
    case rounded
    /// A circle.
    case circle
    /// Square corners.
    case square
}
