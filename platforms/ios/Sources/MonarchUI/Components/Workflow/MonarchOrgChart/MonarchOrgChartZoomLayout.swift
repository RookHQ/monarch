import SwiftUI

/// Reports its single subview's size times the zoom, so a scroll view
/// scrolls the zoomed chart. The subview is drawn at its natural size and
/// scaled from its top leading corner with `scaleEffect`, which changes only
/// how it looks.
struct MonarchOrgChartZoomLayout: Layout {
    let zoom: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        guard let subview = subviews.first else { return .zero }
        let natural = subview.sizeThatFits(.unspecified)
        return CGSize(width: natural.width * zoom, height: natural.height * zoom)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        guard let subview = subviews.first else { return }
        let natural = subview.sizeThatFits(.unspecified)
        subview.place(
            at: CGPoint(x: bounds.minX, y: bounds.minY),
            anchor: .topLeading,
            proposal: ProposedViewSize(natural)
        )
    }
}
