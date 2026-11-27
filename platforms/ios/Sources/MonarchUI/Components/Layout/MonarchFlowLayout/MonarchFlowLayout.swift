import SwiftUI

/// A horizontal row that wraps onto new rows when its children do not fit:
/// button rows, tag lists, metadata lines.
///
/// `HStack` never wraps, so a row of buttons or tags overflows a narrow
/// screen or a large text size. This layout places children in reading
/// order and starts a new row when the next child does not fit.
///
/// ```swift
/// MonarchFlowLayout(spacing: theme.spacing.space2) {
///     ForEach(tags, id: \.self) { tag in
///         MonarchBadge(tag)
///     }
/// }
/// ```
///
/// Take spacing from the theme's space scale. A child wider than the layout
/// gets a row of its own and is offered the full width, so long text wraps
/// instead of overflowing.
///
/// Accessibility: a layout adds no element of its own. Children are placed
/// in source order, which is also the order VoiceOver reads them in, and
/// the rows follow the layout direction.
public struct MonarchFlowLayout: Layout {
    private let spacing: CGFloat
    private let rowSpacing: CGFloat
    private let alignment: VerticalAlignment
    private let justification: MonarchFlowLayoutJustification

    /// A wrapping row.
    ///
    /// - Parameters:
    ///   - spacing: The space between children in a row. 8 points by default.
    ///   - rowSpacing: The space between rows. Equal to `spacing` by default.
    ///   - alignment: How children of different heights line up within a row.
    ///   - justification: How a row is placed in the width that is left over.
    public init(
        spacing: CGFloat? = nil,
        rowSpacing: CGFloat? = nil,
        alignment: VerticalAlignment = .center,
        justification: MonarchFlowLayoutJustification = .leading
    ) {
        let resolved = max(0, spacing ?? MonarchFlowLayoutEngine.defaultSpacing)
        self.spacing = resolved
        self.rowSpacing = max(0, rowSpacing ?? resolved)
        self.alignment = alignment
        self.justification = justification
    }

    /// The size of the layout for a proposal.
    public func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        return arrangement(of: subviews, in: width).size
    }

    /// Places the children in rows.
    public func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let frames = arrangement(of: subviews, in: bounds.width).frames
        for (subview, frame) in zip(subviews, frames) {
            subview.place(
                at: CGPoint(x: bounds.minX + frame.minX, y: bounds.minY + frame.minY),
                anchor: .topLeading,
                proposal: ProposedViewSize(frame.size)
            )
        }
    }

    private func arrangement(of subviews: Subviews, in width: CGFloat) -> MonarchFlowLayoutEngine.Arrangement {
        let items = subviews.map { subview in
            var dimensions = subview.dimensions(in: .unspecified)
            if dimensions.width > width {
                // Too wide for a row: offer the full width so text wraps.
                dimensions = subview.dimensions(in: ProposedViewSize(width: width, height: nil))
            }
            return MonarchFlowLayoutEngine.Item(
                size: CGSize(width: dimensions.width, height: dimensions.height),
                guide: dimensions[alignment]
            )
        }
        return MonarchFlowLayoutEngine.arrange(
            items,
            width: width,
            spacing: spacing,
            rowSpacing: rowSpacing,
            justification: justification
        )
    }
}

#if DEBUG
    #Preview("MonarchFlowLayout") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchFlowLayoutJustification.allCases, id: \.self) { justification in
                    MonarchFlowLayout(justification: justification) {
                        ForEach(["Design", "Research", "Engineering", "Support", "Sales"], id: \.self) { tag in
                            MonarchBadge(tag)
                        }
                    }
                }
            }
        }
    }
#endif
