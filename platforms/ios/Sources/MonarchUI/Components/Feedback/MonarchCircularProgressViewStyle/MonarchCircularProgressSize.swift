/// The diameter of a progress ring before Dynamic Type is applied.
public enum MonarchCircularProgressSize: Sendable, Hashable, CaseIterable {
    /// 20 points. Beside a line of text.
    case sm
    /// 32 points. The default.
    case md
    /// 48 points. Large enough to show the value in the middle.
    case lg
    /// 72 points. The largest ring, with the value in the middle.
    case xl
}
