import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchBurndownChart``.
enum MonarchBurndownChartTokens {
    /// The thickness of the remaining line: the heaviest, it is the one that matters.
    static let remainingWidth: CGFloat = MonarchTokens.Space.space0_5 * 1.25
    /// The thickness of the scope step line.
    static let scopeWidth: CGFloat = MonarchTokens.Space.space0_5 * 0.75
    /// The dash of the today marker.
    static let todayDash: [CGFloat] = [MonarchTokens.Space.space0_5, MonarchTokens.Space.space1 * 0.75]
    /// The most day labels on the x axis, so they never collide.
    static let maximumTicks = 5
    /// The colour slot of each line.
    static func color(_ line: MonarchBurndownChartLine) -> MonarchChartColor {
        switch line {
        case .remaining: .data1
        case .ideal: .neutral
        case .scope: .data2
        }
    }
    /// Whether a line is dashed: the ideal, so it never reads as data.
    static func isDashed(_ line: MonarchBurndownChartLine) -> Bool { line == .ideal }
    /// The colour of the today marker.
    static var todayRole: MonarchColorRole { \.borderStrong }
    /// The colour of the today label.
    static var todayLabelRole: MonarchColorRole { \.textMuted }
    /// The colour of a scope change label.
    static var changeLabelRole: MonarchColorRole { \.textDefault }

    /// Everything the chart draws, worked out from the points.
    struct Model: Equatable {
        /// The day labels.
        let days: [String]
        /// The work left each day; `nil` for days not reached yet.
        let remaining: [Double?]
        /// The scope each day; `nil` where it is not tracked.
        let scope: [Double?]
        /// The ideal each day; all `nil` when there is no starting work.
        let ideal: [Double?]
        /// The scope change each day: 0 where it did not change.
        let changes: [Double]
        /// Whether the scope line is drawn.
        let showsScope: Bool
        /// The index of the latest day with a remaining value.
        let lastActual: Int?
        /// The index of the today marker.
        let today: Int?

        var count: Int { days.count }
        /// The largest value any line reaches.
        var maximum: Double { [remaining, scope, ideal].flatMap { $0 }.compactMap { $0 }.max() ?? 0 }
    }

    /// The model of a chart.
    ///
    /// - Parameters:
    ///   - points: The days, in order.
    ///   - idealStart: The start of the ideal line. Default: the first day's scope, else its remaining.
    ///   - todayIndex: The today marker. Default: the latest day with data, when later days exist.
    ///   - showsToday: Whether there is a today marker at all.
    ///   - showsScope: Whether the scope line is drawn, when any day has a scope.
    ///   - dayLabel: Writes a point's day.
    /// - Returns: The days, lines, changes and markers to draw.
    static func model(
        _ points: [MonarchBurndownChartPoint],
        idealStart: Double?,
        todayIndex: Int?,
        showsToday: Bool,
        showsScope: Bool,
        dayLabel: (MonarchBurndownChartPoint) -> String
    ) -> Model {
        let remaining = points.map { finite($0.remaining) }
        let scope = points.map { finite($0.scope) }
        let start = finite(idealStart) ?? scope.first.flatMap { $0 } ?? remaining.first.flatMap { $0 }
        let last = remaining.lastIndex { $0 != nil }
        return Model(
            days: points.map(dayLabel),
            remaining: remaining,
            scope: scope,
            ideal: ideal(start: start, count: points.count),
            changes: changes(scope),
            showsScope: showsScope && scope.contains { $0 != nil },
            lastActual: last,
            today: showsToday ? today(todayIndex, lastActual: last, count: points.count) : nil
        )
    }

    private static func finite(_ value: Double?) -> Double? {
        guard let value, value.isFinite else { return nil }
        return value
    }

    /// The ideal line: from `start` on the first day to zero on the last,
    /// in equal steps. A single day keeps the start.
    static func ideal(start: Double?, count: Int) -> [Double?] {
        (0..<max(0, count)).map { index in
            guard let start else { return nil }
            guard count > 1 else { return start }
            return start * (1 - Double(index) / Double(count - 1))
        }
    }

    /// The change of scope from the day before: 0 on the first day, where
    /// it did not change, and next to a day without scope.
    static func changes(_ scope: [Double?]) -> [Double] {
        scope.indices.map { index in
            guard index > 0, let value = scope[index], let previous = scope[index - 1] else { return 0 }
            return value - previous
        }
    }

    /// The today marker: the given index when it is a day, or by default the
    /// latest day with data when later days exist.
    static func today(_ index: Int?, lastActual: Int?, count: Int) -> Int? {
        if let index { return (0..<count).contains(index) ? index : nil }
        guard let lastActual, lastActual < count - 1 else { return nil }
        return lastActual
    }

