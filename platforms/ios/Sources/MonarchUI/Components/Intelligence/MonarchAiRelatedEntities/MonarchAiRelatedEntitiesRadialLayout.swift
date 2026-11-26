import SwiftUI

/// Places the first subview in the middle and the others on a ring around
/// it, from the top, clockwise. The graph is drawn left to right in every
/// layout direction, so the relation lines and the nodes always agree.
struct MonarchAiRelatedEntitiesRadialLayout: Layout {
    /// The width of a node, which keeps the ring inside the square.
    let nodeWidth: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let side = MonarchAiRelatedEntitiesTokens.graphSide(proposedWidth: proposal.width, nodeWidth: nodeWidth)
        return CGSize(width: side, height: side)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let side = min(bounds.width, bounds.height)
        let points = MonarchAiRelatedEntitiesTokens.nodePoints(count: subviews.count, side: side, inset: nodeWidth / 2)
        let origin = CGPoint(x: bounds.midX - side / 2, y: bounds.midY - side / 2)
        for (subview, point) in zip(subviews, points) {
            subview.place(
                at: CGPoint(x: origin.x + point.x, y: origin.y + point.y),
                anchor: .center,
                proposal: ProposedViewSize(width: nodeWidth, height: nil)
            )
        }
    }
}

/// The relation lines of the graph, between the same points the layout uses.
struct MonarchAiRelatedEntitiesEdges: Shape {
    let edges: [MonarchAiRelatedEntitiesEdge]
    let count: Int
    let nodeWidth: CGFloat

    func path(in rect: CGRect) -> Path {
        let side = min(rect.width, rect.height)
        let points = MonarchAiRelatedEntitiesTokens.nodePoints(count: count, side: side, inset: nodeWidth / 2)
        let origin = CGPoint(x: rect.midX - side / 2, y: rect.midY - side / 2)
        var path = Path()
        for edge in edges where points.indices.contains(edge.from) && points.indices.contains(edge.to) {
            path.move(to: CGPoint(x: origin.x + points[edge.from].x, y: origin.y + points[edge.from].y))
            path.addLine(to: CGPoint(x: origin.x + points[edge.to].x, y: origin.y + points[edge.to].y))
        }
        return path
    }
}
