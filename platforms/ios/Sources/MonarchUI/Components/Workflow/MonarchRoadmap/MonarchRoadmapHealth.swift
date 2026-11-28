/// The health last reported for a project or an initiative on a ``MonarchRoadmap``.
@_spi(Experimental) public enum MonarchRoadmapHealth: String, Sendable, Hashable, CaseIterable {
    /// Going as planned. Written as "On track"; the bar keeps the accent colour.
    case onTrack
    /// Likely to slip. Written as "At risk"; the bar takes the warning tone.
    case atRisk
    /// Behind plan. Written as "Off track"; the bar takes the danger tone.
    case offTrack
    /// Health is tracked but nobody has reported it yet. Written as "No health reported".
    case notReported
}
