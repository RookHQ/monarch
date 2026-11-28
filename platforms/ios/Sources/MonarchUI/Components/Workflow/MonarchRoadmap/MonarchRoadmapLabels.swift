/// Replacements for the built-in strings of ``MonarchRoadmap``. A property
/// that is `nil` keeps the built-in English string. The timeline's own
/// strings are replaced with ``MonarchGanttTimelineLabels``.
@_spi(Experimental) public struct MonarchRoadmapLabels: Sendable {
    /// The words of a health. Built in: "On track", "At risk", "Off track", "No health reported".
    public var health: (@Sendable (MonarchRoadmapHealth) -> String)?
    /// The number of projects in a group heading. Built in: "1 project", "3 projects".
    public var count: (@Sendable (Int) -> String)?
    /// Joins two parts of a row heading's second line. Built in: "8/20 · On track".
    public var meta: (@Sendable (_ first: String, _ second: String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - health: The words of a health.
    ///   - count: The number of projects in a group heading.
    ///   - meta: Joins two parts of a row heading's second line.
    public init(
        health: (@Sendable (MonarchRoadmapHealth) -> String)? = nil,
        count: (@Sendable (Int) -> String)? = nil,
        meta: (@Sendable (_ first: String, _ second: String) -> String)? = nil
    ) {
        self.health = health
        self.count = count
        self.meta = meta
    }
}
