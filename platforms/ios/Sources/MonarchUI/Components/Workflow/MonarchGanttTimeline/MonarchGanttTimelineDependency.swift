/// A link between two items of a ``MonarchGanttTimeline``, drawn as an arrow
/// and written as "Depends on" in the list.
@_spi(Experimental) public struct MonarchGanttTimelineDependency: Identifiable, Hashable, Sendable {
    /// A stable id, unique among the dependencies.
    public var id: String
    /// The id of the item that comes first.
    public var from: String
    /// The id of the item that depends on it.
    public var to: String
    /// Which ends are linked.
    public var kind: MonarchGanttTimelineDependencyKind
    /// Whether the link is broken. The arrow is dashed in the strong danger colour.
    public var isConflict: Bool

    /// A dependency.
    ///
    /// - Parameters:
    ///   - id: A stable id, unique among the dependencies.
    ///   - from: The id of the item that comes first.
    ///   - to: The id of the item that depends on it.
    ///   - kind: Which ends are linked.
    ///   - isConflict: Whether the link is broken.
    public init(
        id: String,
        from: String,
        to: String,
        kind: MonarchGanttTimelineDependencyKind = .finishToStart,
        isConflict: Bool = false
    ) {
        self.id = id
        self.from = from
        self.to = to
        self.kind = kind
        self.isConflict = isConflict
    }
}
