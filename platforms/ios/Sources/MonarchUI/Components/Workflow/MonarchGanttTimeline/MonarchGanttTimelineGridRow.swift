import SwiftUI

/// The grid part of one row: as tall as its label (a hidden copy sets the
/// height), with the item's bar or marker, its milestones, or a note such
/// as "Ended Jan 9, 2026".
struct MonarchGanttTimelineGridRow: View {
    let model: MonarchGanttTimelineModel
    let line: MonarchGanttTimelineLine
    let labelWidth: CGFloat
    let drag: MonarchGanttTimelineDrag?
    let onDrag: @MainActor (MonarchGanttTimelineDrag?) -> Void

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let scale = model.scale
        MonarchGanttTimelineRowLabel(model: model, line: line)
            .frame(width: labelWidth)
            .hidden()
            .frame(width: MonarchGanttTimelineGrid.width(model), alignment: .leading)
            .overlay(alignment: .leading) {
                if case .item(let item, _, _, let rollup) = line {
                    content(item, hasRollup: rollup != nil, scale: scale)
                }
            }
            .anchorPreference(key: MonarchGanttTimelineRowBounds.self, value: .bounds) { [line.key: $0] }
    }

    @ViewBuilder
    private func content(_ item: MonarchGanttTimelineItem, hasRollup: Bool, scale: MonarchGanttTimelineScale)
        -> some View
    {
        let shown = MonarchGanttTimelineDrag.shown(item, drag: drag, calendar: model.days.calendar)
        let span = model.span(shown)
        let note = MonarchGanttTimelineRules.rowNote(
            span: span,
            hasRollup: hasRollup,
            anchor: model.anchor,
            rangeEnd: model.rangeEnd,
            formats: model.formats,
            text: model.text
        )
        ZStack(alignment: .leading) {
            if let note {
                Text(note)
                    .monarchTextStyle(MonarchGanttTimelineTokens.captionStyle)
                    .foregroundStyle(theme.colors[role: MonarchGanttTimelineTokens.mutedColor])
                    .lineLimit(1)
                    .padding(.leading, MonarchGanttTimelineTokens.offscreenPadding)
            } else if let span {
                let isPoint = MonarchGanttTimelineRules.isPoint(shown)
                let box = MonarchGanttTimelineTokens.barBox(span, isPoint: isPoint, scale: scale)
                MonarchGanttTimelineBar(
                    model: model,
                    item: item,
                    span: span,
                    isPoint: isPoint,
                    box: box,
                    isDragging: drag?.id == item.id,
                    onDrag: onDrag
                )
                .offset(x: box.nodeLeft)
            }
            ForEach(model.milestones.filter { $0.itemID == item.id }) { milestone in
                let day = model.days.day(milestone.date)
                if day >= model.anchor && day <= model.rangeEnd {
                    MonarchGanttTimelineDiamond(isDone: milestone.isDone)
                        .offset(x: scale.x(day) + (scale.dayWidth - MonarchGanttTimelineTokens.diamondSize) / 2)
                        .accessibilityElement()
                        .accessibilityLabel(model.describe(milestone))
                        .accessibilityAddTraits(.isImage)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }
}
