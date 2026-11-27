import SwiftUI

/// The calendar grid: the header, the milestone lane and a row per line,
/// over the gridlines, weekends, spans and today line, under the dependency
/// arrows. Its time axis runs left to right in every layout direction, as on
/// the web, so the chart lays it out left to right.
struct MonarchGanttTimelineGrid: View {
    let model: MonarchGanttTimelineModel
    let labelWidth: CGFloat

    @Environment(\.monarchTheme) private var theme
    @State private var drag: MonarchGanttTimelineDrag?

    /// The width of the grid: the days of the window.
    static func width(_ model: MonarchGanttTimelineModel) -> CGFloat {
        CGFloat(MonarchGanttTimelineTokens.span(model.zoom)) * model.scale.dayWidth
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            if !model.laneMilestones.isEmpty {
                lane
            }
            ForEach(model.lines.lines, id: \.key) { line in
                MonarchGanttTimelineGridRow(
                    model: model,
                    line: line,
                    labelWidth: labelWidth,
                    drag: drag,
                    onDrag: { drag = $0 }
                )
            }
        }
        .coordinateSpace(.named(MonarchGanttTimelineTokens.gridSpace))
        .backgroundPreferenceValue(MonarchGanttTimelineRowBounds.self) { anchors in
            GeometryReader { proxy in
                MonarchGanttTimelineDecor(model: model, bounds: anchors.mapValues { proxy[$0] }, drag: drag)
            }
            .accessibilityHidden(true)
        }
        .overlayPreferenceValue(MonarchGanttTimelineRowBounds.self) { anchors in
            GeometryReader { proxy in
                let bounds = anchors.mapValues { proxy[$0] }
                MonarchGanttTimelineArrows(model: model, bounds: bounds, drag: drag)
                todayLabel(top: bounds[MonarchGanttTimelineRowBounds.header]?.maxY ?? 0)
            }
            .allowsHitTesting(false)
            .accessibilityHidden(true)
        }
        .clipped()
    }

    private var header: some View {
        let scale = model.scale
        let ticks = MonarchGanttTimelineRules.ticks(anchor: model.anchor, zoom: model.zoom, days: model.days)
        let width = Self.width(model)
        let pad = MonarchGanttTimelineTokens.tickPadding
        return MonarchGanttTimelineHeaderProbe()
            .frame(width: width, alignment: .leading)
            .overlay(alignment: .topLeading) {
                ZStack(alignment: .topLeading) {
                    ForEach(Array(ticks.major.enumerated()), id: \.element.day) { index, tick in
                        let next = index + 1 < ticks.major.count ? scale.x(ticks.major[index + 1].day) : width
                        tickText(model.formats.month(tick.day, withYear: tick.withYear), role: \.textDefault)
                            .frame(width: max(next - scale.x(tick.day) - pad * 2, 0), alignment: .leading)
                            .offset(x: scale.x(tick.day) + pad)
                    }
                }
                .padding(.top, pad)
            }
            .overlay(alignment: .bottomLeading) {
                ZStack(alignment: .bottomLeading) {
                    ForEach(Array(ticks.minor.enumerated()), id: \.element) { index, day in
                        let next = index + 1 < ticks.minor.count ? scale.x(ticks.minor[index + 1]) : width
                        let label =
                            model.zoom == .week ? model.formats.weekdayDay(day) : model.formats.dayNumber(day)
                        tickText(label, role: MonarchGanttTimelineTokens.mutedColor)
                            .frame(width: max(next - scale.x(day) - pad, 0), alignment: .leading)
                            .offset(x: scale.x(day) + pad / 2)
                    }
                }
                .padding(.bottom, pad)
            }
            .accessibilityHidden(true)
            .anchorPreference(key: MonarchGanttTimelineRowBounds.self, value: .bounds) {
                [MonarchGanttTimelineRowBounds.header: $0]
            }
    }

    private func tickText(_ text: String, role: MonarchColorRole) -> some View {
        Text(text)
            .monarchTextStyle(MonarchGanttTimelineTokens.captionStyle)
            .foregroundStyle(theme.colors[role: role])
            .lineLimit(1)
            .truncationMode(.tail)
    }

    private var lane: some View {
        let scale = model.scale
        return MonarchGanttTimelineLaneLabel(text: model.text.milestones)
            .frame(width: labelWidth)
            .hidden()
            .frame(width: Self.width(model), alignment: .leading)
            .overlay(alignment: .leading) {
                ZStack(alignment: .leading) {
                    ForEach(model.laneMilestones) { milestone in
                        let day = model.days.day(milestone.date)
                        if day >= model.anchor && day <= model.rangeEnd {
                            HStack(spacing: MonarchGanttTimelineTokens.laneGap) {
                                MonarchGanttTimelineDiamond(isDone: milestone.isDone)
                                Text(milestone.label)
                                    .monarchTextStyle(MonarchGanttTimelineTokens.captionStyle)
                                    .foregroundStyle(theme.colors[role: MonarchGanttTimelineTokens.mutedColor])
                                    .lineLimit(1)
                            }
                            .offset(x: scale.x(day) + (scale.dayWidth - MonarchGanttTimelineTokens.diamondSize) / 2)
                            .accessibilityElement(children: .ignore)
                            .accessibilityLabel(model.describe(milestone))
                            .accessibilityAddTraits(.isImage)
                            .help(milestone.label)
                        }
                    }
                }
            }
            .anchorPreference(key: MonarchGanttTimelineRowBounds.self, value: .bounds) {
                [MonarchGanttTimelineRowBounds.lane: $0]
            }
    }

    @ViewBuilder private func todayLabel(top: CGFloat) -> some View {
        if let today = model.today, model.showsToday {
            let scale = model.scale
            Text(model.text.today)
                .monarchTextStyle(MonarchGanttTimelineTokens.captionStyle)
                .foregroundStyle(theme.colors[role: MonarchGanttTimelineTokens.onTodayColor])
                .lineLimit(1)
                .fixedSize()
                .padding(.horizontal, MonarchGanttTimelineTokens.todayLabelPadding)
                .background(
                    theme.colors[role: MonarchGanttTimelineTokens.todayColor],
                    in: RoundedRectangle(cornerRadius: MonarchGanttTimelineTokens.todayLabelRadius)
                )
                .offset(
                    x: scale.x(today) + scale.dayWidth / 2 + MonarchGanttTimelineTokens.todayLineWidth,
                    y: top
                )
        }
    }
}
