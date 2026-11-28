import Foundation

/// A dated checkpoint of a ``MonarchGanttTimeline``, drawn as a diamond.
@_spi(Experimental) public struct MonarchGanttTimelineMilestone: Identifiable, Hashable, Sendable {
    /// A stable id, unique among the milestones.
    public var id: String
    /// The plain-text name, read by VoiceOver and written in the list.
    public var label: String
    /// Any time on the milestone's day, read in the environment's calendar.
    public var date: Date
    /// Whether the milestone is reached. Done milestones are filled.
    public var isDone: Bool
    /// The id of the item whose row shows the diamond, or `nil` for the
    /// milestone lane under the header.
    public var itemID: String?

    /// A milestone.
    ///
    /// - Parameters:
    ///   - id: A stable id, unique among the milestones.
    ///   - label: The plain-text name.
    ///   - date: Any time on the milestone's day.
    ///   - isDone: Whether the milestone is reached.
    ///   - itemID: The item whose row shows it, or `nil` for the lane.
    public init(id: String, label: String, date: Date, isDone: Bool = false, itemID: String? = nil) {
        self.id = id
        self.label = label
        self.date = date
        self.isDone = isDone
        self.itemID = itemID
    }
}
