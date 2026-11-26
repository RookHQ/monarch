import Charts
import SwiftUI

/// The Swift Charts plot of a burndown chart. Days are evenly spaced, by
/// their position, so days left out of the sprint (weekends) leave no flat
/// stretch.
struct MonarchBurndownChartPlot: View {
    let model: MonarchBurndownChartTokens.Model
    let visible: [MonarchBurndownChartLine]
    let labels: MonarchBurndownChartLabels
    let noValue: String
    let active: Int?
    let formatValue: ((Double) -> String)?
    let setActive: (Int?) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    private typealias Tokens = MonarchBurndownChartTokens

    var body: some View {
        Chart {
            if let today = model.today {
                todayMark(today)
            }
            if visible.contains(.ideal) {
                lineMarks(.ideal, runs: Tokens.lineRuns(model.ideal), width: MonarchLineChartTokens.lineWidth)
            }
            if visible.contains(.scope) {
                lineMarks(.scope, runs: Tokens.scopeRuns(model.scope), width: Tokens.scopeWidth)
                scopeChanges
            }
            if visible.contains(.remaining) {
                lineMarks(.remaining, runs: Tokens.lineRuns(model.remaining), width: Tokens.remainingWidth)
                lonePoints(.remaining)
            }
            if let active, model.days.indices.contains(active) {
                crosshair(at: active)
            }
        }
        .chartLegend(.hidden)
        .chartXScale(domain: Tokens.xDomain(count: model.count))
        .chartYScale(domain: 0...(Tokens.valueTicks(maximum: model.maximum).last ?? 1))
        .chartXAxis {
            AxisMarks(values: Tokens.tickIndices(count: model.count).map(Double.init)) { value in
                // The first label starts at its tick and the last ends at its tick, as on the web.
                AxisValueLabel(
                    anchor: Tokens.labelAnchor(at: value.as(Double.self), count: model.count),
                    collisionResolution: .greedy
                ) {
                    if let x = value.as(Double.self), let index = Tokens.index(at: x, count: model.count) {
                        Text(model.days[index]).fixedSize()
                    }
                }
                .foregroundStyle(theme.colors.textMuted)
            }
        }
        .chartYAxis {
            // The values on the leading side with faint grid lines, in steps of 1, 2, 5 or 10 as the web picks them.
            AxisMarks(position: .leading, values: Tokens.valueTicks(maximum: model.maximum)) { value in
                AxisGridLine().foregroundStyle(theme.colors.borderSubtle)
                AxisValueLabel {
                    if let number = value.as(Double.self) {
                        Text(number.formatted(.number.locale(locale)))
                    }
                }
                .foregroundStyle(theme.colors.textMuted)
            }
        }
        .chartXSelection(
            value: Binding<Double?>(
                get: { active.map(Double.init) },
                set: { value in setActive(value.flatMap { Tokens.index(at: $0, count: model.count) }) }
            )
        )
    }

    private var dayName: String { labels.day }
    private var valueName: String { labels.remaining }

    private func color(_ line: MonarchBurndownChartLine) -> Color {
        theme.colors[role: MonarchChartLegendTokens.role(Tokens.color(line))]
    }

    @ChartContentBuilder private func lineMarks(
        _ line: MonarchBurndownChartLine,
        runs: [[MonarchBurndownChartTokens.LinePoint]],
        width: CGFloat
    ) -> some ChartContent {
        let stroke = StrokeStyle(
            lineWidth: width,
            lineCap: .round,
            lineJoin: .round,
            dash: Tokens.isDashed(line) ? MonarchLineChartTokens.dash : []
        )
        ForEach(Array(runs.enumerated()), id: \.offset) { run, points in
            ForEach(Array(points.enumerated()), id: \.offset) { _, point in
                LineMark(
                    x: .value(dayName, point.x),
                    y: .value(valueName, point.y),
                    series: .value("Line", "\(line.rawValue)#\(run)")
                )
                .foregroundStyle(color(line))
                .lineStyle(stroke)
            }
        }
    }

    private func lonePoints(_ line: MonarchBurndownChartLine) -> some ChartContent {
        let values = Tokens.values(line, in: model)
        return ForEach(MonarchLineChartTokens.pointIndices(values, showsPoints: false), id: \.self) { index in
            PointMark(x: .value(dayName, Double(index)), y: .value(valueName, values[index] ?? 0))
                .foregroundStyle(color(line))
                .symbolSize(CGSize(width: MonarchLineChartTokens.pointSide, height: MonarchLineChartTokens.pointSide))
        }
    }

    private var scopeChanges: some ChartContent {
        ForEach(model.changes.indices.filter { model.changes[$0] != 0 }, id: \.self) { index in
            PointMark(x: .value(dayName, Double(index)), y: .value(valueName, model.scope[index] ?? 0))
                .foregroundStyle(color(.scope))
                .symbolSize(CGSize(width: MonarchLineChartTokens.pointSide, height: MonarchLineChartTokens.pointSide))
                .annotation(position: .top) {
                    Text(
                        Tokens.changeText(
                            model.changes[index],
                            labels: labels,
                            locale: locale,
                            formatValue: formatValue
                        )
                            ?? ""
                    )
                    .monarchTextStyle(.caption, weight: .medium)
                    .monospacedDigit()
                    .fixedSize()
                    .foregroundStyle(theme.colors[role: Tokens.changeLabelRole])
                }
        }
    }

    private func todayMark(_ index: Int) -> some ChartContent {
        RuleMark(x: .value(dayName, Double(index)))
            .foregroundStyle(theme.colors[role: Tokens.todayRole])
            .lineStyle(StrokeStyle(lineWidth: MonarchLineChartTokens.ruleWidth, dash: Tokens.todayDash))
            .annotation(position: .top, alignment: .center) {
                Text(labels.today)
                    .monarchTextStyle(.caption)
                    .fixedSize()
                    .foregroundStyle(theme.colors[role: Tokens.todayLabelRole])
            }
    }

    @ChartContentBuilder private func crosshair(at index: Int) -> some ChartContent {
        RuleMark(x: .value(dayName, Double(index)))
            .foregroundStyle(theme.colors[role: MonarchLineChartTokens.crosshairRole])
            .lineStyle(StrokeStyle(lineWidth: MonarchLineChartTokens.ruleWidth))
            .annotation(
                position: .top,
                spacing: 0,
                overflowResolution: AnnotationOverflowResolution(x: .fit(to: .chart), y: .fit(to: .chart))
            ) {
                MonarchChartTooltip(
                    title: model.days[index],
                    rows: Tokens.tooltipRows(
                        model,
                        at: index,
                        visible: visible,
                        labels: labels,
                        noValue: noValue,
                        locale: locale,
                        formatValue: formatValue
                    ),
                    footer: Tokens.tooltipFooter(
                        model,
                        at: index,
                        labels: labels,
                        locale: locale,
                        formatValue: formatValue
                    ),
                    marker: .line
                )
            }
        if visible.contains(.remaining), let value = model.remaining[index] {
            PointMark(x: .value(dayName, Double(index)), y: .value(valueName, value))
                .foregroundStyle(color(.remaining))
                .symbolSize(
                    CGSize(
                        width: MonarchLineChartTokens.activePointSide,
                        height: MonarchLineChartTokens.activePointSide
                    )
                )
        }
    }
}
