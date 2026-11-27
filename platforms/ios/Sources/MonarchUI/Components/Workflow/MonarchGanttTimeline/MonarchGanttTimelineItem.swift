import Foundation

/// One row of a ``MonarchGanttTimeline``: a task, issue, project or phase.
@_spi(Experimental) public struct MonarchGanttTimelineItem: Identifiable, Hashable, Sendable {
    /// A stable id, unique among the items.
    public var id: String
    /// The plain-text name, shown in the row heading and on the bar and read by VoiceOver.
    public var label: String
    /// Any time on the first day, read in the environment's calendar, or `nil`.
    /// With only one of `start` and `end` the item is a one-day marker; with
    /// neither it is unscheduled.
    public var start: Date?
    /// Any time on the last day, inclusive, or `nil`.
    public var end: Date?
    /// The id of the parent item, for sub-tasks. A parent can be collapsed;
    /// without dates of its own it shows a bracket over its children.
    public var parentID: String?
    /// The id of the ``MonarchGanttTimelineGroup`` the item is listed under.
    public var groupID: String?
    /// The completion from 0 to 1, drawn as a strip along the bottom of the
    /// bar and read as a percentage, or `nil` for none.
    public var progress: Double?
    /// The colour of the bar; `nil` is the accent colour. Pair a tone with
    /// words: colour is never the only signal.
    public var tone: MonarchTone?
    /// `false` locks this item's dates when the timeline is editable.
    public var isEditable: Bool
    /// A scheduling problem, such as "Starts before Design ends". The bar gets
    /// a dashed danger outline and the text is read with the bar and written
    /// in the list.
    public var conflict: String?
    /// A short extra text under the row heading, such as a status.
    public var meta: String?

    /// An item.
    ///
    /// - Parameters:
    ///   - id: A stable id, unique among the items.
    ///   - label: The plain-text name.
    ///   - start: The first day, or `nil`.
    ///   - end: The last day, inclusive, or `nil`.
    ///   - parentID: The parent item, for sub-tasks.
    ///   - groupID: The group the item is listed under.
    ///   - progress: The completion from 0 to 1.
    ///   - tone: The colour of the bar; `nil` is the accent colour.
    ///   - isEditable: `false` locks this item's dates.
    ///   - conflict: A scheduling problem in words.
    ///   - meta: A short extra text under the row heading.
    public init(
        id: String,
        label: String,
        start: Date? = nil,
        end: Date? = nil,
        parentID: String? = nil,
        groupID: String? = nil,
        progress: Double? = nil,
        tone: MonarchTone? = nil,
        isEditable: Bool = true,
        conflict: String? = nil,
        meta: String? = nil
    ) {
        self.id = id
        self.label = label
        self.start = start
        self.end = end
        self.parentID = parentID
        self.groupID = groupID
        self.progress = progress
        self.tone = tone
        self.isEditable = isEditable
        self.conflict = conflict
        self.meta = meta
    }

    /// The item's own dates.
    public var dates: MonarchGanttTimelineDates {
        MonarchGanttTimelineDates(start: start, end: end)
    }
}