    /// A point of a line, with the day as a number so steps can sit between days.
    struct LinePoint: Equatable {
        let x: Double
        let y: Double
    }

    /// The runs of a line through the values: a gap breaks it.
    static func lineRuns(_ values: [Double?]) -> [[LinePoint]] {
        MonarchLineChartTokens.runs(values).map { run in
            run.compactMap { index in values[index].map { LinePoint(x: Double(index), y: $0) } }
        }
    }

    /// The scope as a step line: it holds its value until the day it
    /// changes, then moves straight up or down. A gap breaks it.
    static func scopeRuns(_ scope: [Double?]) -> [[LinePoint]] {
        MonarchLineChartTokens.runs(scope).map { run in
            var points: [LinePoint] = []
            for index in run {
                guard let value = scope[index] else { continue }
                if let previous = points.last, previous.y != value {
                    points.append(LinePoint(x: Double(index), y: previous.y))
                }
                points.append(LinePoint(x: Double(index), y: value))
            }
            return points
        }
    }

    /// Where the sprint stands on the latest day with data.
    enum Trend: Equatable {
        case notStarted
        case ahead(Double)
        case behind(Double)
        case onTrack
    }

    /// The gap between the latest remaining value and the ideal on that
    /// day, rounded to two decimals so float noise is never "behind".
    static func trend(_ model: Model) -> Trend {
        guard let last = model.lastActual, let remaining = model.remaining[last] else { return .notStarted }
        let ideal = model.ideal[last] ?? remaining
        let difference = ((remaining - ideal) * 100).rounded() / 100
        if difference > 0 { return .behind(difference) }
        if difference < 0 { return .ahead(-difference) }
        return .onTrack
    }

    /// A value as the chart writes it.
    static func valueText(_ value: Double, locale: Locale, formatValue: ((Double) -> String)?) -> String {
        MonarchLineChartTokens.valueText(value, locale: locale, formatValue: formatValue)
    }

    /// A scope change with its sign, or `nil` for no change.
    static func changeText(
        _ change: Double,
        labels: MonarchBurndownChartLabels,
        locale: Locale,
        formatValue: ((Double) -> String)?
    ) -> String? {
        guard change != 0 else { return nil }
        return labels.change(valueText(abs(change), locale: locale, formatValue: formatValue), change > 0)
    }

    /// The generated summary: where the sprint stands and, with scope, how it changed.
    static func summary(
        _ model: Model,
        labels: MonarchBurndownChartLabels,
        locale: Locale,
        formatValue: ((Double) -> String)?
    ) -> String {
        let text = { (value: Double) in valueText(value, locale: locale, formatValue: formatValue) }
        var sentences: [String] = []
        let trend = trend(model)
        if let last = model.lastActual, let remaining = model.remaining[last] {
            let ideal = model.ideal[last] ?? remaining
            let position: String
            switch trend {
            case .behind(let amount): position = labels.behind(text(amount))
            case .ahead(let amount): position = labels.ahead(text(amount))
            case .onTrack, .notStarted: position = labels.onTrack
            }
            sentences.append("\(labels.status(model.days[last], text(remaining), text(ideal))), \(position)")
        } else {
            sentences.append(labels.notStarted)
        }
        if model.showsScope, let scope = model.scope.last(where: { $0 != nil }).flatMap({ $0 }) {
            let added = model.changes.filter { $0 > 0 }.reduce(0, +)
            let removed = -model.changes.filter { $0 < 0 }.reduce(0, +)
            sentences.append(labels.scopeSummary(text(scope), text(added), text(removed)))
        }
        return sentences.joined(separator: ". ")
    }

    /// The data table: day, remaining, ideal and, with scope, scope and its change.
    static func table(
        _ model: Model,
        labels: MonarchBurndownChartLabels,
        locale: Locale,
        formatValue: ((Double) -> String)?
    ) -> MonarchChartTable {
        let column = { (name: String, values: [Double?]) in
            MonarchChartTableSeries(
                name,
                values: values,
                formattedValues: values.map { value in
                    value.map { valueText($0, locale: locale, formatValue: formatValue) } ?? ""
                }
            )
        }
        var series = [column(labels.remaining, model.remaining), column(labels.ideal, model.ideal)]
        if model.showsScope {
            series.append(column(labels.scope, model.scope))
            let changes: [Double?] = model.scope.indices.map { model.scope[$0] == nil ? nil : model.changes[$0] }
            series.append(
                MonarchChartTableSeries(
                    labels.scopeChange,
                    values: changes,
                    formattedValues: changes.map { change in
                        guard let change else { return "" }
                        return changeText(change, labels: labels, locale: locale, formatValue: formatValue)
                            ?? valueText(0, locale: locale, formatValue: formatValue)
                    }
                )
            )
        }
        return MonarchChartTable(categoryTitle: labels.day, categories: model.days, series: series)
    }

