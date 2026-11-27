import Foundation

/// What a ``MonarchGanttTimeline`` shows: the first visible day, the zoom,
/// the chart or the list, and the collapsed parents. The toolbar and the rows
/// change it; pass a binding to read or set it.
@_spi(Experimental) public struct MonarchGanttTimelineState: Hashable, Sendable {
    /// The first visible day, or `nil` to start the week before today (or
    /// before the earliest item when there is no today).
    public var start: Date?
    /// The zoom.
    public var zoom: MonarchGanttTimelineZoom
    /// The chart or the list.
    public var view: MonarchGanttTimelineView
    /// The ids of the parent items whose children are hidden.
    public var collapsed: Set<String>

    /// A state.
    ///
    /// - Parameters:
    ///   - start: The first visible day, or `nil` for the automatic start.
    ///   - zoom: The zoom.
    ///   - view: The chart or the list.
    ///   - collapsed: The ids of the parent items whose children are hidden.
    public init(
        start: Date? = nil,
        zoom: MonarchGanttTimelineZoom = .month,
        view: MonarchGanttTimelineView = .chart,
        collapsed: Set<String> = []
    ) {
        self.start = start
        self.zoom = zoom
        self.view = view
        self.collapsed = collapsed
    }

    /// Hides the children of an item when they are shown, and shows them when they are hidden.
    ///
    /// - Parameter id: The id of the parent item.
    public mutating func toggle(_ id: String) {
        if collapsed.contains(id) {
            collapsed.remove(id)
        } else {
            collapsed.insert(id)
        }
    }
}
