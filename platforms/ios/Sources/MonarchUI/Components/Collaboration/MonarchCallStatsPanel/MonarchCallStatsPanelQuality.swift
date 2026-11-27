/// The connection grade of one person in a ``MonarchCallStatsPanel``.
public enum MonarchCallStatsPanelQuality: Sendable, Hashable, CaseIterable {
    /// A good connection.
    case good
    /// An unstable connection.
    case fair
    /// A poor connection.
    case poor
    /// Not measured yet. The default.
    case unknown
}
