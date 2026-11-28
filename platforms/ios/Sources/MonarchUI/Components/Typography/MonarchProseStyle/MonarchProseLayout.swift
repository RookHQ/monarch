import SwiftUI

/// Stacks prose blocks at the leading edge with the space each pair of
/// blocks asks for. Placement is in reading order; SwiftUI mirrors it in a
/// right-to-left layout.
struct MonarchProseLayout: Layout {
    /// One em of the prose in points.
    let em: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let frames = arrangement(of: subviews, width: proposal.width)
        let width = proposal.width ?? frames.map(\.maxX).max() ?? 0
        return CGSize(width: width, height: frames.last?.maxY ?? 0)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let frames = arrangement(of: subviews, width: bounds.width)
        for (subview, frame) in zip(subviews, frames) {
            subview.place(
                at: CGPoint(x: bounds.minX + frame.minX, y: bounds.minY + frame.minY),
                anchor: .topLeading,
                proposal: ProposedViewSize(width: frame.width, height: frame.height)
            )
        }
    }

    /// The frame of each block, from the top.
    private func arrangement(of subviews: Subviews, width: CGFloat?) -> [CGRect] {
        var frames: [CGRect] = []
        var y: CGFloat = 0
        var previous: MonarchProseBlock?
        for subview in subviews {
            let block = subview[MonarchProseBlockKey.self]
            if let previous {
                y += MonarchProseTokens.spacing(after: previous, before: block) * em
            }
            let size = subview.sizeThatFits(ProposedViewSize(width: width, height: nil))
            frames.append(CGRect(x: 0, y: y, width: width ?? size.width, height: size.height))
            y += size.height
            previous = block
        }
        return frames
    }
}
