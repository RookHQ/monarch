import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchBarChart``.
enum MonarchBarChartTokens {
    /// The height of a vertical chart: 240 points, as the chart frame.
    static let verticalHeight: CGFloat = MonarchChartFrameTokens.defaultHeight
    /// The room each category takes in a horizontal chart.
    static let horizontalRowHeight: CGFloat = MonarchTokens.Space.space8
    /// Room for the value axis below a horizontal chart.
    static let horizontalAxisRoom: CGFloat = MonarchTokens.Space.space8
    /// The least height of a horizontal chart: 120 points.
    static let horizontalMinimumHeight: CGFloat = MonarchTokens.Space.space20 + MonarchTokens.Space.space10
    /// The corner radius of a bar's end.
    static let barRadius: CGFloat = MonarchTokens.Radius.xs
    /// The share of the value range left between the parts of a stacked bar
    /// when `segmentSeparator` is on.
    static let separatorShare: Double = 0.006
    /// The share of a part that the gap may take at most, so a thin part stays visible.
    static let separatorLimit: Double = 0.25

    /// The largest distance from zero that any bar reaches.
    static func extent(_ segments: [Segment]) -> Double {
        segments.map { max(abs($0.start), abs($0.end)) }.max() ?? 0
    }

    /// `segment` with its two ends pulled in, so a thin gap shows between
    /// stacked parts. Unchanged when `isOn` is false.
    static func separated(_ segment: Segment, isOn: Bool, extent: Double) -> Segment {
        guard isOn else { return segment }
        let length = abs(segment.end - segment.start)
        let inset = min(extent * separatorShare / 2, length * separatorLimit / 2)
        let direction: Double = segment.end >= segment.start ? 1 : -1
        return Segment(
            categoryIndex: segment.categoryIndex,
            category: segment.category,
            seriesKey: segment.seriesKey,
            color: segment.color,
            value: segment.value,
            start: segment.start + direction * inset,
            end: segment.end - direction * inset
        )
    }

    /// The share of a category's band the bars fill.
    static let bandFill: CGFloat = 0.7
    /// How far a bar fill moves from its data colour towards the surface. None: the bars are the data colours at
    /// full strength, as the web draws them (the web example is the reference for colour).
    static let softening: Double = 0
    /// The opacity of the categories that are not highlighted. A named ratio:
    /// the chart roles have no dimmed variant.
    static let dimmedOpacity: Double = 0.4
    /// The width of a reference line.
    static let referenceWidth: CGFloat = MonarchTokens.Space.px
    /// The dash and gap of a reference line.
    static let referenceDash: [CGFloat] = [MonarchTokens.Space.space1, MonarchTokens.Space.space1]
    /// The gap between a bar's end and its value label.
    static let valueLabelSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the plot and the inspection row.
    static let detailSpacing: CGFloat = MonarchTokens.Space.space2
    /// The type role of value labels, reference labels and the inspection row.
    static let labelStyle: MonarchTextStyle = .caption
    /// The colour of the band behind the inspected category.
    static var bandRole: MonarchColorRole { \.bgMuted }
    /// The colour of a reference line.
    static var referenceRole: MonarchColorRole { \.textSubtle }
    /// The colour of value and reference labels.
    static var labelRole: MonarchColorRole { \.textMuted }

    // MARK: Data

    /// One bar, or one part of a stacked bar.
    struct Segment: Hashable, Identifiable {
        /// The position of the category.
        let categoryIndex: Int
        /// The category.
        let category: String
        /// The key of the series.
        let seriesKey: String
        /// The colour slot of the series.
        let color: MonarchChartColor
        /// The value.
        let value: Double
        /// Where the bar starts on the value axis.
        let start: Double
        /// Where the bar ends on the value axis.
        let end: Double

        /// The category and the series.
        var id: String { "\(categoryIndex)-\(seriesKey)" }
    }

    /// The value of a series in a category, or `nil` for a gap or a value
    /// that is not finite.
    static func value(_ series: MonarchBarChartSeries, at index: Int) -> Double? {
        guard series.values.indices.contains(index), let value = series.values[index], value.isFinite else {
            return nil
        }
        return value
    }

