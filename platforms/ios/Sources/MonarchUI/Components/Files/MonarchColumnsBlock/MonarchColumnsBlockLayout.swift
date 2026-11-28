import SwiftUI

/// Lays out the columns of a ``MonarchColumnsBlock``: side by side and
/// top-aligned, or stacked below ``MonarchColumnsBlockTokens/stackWidth``.
struct MonarchColumnsBlockLayout: Layout {
    let ratio: Double
    let forcesStack: Bool

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? MonarchColumnsBlockTokens.stackWidth
        if MonarchColumnsBlockTokens.isStacked(width: width, forcesStack: forcesStack) {
            let heights = subviews.map { $0.sizeThatFits(ProposedViewSize(width: width, height: nil)).height }
            let gaps = MonarchColumnsBlockTokens.rowSpacing * CGFloat(max(0, subviews.count - 1))
            return CGSize(width: width, height: heights.reduce(0, +) + gaps)
        }
        let columns = MonarchColumnsBlockTokens.columns(count: subviews.count, width: width, ratio: ratio)
        let height =
            zip(subviews, columns).map { subview, column in
                subview.sizeThatFits(ProposedViewSize(width: column.width, height: nil)).height
            }
            .max() ?? 0
        return CGSize(width: width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        if MonarchColumnsBlockTokens.isStacked(width: bounds.width, forcesStack: forcesStack) {
            var y = bounds.minY
            for subview in subviews {
                let size = ProposedViewSize(width: bounds.width, height: nil)
                subview.place(at: CGPoint(x: bounds.minX, y: y), anchor: .topLeading, proposal: size)
                y += subview.sizeThatFits(size).height + MonarchColumnsBlockTokens.rowSpacing
            }
            return
        }
        let columns = MonarchColumnsBlockTokens.columns(count: subviews.count, width: bounds.width, ratio: ratio)
        for (subview, column) in zip(subviews, columns) {
            subview.place(
                at: CGPoint(x: bounds.minX + column.x, y: bounds.minY),
                anchor: .topLeading,
                proposal: ProposedViewSize(width: column.width, height: nil)
            )
        }
    }
}
