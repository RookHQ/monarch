/// Replacements for the built-in strings of ``MonarchBacklogPlanner``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchBacklogPlannerLabels: Sendable {
    /// The header line, from the item count, the points (`nil` without estimates) and the number
    /// unestimated. Built in: "5 items · 13 points · 1 unestimated".
    public var summary: (@Sendable (_ count: Int, _ points: Int?, _ unestimated: Int) -> String)?
    /// The capacity meter text. Built in: "18 of 20 points".
    public var capacity: (@Sendable (_ points: Int, _ capacity: Int) -> String)?
    /// The spoken name of the capacity meter. Built in: "Capacity".
    public var capacityLabel: String?
    /// The over-capacity note, from the points over. Built in: "Planned work is over capacity by 3 points."
    public var overCapacity: (@Sendable (Int) -> String)?
    /// The points on a row. Built in: "3 pt".
    public var rowPoints: (@Sendable (Int) -> String)?
    /// The points on a row with no estimate. Built in: "–".
    public var rowUnestimated: String?
    /// The spoken points of a row with no estimate. Built in: "Not estimated".
    public var notEstimated: String?
    /// The spoken name of a row's move menu, from the item. Built in: "Move PROJ-1 to…".
    public var moveMenu: (@Sendable (String) -> String)?
    /// The heading of the buckets in the move menu. Built in: "Move to".
    public var moveTo: String?
    /// Built in: "Move up".
    public var moveUp: String?
    /// Built in: "Move down".
    public var moveDown: String?
    /// The VoiceOver action to another bucket, from its title. Built in: "Move to Sprint 14".
    public var moveToBucket: (@Sendable (String) -> String)?
    /// Shown in a bucket with no items and no message of its own. Built in: "Nothing here yet."
    public var empty: String?
    /// Announced after a move, from the item, the bucket, the position and the count. Built in:
    /// "PROJ-1 moved to Sprint 14, position 2 of 5."
    public var moved: (@Sendable (_ item: String, _ bucket: String, _ position: Int, _ count: Int) -> String)?
    /// The spoken state of an open bucket. Built in: "Expanded".
    public var expanded: String?
    /// The spoken state of a closed bucket. Built in: "Collapsed".
    public var collapsed: String?

    /// Replacements for built-in strings. Every parameter is optional; each is described on its property.
    public init(
        summary: (@Sendable (_ count: Int, _ points: Int?, _ unestimated: Int) -> String)? = nil,
        capacity: (@Sendable (_ points: Int, _ capacity: Int) -> String)? = nil,
        capacityLabel: String? = nil,
        overCapacity: (@Sendable (Int) -> String)? = nil,
        rowPoints: (@Sendable (Int) -> String)? = nil,
        rowUnestimated: String? = nil,
        notEstimated: String? = nil,
        moveMenu: (@Sendable (String) -> String)? = nil,
        moveTo: String? = nil,
        moveUp: String? = nil,
        moveDown: String? = nil,
        moveToBucket: (@Sendable (String) -> String)? = nil,
        empty: String? = nil,
        moved: (@Sendable (_ item: String, _ bucket: String, _ position: Int, _ count: Int) -> String)? = nil,
        expanded: String? = nil,
        collapsed: String? = nil
    ) {
        self.summary = summary
        self.capacity = capacity
        self.capacityLabel = capacityLabel
        self.overCapacity = overCapacity
        self.rowPoints = rowPoints
        self.rowUnestimated = rowUnestimated
        self.notEstimated = notEstimated
        self.moveMenu = moveMenu
        self.moveTo = moveTo
        self.moveUp = moveUp
        self.moveDown = moveDown
        self.moveToBucket = moveToBucket
        self.empty = empty
        self.moved = moved
        self.expanded = expanded
        self.collapsed = collapsed
    }
}
