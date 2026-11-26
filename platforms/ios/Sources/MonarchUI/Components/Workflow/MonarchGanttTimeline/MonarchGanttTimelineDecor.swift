import SwiftUI

/// What is drawn under the rows: the header and group shading, weekends,
/// gridlines, row lines, group spans, parent brackets and the today line.
/// Decorative; drawn left to right like the grid.
struct MonarchGanttTimelineDecor: View {
    let model: MonarchGanttTimelineModel
    let bounds: [String: CGRect]
    let drag: MonarchGanttTimelineDrag?

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Canvas { context, size in
            let tokens = MonarchGanttTimelineTokens.self
            let scale = model.scale
            let top = bounds[MonarchGanttTimelineRowBounds.header]?.maxY ?? 0
            let shade = theme.colors[role: tokens.shadeColor]
            let line = theme.colors[role: tokens.lineColor]
            context.fill(Path(CGRect(x: 0, y: 0, width: size.width, height: top)), with: .color(shade))
            if tokens.usesMinorGrid(model.zoom) {
                for day in model.anchor...model.rangeEnd where model.days.isWeekend(day) {
                    let rect = CGRect(x: scale.x(day), y: top, width: scale.dayWidth, height: size.height - top)
                    context.fill(Path(rect), with: .color(shade))
                }
            }
            for row in model.lines.lines {
                if case .group = row, let rect = bounds[row.key] {
                    context.fill(
                        Path(CGRect(x: 0, y: rect.minY, width: size.width, height: rect.height)),
                        with: .color(shade)
                    )
                }
            }
            let ticks = MonarchGanttTimelineRules.ticks(anchor: model.anchor, zoom: model.zoom, days: model.days)
            let gridDays = tokens.usesMinorGrid(model.zoom) ? ticks.minor : ticks.major.map(\.day)
            for day in gridDays where day > model.anchor {
                stroke(
                    &context,
                    from: CGPoint(x: scale.x(day), y: top),
                    to: CGPoint(x: scale.x(day), y: size.height),
                    color: line
                )
            }
            for rect in bounds.values {
                stroke(
                    &context,
                    from: CGPoint(x: 0, y: rect.maxY),
                    to: CGPoint(x: size.width, y: rect.maxY),
                    color: line
                )
            }
            spans(&context)
            if let today = model.today, model.showsToday {
                let x = scale.x(today) + scale.dayWidth / 2
                var path = Path()
                path.move(to: CGPoint(x: x, y: top))
                path.addLine(to: CGPoint(x: x, y: size.height))
                context.stroke(
                    path,
                    with: .color(theme.colors[role: tokens.todayColor]),
                    lineWidth: tokens.todayLineWidth
                )
            }
        }
    }

    /// The thin span of a dated group and the bracket of a parent without dates.
    private func spans(_ context: inout GraphicsContext) {
        let tokens = MonarchGanttTimelineTokens.self
        let scale = model.scale
        let fill = theme.colors[role: tokens.groupSpanColor]
        let edge = theme.colors[role: tokens.bracketColor]
        for row in model.lines.lines {
            guard let rect = bounds[row.key] else { continue }
            let span: MonarchGanttTimelineSpan?
            switch row {
            case .group(let group):
                span = MonarchGanttTimelineRules.span(group.start, group.end, days: model.days)
            case .item(_, _, _, let rollup):
                span = rollup
            }
            guard let span else { continue }
            let box = CGRect(
                x: scale.x(span.start),
                y: rect.midY - tokens.spanHeight / 2,
                width: CGFloat(span.days) * scale.dayWidth,
                height: tokens.spanHeight
            )
            if case .group = row {
                let shape = Path(roundedRect: box, cornerRadius: tokens.spanHeight / 2)
                context.fill(shape, with: .color(fill))
                context.stroke(shape, with: .color(edge), lineWidth: tokens.lineWidth)
            } else {
                var bracket = Path()
                bracket.move(to: CGPoint(x: box.minX, y: box.maxY))
                bracket.addLine(to: CGPoint(x: box.minX, y: box.minY))
                bracket.addLine(to: CGPoint(x: box.maxX, y: box.minY))
                bracket.addLine(to: CGPoint(x: box.maxX, y: box.maxY))
                context.stroke(bracket, with: .color(edge), lineWidth: tokens.arrowStroke)
            }
        }
    }

    private func stroke(_ context: inout GraphicsContext, from start: CGPoint, to end: CGPoint, color: Color) {
        var path = Path()
        path.move(to: start)
        path.addLine(to: end)
        context.stroke(path, with: .color(color), lineWidth: MonarchGanttTimelineTokens.lineWidth)
    }
}

/// The dependency arrows over the bars. They follow a bar while it is
/// dragged; an arrow to an item that is not shown is not drawn. Decorative:
/// the list and each bar's "Depends on" carry the same facts.
struct MonarchGanttTimelineArrows: View {
    let model: MonarchGanttTimelineModel
    let bounds: [String: CGRect]
    let drag: MonarchGanttTimelineDrag?

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Canvas { context, _ in
            let tokens = MonarchGanttTimelineTokens.self
            let byID = model.itemsByID
            for dependency in model.dependencies {
                guard let from = byID[dependency.from], let to = byID[dependency.to],
                    let fromRow = bounds["i:\(from.id)"], let toRow = bounds["i:\(to.id)"],
                    let fromSpan = shownSpan(from), let toSpan = shownSpan(to)
                else { continue }
                let points = MonarchGanttTimelineRules.dependencyPath(
                    kind: dependency.kind,
                    from: fromSpan,
                    to: toSpan,
                    fromY: fromRow.midY,
                    toY: toRow.midY,
                    scale: model.scale,
                    gap: tokens.arrowGap,
                    tip: tokens.arrowTip
                )
                guard let tip = points.last, points.count > 1 else { continue }
                let color = theme.colors[role: tokens.dependencyColor(isConflict: dependency.isConflict)]
                var path = Path()
                path.addLines(points)
                context.stroke(
                    path,
                    with: .color(color),
                    style: StrokeStyle(
                        lineWidth: tokens.arrowStroke,
                        dash: dependency.isConflict ? [tokens.dashLength, tokens.dashLength] : []
                    )
                )
                let direction: CGFloat = points[points.count - 2].x <= tip.x ? 1 : -1
                var head = Path()
                head.move(to: tip)
                head.addLine(to: CGPoint(x: tip.x - direction * tokens.arrowHead, y: tip.y - tokens.arrowHead))
                head.addLine(to: CGPoint(x: tip.x - direction * tokens.arrowHead, y: tip.y + tokens.arrowHead))
                head.closeSubpath()
                context.fill(head, with: .color(color))
            }
        }
    }

    private func shownSpan(_ item: MonarchGanttTimelineItem) -> MonarchGanttTimelineSpan? {
        model.span(MonarchGanttTimelineDrag.shown(item, drag: drag, calendar: model.days.calendar))
    }
}
