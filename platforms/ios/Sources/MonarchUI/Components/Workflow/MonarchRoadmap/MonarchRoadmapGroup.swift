import Foundation

/// A heading row of a ``MonarchRoadmap`` that groups projects, such as an
/// initiative, a theme or a team. The heading says how many projects it has
/// and, when given, its health.
@_spi(Experimental) public struct MonarchRoadmapGroup: Identifiable, Hashable, Sendable {
    /// A stable id, matched by ``MonarchRoadmapItem/groupID``.
    public var id: String
    /// The plain-text heading.
    public var label: String
    /// The first day of an optional span drawn as a thin bar on the group row, or `nil`.
    public var start: Date?
    /// The last day of that span, inclusive, or `nil`.
    public var end: Date?
    /// The health last reported for the group, or `nil` to write none.
    public var health: MonarchRoadmapHealth?

    /// A group.
    ///
    /// - Parameters:
    ///   - id: A stable id, matched by the items' `groupID`.
    ///   - label: The plain-text heading.
    ///   - start: The first day of the group's span, or `nil`.
    ///   - end: The last day of the group's span, inclusive, or `nil`.
    ///   - health: The health last reported, or `nil` to write none.
    public init(
        id: String,
        label: String,
        start: Date? = nil,
        end: Date? = nil,
        health: MonarchRoadmapHealth? = nil
    ) {
        self.id = id
        self.label = label
        self.start = start
        self.end = end
        self.health = health
    }
}
