/// One bucket of a ``MonarchBacklogPlanner``: an iteration, a sprint, a release or the backlog.
@_spi(Experimental) public struct MonarchBacklogBucket<Item: Identifiable>: Identifiable {
    /// A stable, unique id, passed back in moves.
    public let id: String
    /// The heading, already localised, such as "Sprint 14" or "Backlog".
    public var title: String
    /// Quiet text under the title, such as the dates.
    public var subtitle: String?
    /// A badge after the title, such as "Active".
    public var badge: String?
    /// The tone of the badge. By default ``MonarchTone/info``; the web draws "Active" with the accent.
    public var badgeTone: MonarchTone
    /// The points the bucket can hold. Shows a capacity meter and an over-capacity note.
    public var capacity: Int?
    /// The items, in priority order.
    public var items: [Item]
    /// Shown when the bucket has no items. By default "Nothing here yet."
    public var emptyMessage: String?
    /// The title of a button in the header, such as "Start". The planner reports a press through `onBucketAction`.
    public var actionTitle: String?

    /// A bucket.
    ///
    /// - Parameters:
    ///   - id: A stable, unique id.
    ///   - title: The heading, already localised.
    ///   - subtitle: Quiet text under the title.
    ///   - badge: A badge after the title.
    ///   - badgeTone: The tone of the badge.
    ///   - capacity: The points the bucket can hold.
    ///   - actionTitle: The title of a button in the header, such as "Start".
    ///   - items: The items, in priority order.
    ///   - emptyMessage: Shown when the bucket has no items.
    public init(
        id: String,
        title: String,
        subtitle: String? = nil,
        badge: String? = nil,
        badgeTone: MonarchTone = .info,
        capacity: Int? = nil,
        actionTitle: String? = nil,
        items: [Item],
        emptyMessage: String? = nil
    ) {
        self.id = id
        self.title = title
        self.subtitle = subtitle
        self.badge = badge
        self.badgeTone = badgeTone
        self.capacity = capacity
        self.items = items
        self.emptyMessage = emptyMessage
        self.actionTitle = actionTitle
    }
}

extension MonarchBacklogBucket: Equatable where Item: Equatable {}
extension MonarchBacklogBucket: Hashable where Item: Hashable {}
extension MonarchBacklogBucket: Sendable where Item: Sendable {}
