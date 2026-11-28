import SwiftUI

/// The marker of one legend entry, drawn in its series colour.
struct MonarchChartLegendSwatch: View {
    let marker: MonarchChartLegendMarker
    let role: MonarchColorRole
    let isDashed: Bool
    /// A colour that replaces the role, for a derived (softened) fill.
    var fill: Color?

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .caption) private var size = MonarchChartLegendTokens.markerSize
    @ScaledMetric(relativeTo: .caption) private var length = MonarchChartLegendTokens.lineLength

    var body: some View {
        let color = fill ?? theme.colors[role: role]
        let stroke = StrokeStyle(
            lineWidth: MonarchChartLegendTokens.strokeWidth,
            lineCap: .round,
            dash: isDashed ? MonarchChartLegendTokens.dash : []
        )
        Group {
            switch marker {
            case .line:
                MonarchChartLegendLine()
                    .stroke(color, style: stroke)
                    .frame(width: length, height: size)
            case .square:
                let shape = RoundedRectangle(cornerRadius: MonarchChartLegendTokens.squareRadius, style: .continuous)
                if isDashed {
                    shape.strokeBorder(color, lineWidth: MonarchChartLegendTokens.strokeWidth).frame(
                        width: size,
                        height: size
                    )
                } else {
                    shape.fill(color).frame(width: size, height: size)
                }
            case .dot:
                if isDashed {
                    Circle().strokeBorder(color, lineWidth: MonarchChartLegendTokens.strokeWidth)
                        .frame(width: size, height: size)
                } else {
                    Circle().fill(color).frame(width: size, height: size)
                }
            }
        }
        .accessibilityHidden(true)
    }
}

/// A horizontal line through the middle of its frame.
private struct MonarchChartLegendLine: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.minX, y: rect.midY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.midY))
        return path
    }
}
