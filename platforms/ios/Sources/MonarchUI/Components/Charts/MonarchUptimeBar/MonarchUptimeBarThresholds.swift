/// The cut points used when a ``MonarchUptimeBarSegment`` has an uptime but no status.
public struct MonarchUptimeBarThresholds: Sendable, Hashable {
    /// At or above: operational. 99.9 by default.
    public let operational: Double
    /// At or above, and below `operational`: degraded. Below: down. 99 by default.
    public let degraded: Double

    /// Cut points, in percent.
    ///
    /// - Parameters:
    ///   - operational: At or above: operational.
    ///   - degraded: At or above, and below `operational`: degraded. Below: down.
    public init(operational: Double = 99.9, degraded: Double = 99) {
        self.operational = operational
        self.degraded = degraded
    }
}
