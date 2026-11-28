import SwiftUI

/// The chart: the row labels at the leading edge and the calendar grid
/// beside them, which scrolls sideways. Rows are as tall as their label and
/// at least 44 points.
struct MonarchGanttTimelineChart: View {
    let model: MonarchGanttTimelineModel

    @Environment(\.monarchTheme) private var theme
    @State private var width: CGFloat = 0

    var body: some View {
        let labelWidth = MonarchGanttTimelineTokens.labelWidth(width)
        let shape = MonarchShapes.container
        HStack(alignment: .top, spacing: 0) {
            labels
                .frame(width: labelWidth)
            Rectangle()
                .fill(theme.colors[role: MonarchGanttTimelineTokens.lineColor])
                .frame(width: MonarchGanttTimelineTokens.lineWidth)
                .accessibilityHidden(true)
            ScrollView(.horizontal) {
                MonarchGanttTimelineGrid(model: model, labelWidth: labelWidth)
            }
            .scrollIndicators(.automatic)
            // The time axis runs left to right in every layout direction, as on the web.
            .environment(\.layoutDirection, .leftToRight)
        }
        .fixedSize(horizontal: false, vertical: true)
        .background(theme.colors[role: MonarchGanttTimelineTokens.surfaceColor])
        .clipShape(shape)
        .overlay {
            shape.strokeBorder(
                theme.colors[role: MonarchGanttTimelineTokens.lineColor],
                lineWidth: MonarchGanttTimelineTokens.lineWidth
            )
        }
        .onGeometryChange(for: CGFloat.self, of: { $0.size.width }, action: { width = $0 })
        .accessibilityElement(children: .contain)
        .accessibilityLabel(model.text.timeline)
    }

    private var labels: some View {
        VStack(alignment: .leading, spacing: 0) {
            MonarchGanttTimelineHeaderProbe()
                .frame(maxWidth: .infinity)
                .overlay(alignment: .bottomLeading) {
                    Text(model.text.rowHeader)
                        .monarchTextStyle(MonarchGanttTimelineTokens.captionStyle)
                        .foregroundStyle(theme.colors[role: MonarchGanttTimelineTokens.mutedColor])
                        .lineLimit(1)
                        .padding(.horizontal, MonarchGanttTimelineTokens.labelPadding)
                        .padding(.bottom, MonarchGanttTimelineTokens.tickPadding)
                }
                .background(theme.colors[role: MonarchGanttTimelineTokens.shadeColor])
                .overlay(alignment: .bottom) { rule }
            if !model.laneMilestones.isEmpty {
                MonarchGanttTimelineLaneLabel(text: model.text.milestones)
                    .overlay(alignment: .bottom) { rule }
            }
            ForEach(model.lines.lines, id: \.key) { line in
                MonarchGanttTimelineRowLabel(model: model, line: line)
                    .background {
                        if case .group = line {
                            theme.colors[role: MonarchGanttTimelineTokens.shadeColor]
                        }
                    }
                    .overlay(alignment: .bottom) { rule }
            }
        }
    }

    private var rule: some View {
        Rectangle()
            .fill(theme.colors[role: MonarchGanttTimelineTokens.lineColor])
            .frame(height: MonarchGanttTimelineTokens.lineWidth)
            .accessibilityHidden(true)
    }
}
