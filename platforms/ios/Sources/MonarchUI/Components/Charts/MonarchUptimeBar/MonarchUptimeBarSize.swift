/// The height of the segments of a ``MonarchUptimeBar``.
public enum MonarchUptimeBarSize: Sendable, Hashable, CaseIterable {
    /// 20 points, for tiles and rows.
    case sm
    /// 32 points, for status pages. The default.
    case md
}
