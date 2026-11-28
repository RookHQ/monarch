/// The largest width of a gauge. It shrinks with its container.
public enum MonarchGaugeSize: Sendable, Hashable, CaseIterable {
    /// 120 points wide.
    case sm
    /// 160 points wide. The default.
    case md
    /// 200 points wide.
    case lg
}
