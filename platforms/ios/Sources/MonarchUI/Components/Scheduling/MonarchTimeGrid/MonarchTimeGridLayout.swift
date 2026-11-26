import SwiftUI

/// Where a child of a day column goes: a span of minutes, and its column in a
/// cluster of overlapping events.
struct MonarchTimeGridPlacement: Equatable {
    /// The span of wall-clock minutes the child covers.
    var span: MonarchTimeGridSpan
    /// Zero-based column inside the overlap cluster.
    var column = 0
    /// The number of columns in the cluster.
    var columns = 1
    /// Whether the child is an event, which leaves the trailing inset free
    /// and is at least the minimum event height.
    var isEvent = false
    /// A fixed height centred on the span's top, for the now line.
    var lineHeight: CGFloat?
}

/// The layout value that carries a child's placement.
struct MonarchTimeGridPlacementKey: LayoutValueKey {
    static let defaultValue = MonarchTimeGridPlacement(span: MonarchTimeGridSpan(top: 0, bottom: 0))
}

extension View {
    /// Places the view in a ``MonarchTimeGridColumnLayout``.
    func monarchTimeGridPlacement(_ placement: MonarchTimeGridPlacement) -> some View {
        layoutValue(key: MonarchTimeGridPlacementKey.self, value: placement)
    }
}

/// Lays out one day column: every child at the height of its minutes, and
/// overlapping events side by side. The column is as tall as the visible
/// hours. SwiftUI mirrors a custom layout's placements in right-to-left
/// layouts, so the first overlap column is on the leading side.
struct MonarchTimeGridColumnLayout: Layout {
    let range: MonarchTimeGridSpan
    let minuteHeight: CGFloat
    let gap: CGFloat
    let trailingInset: CGFloat
    let eventMinHeight: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        CGSize(width: proposal.width ?? 0, height: CGFloat(range.bottom - range.top) * minuteHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        for subview in subviews {
            let frame = Self.frame(
                for: subview[MonarchTimeGridPlacementKey.self],
                in: bounds,
                range: range,
                minuteHeight: minuteHeight,
                gap: gap,
                trailingInset: trailingInset,
                eventMinHeight: eventMinHeight
            )
            subview.place(
                at: frame.origin,
                anchor: .topLeading,
                proposal: ProposedViewSize(width: frame.width, height: frame.height)
            )
        }
    }

    /// The frame of a child, from its placement. Pure, so it is tested.
    static func frame(
        for placement: MonarchTimeGridPlacement,
        in bounds: CGRect,
        range: MonarchTimeGridSpan,
        minuteHeight: CGFloat,
        gap: CGFloat,
        trailingInset: CGFloat,
        eventMinHeight: CGFloat
    ) -> CGRect {
        let top = bounds.minY + CGFloat(placement.span.top - range.top) * minuteHeight
        if let line = placement.lineHeight {
            return CGRect(x: bounds.minX, y: top - line / 2, width: bounds.width, height: line)
        }
        let height = CGFloat(placement.span.bottom - placement.span.top) * minuteHeight
        guard placement.isEvent else {
            return CGRect(x: bounds.minX, y: top, width: bounds.width, height: height)
        }
        let columns = CGFloat(max(1, placement.columns))
        let usable = max(0, bounds.width - trailingInset)
        let slot = usable / columns
        let width = max(0, slot - (placement.columns > 1 ? gap : 0))
        return CGRect(
            x: bounds.minX + slot * CGFloat(placement.column),
            y: top,
            width: width,
            height: max(height, eventMinHeight)
        )
    }
}