    /// The lines shown in the legend: the scope only when it is drawn.
    static func lines(_ model: Model) -> [MonarchBurndownChartLine] {
        model.showsScope ? [.remaining, .ideal, .scope] : [.remaining, .ideal]
    }

    /// The legend entries.
    static func legend(_ model: Model, labels: MonarchBurndownChartLabels) -> [MonarchChartLegendItem] {
        lines(model).map { line in
            MonarchChartLegendItem(
                line.rawValue,
                label: labels.name(line),
                color: color(line),
                isDashed: isDashed(line)
            )
        }
    }

    /// The lines that are drawn: those in the legend that are not hidden.
    static func visible(_ model: Model, hidden: Set<MonarchBurndownChartLine>) -> [MonarchBurndownChartLine] {
        lines(model).filter { !hidden.contains($0) }
    }

    /// The values of a line.
    static func values(_ line: MonarchBurndownChartLine, in model: Model) -> [Double?] {
        switch line {
        case .remaining: model.remaining
        case .ideal: model.ideal
        case .scope: model.scope
        }
    }

    /// The popover rows at a day: every visible line and its value, or the
    /// "No value" text for a gap.
    static func tooltipRows(
        _ model: Model,
        at index: Int,
        visible: [MonarchBurndownChartLine],
        labels: MonarchBurndownChartLabels,
        noValue: String,
        locale: Locale,
        formatValue: ((Double) -> String)?
    ) -> [MonarchChartTooltip.Row] {
        visible.map { line in
            let values = values(line, in: model)
            let value = values.indices.contains(index) ? values[index] : nil
            return MonarchChartTooltip.Row(
                key: line.rawValue,
                label: labels.name(line),
                value: value.map { valueText($0, locale: locale, formatValue: formatValue) } ?? noValue,
                role: MonarchChartLegendTokens.role(color(line)),
                isDashed: isDashed(line)
            )
        }
    }

    /// The popover footer: the scope change that day, when there is one.
    static func tooltipFooter(
        _ model: Model,
        at index: Int,
        labels: MonarchBurndownChartLabels,
        locale: Locale,
        formatValue: ((Double) -> String)?
    ) -> String? {
        guard model.showsScope, model.changes.indices.contains(index),
            let change = changeText(model.changes[index], labels: labels, locale: locale, formatValue: formatValue)
        else { return nil }
        return "\(labels.scopeChange) \(change)"
    }

    /// The x domain: the days from first to last, or around a single day.
    static func xDomain(count: Int) -> ClosedRange<Double> {
        count > 1 ? 0...Double(count - 1) : -1...1
    }

    /// The day nearest to a touched x position.
    static func index(at x: Double, count: Int) -> Int? {
        guard count > 0, x.isFinite else { return nil }
        return min(max(Int(x.rounded()), 0), count - 1)
    }

    /// Nice tick values from zero up to a round number above `maximum`, as the web's `niceTicks` picks them: a
    /// step of 1, 2, 2.5, 5 or 10 times a power of ten close to `maximum / count`.
    static func valueTicks(maximum: Double, count: Int = 4) -> [Double] {
        guard maximum.isFinite, maximum > 0, count > 0 else { return [0] }
        let raw = maximum / Double(count)
        let power = pow(10, floor(log10(raw)))
        let unit = raw / power
        let step = power * (unit <= 1 ? 1 : unit <= 2 ? 2 : unit <= 2.5 ? 2.5 : unit <= 5 ? 5 : 10)
        let end = (maximum / step - 1e-9).rounded(.up)
        return (0...Int(min(end, 12))).map { Double($0) * step }
    }

    /// The days labelled on the x axis: the first, the last and evenly
    /// spaced days between, at most `maximum`.
    static func tickIndices(count: Int, maximum: Int = maximumTicks) -> [Int] {
        guard count > 0 else { return [] }
        guard count > 1, maximum > 1 else { return [0] }
        let step = Int((Double(count - 1) / Double(maximum - 1)).rounded(.up))
        var ticks = Array(stride(from: 0, to: count - 1, by: max(1, step)))
        if let last = ticks.last, count - 1 - last < max(1, step) / 2 + 1, ticks.count > 1, step > 1 {
            ticks.removeLast()
        }
        ticks.append(count - 1)
        return ticks
    }

    /// Where a day label sits against its tick: the first starts at the tick, the last ends at it, the rest centre.
    static func labelAnchor(at x: Double?, count: Int) -> UnitPoint {
        guard let x, count > 1 else { return .top }
        if x <= 0 { return .topLeading }
        if x >= Double(count - 1) { return .topTrailing }
        return .top
    }
}
