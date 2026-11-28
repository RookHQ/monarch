import SwiftUI

/// A milestone diamond: filled with the accent when done, otherwise the
/// surface with a muted outline. The caller names it or hides it.
struct MonarchGanttTimelineDiamond: View {
    let isDone: Bool

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let colors = MonarchGanttTimelineTokens.milestoneColors(isDone: isDone)
        Rectangle()
            .fill(theme.colors[role: colors.fill])
            .overlay {
                Rectangle()
                    .strokeBorder(
                        theme.colors[role: colors.border],
                        lineWidth: MonarchGanttTimelineTokens.diamondBorder
                    )
            }
            .frame(width: MonarchGanttTimelineTokens.diamondSize, height: MonarchGanttTimelineTokens.diamondSize)
            .rotationEffect(.degrees(MonarchGanttTimelineTokens.diamondDegrees))
    }
}

/// Collects the bounds of the header, the lane and every row of the grid,
/// by row key, so the gridlines, spans and arrows are drawn at real positions.
struct MonarchGanttTimelineRowBounds: PreferenceKey {
    static var defaultValue: [String: Anchor<CGRect>] { [:] }

    static func reduce(value: inout [String: Anchor<CGRect>], nextValue: () -> [String: Anchor<CGRect>]) {
        value.merge(nextValue()) { current, _ in current }
    }

    /// The key of the header.
    static let header = "header"
    /// The key of the milestone lane.
    static let lane = "lane"
}
