import CoreGraphics

/// What every card of a ``MonarchOrgChart`` needs to know and do, passed down
/// the chart and the list.
struct MonarchOrgChartContext {
    let tree: MonarchOrgChartTree
    let expanded: Set<String>
    let loading: Set<String>
    let focusID: String?
    let dropTargetID: String?
    let dottedLines: [MonarchOrgChartDottedLine]
    let labels: MonarchOrgChartLabels
    let canSelect: Bool
    let canMove: Bool
    /// The zoom of the chart. Chips grow their touch target in step, so it stays 44 points on screen.
    var zoom: CGFloat = 1
    let toggle: @MainActor (MonarchOrgChartNode) -> Void
    let select: @MainActor (MonarchOrgChartNode) -> Void
    let requestMove: @MainActor (MonarchOrgChartNode, MonarchOrgChartMoveTarget) -> Void
    let setDropTarget: @MainActor (String, Bool) -> Void

    func isExpanded(_ node: MonarchOrgChartNode) -> Bool {
        tree.hasReports(node) && expanded.contains(node.id)
    }

    func isLoading(_ node: MonarchOrgChartNode) -> Bool {
        loading.contains(node.id)
    }

    /// Whether this card can be dragged or asked to move.
    func canMove(_ node: MonarchOrgChartNode) -> Bool {
        canMove && node.kind == .person
    }
}
