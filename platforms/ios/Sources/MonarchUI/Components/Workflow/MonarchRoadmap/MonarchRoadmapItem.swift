import Foundation

/// One project (or epic, or release) on a ``MonarchRoadmap``.
@_spi(Experimental) public struct MonarchRoadmapItem: Identifiable, Hashable, Sendable {
    /// A stable id, unique among the items. Dependencies and milestones refer to it.
    public var id: String
    /// The plain-text name, shown in the row heading and on the bar and read by VoiceOver.
    public var label: String
    /// Any time on the start day, read in the environment's calendar, or `nil`.
    public var start: Date?
    /// Any time on the target day, inclusive, or `nil`. With neither date the
    /// project stays in its group as a row that says "No dates".
    public var end: Date?
    /// The id of the ``MonarchRoadmapGroup`` (initiative, theme or team) it belongs to.
    public var groupID: String?
    /// The completion from 0 to 1, drawn inside the bar and read as a percentage, or `nil`.
    public var progress: Double?
    /// A short progress text in the row heading, such as "12/20".
    public var progressText: String?
    /// The health last reported, written in the row heading, or `nil` when
    /// the roadmap does not track health for this project (nothing is written).
    public var health: MonarchRoadmapHealth?
    /// The bar colour. `nil` takes it from `health`: at risk is warning, off
    /// track is danger, anything else the accent colour.
    public var tone: MonarchTone?

    /// A project.
    ///
    /// - Parameters:
    ///   - id: A stable id, unique among the items.
    ///   - label: The plain-text name.
    ///   - start: The start day, or `nil`.
    ///   - end: The target day, inclusive, or `nil`.
    ///   - groupID: The group the project belongs to.
    ///   - progress: The completion from 0 to 1.
    ///   - progressText: A short progress text, such as "12/20".
    ///   - health: The health last reported, or `nil` when health is not tracked.
    ///   - tone: The bar colour; `nil` takes it from `health`.
    public init(
        id: String,
        label: String,
        start: Date? = nil,
        end: Date? = nil,
        groupID: String? = nil,
        progress: Double? = nil,
        progressText: String? = nil,
        health: MonarchRoadmapHealth? = nil,
        tone: MonarchTone? = nil
    ) {
        self.id = id
        self.label = label
        self.start = start
        self.end = end
        self.groupID = groupID
        self.progress = progress
        self.progressText = progressText
        self.health = health
        self.tone = tone
    }
}