    /// The colour slot of a series: its own, or the slot of its position
    /// among every series (hidden ones included, so colours do not move).
    static func color(_ series: MonarchBarChartSeries, index: Int) -> MonarchChartColor {
        MonarchChartLegendTokens.color(series.color, index: index)
    }

    /// The theme role of a colour slot.
    static func role(_ color: MonarchChartColor) -> MonarchColorRole {
        MonarchChartLegendTokens.role(color)
    }

    /// The series that are shown, with their colour slots.
    static func visible(
        _ series: [MonarchBarChartSeries],
        hidden: Set<String>
    ) -> [(series: MonarchBarChartSeries, color: MonarchChartColor)] {
        series.enumerated()
            .filter { !hidden.contains($0.element.key) }
            .map { (series: $0.element, color: color($0.element, index: $0.offset)) }
    }

    /// The bars to draw. Grouped bars start at zero; stacked bars start
    /// where the previous series of the same sign ended. Gaps draw nothing.
    static func segments(
        categories: [String],
        visible: [(series: MonarchBarChartSeries, color: MonarchChartColor)],
        layout: MonarchBarChartLayout
    ) -> [Segment] {
        var result: [Segment] = []
        for (index, category) in categories.enumerated() {
            var positive = 0.0
            var negative = 0.0
            for (series, color) in visible {
                guard let value = value(series, at: index) else { continue }
                var start = 0.0
                if layout == .stacked {
                    start = value >= 0 ? positive : negative
                    if value >= 0 { positive += value } else { negative += value }
                }
                result.append(
                    Segment(
                        categoryIndex: index,
                        category: category,
                        seriesKey: series.key,
                        color: color,
                        value: value,
                        start: start,
                        end: start + value
                    )
                )
            }
        }
        return result
    }

    /// The sum of the shown values of a category; gaps count as zero.
    static func total(
        at index: Int,
        visible: [(series: MonarchBarChartSeries, color: MonarchChartColor)]
    ) -> Double {
        visible.reduce(0) { $0 + (value($1.series, at: index) ?? 0) }
    }

    /// The value label of each bar, by segment id. Grouped bars show their
    /// own value; a stack shows its total once, at the end of the last part
    /// with the total's sign.
    static func valueLabels(_ segments: [Segment], layout: MonarchBarChartLayout) -> [String: Double] {
        guard layout == .stacked else {
            return Dictionary(uniqueKeysWithValues: segments.map { ($0.id, $0.value) })
        }
        var labels: [String: Double] = [:]
        for index in Set(segments.map(\.categoryIndex)) {
            let parts = segments.filter { $0.categoryIndex == index }
            let total = parts.reduce(0) { $0 + $1.value }
            let end = parts.last { total >= 0 ? $0.value > 0 : $0.value < 0 } ?? parts.last
            if let end { labels[end.id] = total }
        }
        return labels
    }

    // MARK: State

    /// The status to show: an empty state when there is nothing to draw.
    static func resolvedStatus(
        _ status: MonarchChartFrameStatus,
        categories: [String],
        series: [MonarchBarChartSeries]
    ) -> MonarchChartFrameStatus {
        status == .ready && (categories.isEmpty || series.isEmpty) ? .empty : status
    }

    /// Whether the chart has a legend: with two or more series.
    static func showsLegend(seriesCount: Int) -> Bool {
        seriesCount > 1
    }

    /// Whether the bars of a category are dimmed: when another is highlighted.
    static func isDimmed(_ index: Int, highlighted: Int?) -> Bool {
        guard let highlighted else { return false }
        return highlighted != index
    }

    /// The height of the chart: given, 240 points for columns, or 32 points
    /// per category (at least 120) for bars.
    static func height(_ given: CGFloat?, orientation: MonarchBarChartOrientation, count: Int) -> CGFloat {
        if let given { return given }
        switch orientation {
        case .vertical:
            return verticalHeight
        case .horizontal:
            return max(horizontalMinimumHeight, CGFloat(count) * horizontalRowHeight + horizontalAxisRoom)
        }
    }

