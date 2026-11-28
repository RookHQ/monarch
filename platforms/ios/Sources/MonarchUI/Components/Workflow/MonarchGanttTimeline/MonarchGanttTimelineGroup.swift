import Foundation

/// A heading row of a ``MonarchGanttTimeline``. Items with a matching
/// `groupID` are listed under it.
@_spi(Experimental) public struct MonarchGanttTimelineGroup: Identifiable, Hashable, Sendable {
    /// A stable id, unique among the groups.
    public var id: String
    /// The plain-text heading.
    public var label: String
    /// The group's first day, or `nil`. With dates the group row shows a thin span.
    public var start: Date?
    /// The group's last day, inclusive, or `nil`.
    public var end: Date?
    /// A short extra text under the heading.
    public var meta: String?

    /// A group.
    ///
    /// - Parameters:
    ///   - id: A stable id, unique among the groups.
    ///   - label: The plain-text heading.
    ///   - start: The first day, or `nil`.
    ///   - end: The last day, inclusive, or `nil`.
    ///   - meta: A short extra text under the heading.
    public init(id: String, label: String, start: Date? = nil, end: Date? = nil, meta: String? = nil) {
        self.id = id
        self.label = label
        self.start = start
        self.end = end
        self.meta = meta
    }
}
