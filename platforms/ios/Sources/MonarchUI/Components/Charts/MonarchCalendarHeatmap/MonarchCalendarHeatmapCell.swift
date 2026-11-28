import MonarchTokens
import SwiftUI

/// One day: its fill, its outline and its marker. Text, if any, is the content.
struct MonarchCalendarHeatmapCell<Content: View>: View {
    let style: MonarchCalendarHeatmapTokens.CellStyle
    let base: MonarchRGBA
    let radius: CGFloat
    let markerSize: CGFloat?
    @ViewBuilder let content: () -> Content

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: radius, style: .continuous)
        content()
            .background { fill(shape) }
            .overlay {
                if let stroke = style.stroke {
                    shape.strokeBorder(
                        theme.colors[role: stroke],
                        style: StrokeStyle(
                            lineWidth: style.strokeWidth,
                            dash: style.isDashed ? MonarchCalendarHeatmapTokens.dash : []
                        )
                    )
                }
            }
            .overlay(alignment: .topTrailing) {
                if let markerSize {
                    Circle()
                        .fill(theme.colors[role: MonarchCalendarHeatmapTokens.markerRole])
                        .overlay(
                            Circle().strokeBorder(
                                theme.colors[role: MonarchCalendarHeatmapTokens.markerRingRole],
                                lineWidth: MonarchCalendarHeatmapTokens.thinStroke
                            )
                        )
                        .frame(width: markerSize, height: markerSize)
                        .padding(MonarchCalendarHeatmapTokens.thinStroke)
                }
            }
    }

    @ViewBuilder private func fill(_ shape: RoundedRectangle) -> some View {
        switch style.fill {
        case .level(let level):
            if let rgba = MonarchHeatmapTokens.fill(level: level, base: base, muted: theme.colors.tokens.bgMuted) {
                shape.fill(Color(monarch: rgba))
            }
        case .inactive:
            shape.fill(theme.colors[role: MonarchCalendarHeatmapTokens.inactiveRole])
        case .none:
            EmptyView()
        }
    }
}