    /// The position of an inspected category, or `nil`.
    static func index(of category: String?, in categories: [String]) -> Int? {
        guard let category else { return nil }
        return categories.firstIndex(of: category)
    }

    // MARK: Text

    /// The text of one value: the app's formatting, or the locale's number.
    static func text(_ value: Double, format: ((Double) -> String)?) -> String {
        format?(value) ?? value.formatted()
    }

    /// What the inspection row and VoiceOver read for a category: the
    /// category, then each shown series and its value, then the stack's
    /// total.
    static func inspection(
        at index: Int,
        categories: [String],
        visible: [(series: MonarchBarChartSeries, color: MonarchChartColor)],
        layout: MonarchBarChartLayout,
        format: ((Double) -> String)?,
        noValue: String,
        total: String
    ) -> String {
        let category = categories.indices.contains(index) ? categories[index] : ""
        var parts = visible.map { entry in
            "\(entry.series.label) \(value(entry.series, at: index).map { text($0, format: format) } ?? noValue)"
        }
        if layout == .stacked && visible.count > 1 {
            parts.append("\(total) \(text(self.total(at: index, visible: visible), format: format))")
        }
        return "\(category): \(parts.joined(separator: ", "))"
    }

    /// The generated summary: the number and range of categories, each
    /// shown series' total and highest category, and each reference line.
    static func summary(
        categories: [String],
        visible: [(series: MonarchBarChartSeries, color: MonarchChartColor)],
        referenceLines: [MonarchBarChartReferenceLine],
        format: ((Double) -> String)?,
        noData: String
    ) -> String {
        guard let first = categories.first, let last = categories.last else { return noData }
        var sentences = [
            categories.count == 1
                ? MonarchBarChartStrings.onePoint(first)
                : MonarchBarChartStrings.points(count: categories.count.formatted(), first: first, last: last)
        ]
        for (series, _) in visible {
            let values = categories.indices.compactMap { index in value(series, at: index).map { (index, $0) } }
            guard let highest = values.max(by: { $0.1 < $1.1 }) else {
                sentences.append("\(series.label): \(noData)")
                continue
            }
            let sum = values.reduce(0) { $0 + $1.1 }
            sentences.append(
                MonarchBarChartStrings.series(
                    series.label,
                    total: text(sum, format: format),
                    highest: categories[highest.0],
                    value: text(highest.1, format: format)
                )
            )
        }
        for line in referenceLines {
            let value = text(line.value, format: format)
            sentences.append(line.label.map { "\($0) \(value)" } ?? value)
        }
        return sentences.joined(separator: ". ")
    }

    /// The data table: one column per series, hidden ones included, and a
    /// total column when several series are stacked.
    static func table(
        categoryTitle: String,
        categories: [String],
        series: [MonarchBarChartSeries],
        layout: MonarchBarChartLayout,
        valueTitle: String?,
        format: ((Double) -> String)?,
        total: String
    ) -> MonarchChartTable {
        var columns = series.map { series in
            let values = categories.indices.map { value(series, at: $0) }
            return MonarchChartTableSeries(
                series.label,
                values: values,
                formattedValues: values.map { $0.map { text($0, format: format) } ?? "" }
            )
        }
        if layout == .stacked && series.count > 1 {
            let all = series.enumerated().map { (series: $0.element, color: color($0.element, index: $0.offset)) }
            let totals = categories.indices.map { self.total(at: $0, visible: all) }
            columns.append(
                MonarchChartTableSeries(total, values: totals, formattedValues: totals.map { text($0, format: format) })
            )
        }
        return MonarchChartTable(
            categoryTitle: categoryTitle,
            categories: categories,
            series: columns,
            valueTitle: valueTitle
        )
    }

    /// The name of the VoiceOver action that opens a category.
    static func detailsActionName(_ details: String, category: String) -> String {
        "\(details), \(category)"
    }
}
