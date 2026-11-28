/// The diameter of a score ring before Dynamic Type is applied.
public enum MonarchScoreRingSize: Sendable, Hashable, CaseIterable {
    /// 48 points, with the number only. In a list row or a table cell.
    case sm
    /// 80 points, with "/ 100" under the number. The default.
    case md
    /// 120 points. The main figure of a card or a page.
    case lg
}
