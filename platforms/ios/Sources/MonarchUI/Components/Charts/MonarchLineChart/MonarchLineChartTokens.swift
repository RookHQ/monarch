import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchLineChart``.
enum MonarchLineChartTokens {
    /// The thickness of a series line.
    static let lineWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The thickness of the crosshair and of reference lines.
    static let ruleWidth: CGFloat = MonarchShapes.borderWidth
    /// The dash of a dashed series, the same as its legend marker.
    static let dash: [CGFloat] = MonarchChartLegendTokens.dash
    /// The dash of a reference line.
    static let referenceDash: [CGFloat] = [MonarchTokens.Space.space1, MonarchTokens.Space.space1]
    /// The side of a point: a lone point between gaps, or every point with `showsPoints`.
    static let pointSide: CGFloat = MonarchTokens.Space.space2
    /// The side of the point drawn on each series at the inspected x.
    static let activePointSide: CGFloat = MonarchTokens.Space.space2_5
    /// The strength of the area fill under a line, as a share of the series colour.
    static let areaFillOpacity: Double = 0.16
    /// The padding inside the inspection popover.
    static let tooltipPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between rows of the inspection popover.
    static let tooltipSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the marker, the name and the value in a popover row.
    static let tooltipColumnSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The colour of the crosshair.
    static var crosshairRole: MonarchColorRole { \.borderStrong }
    /// The colour of reference lines and their labels.
    static var referenceRole: MonarchColorRole { \.textMuted }
    /// The default date format of the x values.
    static let defaultDateFormat: Date.FormatStyle = .dateTime.month(.abbreviated).day()

    /// The x values of a chart: dates spaced by time, or categories spaced evenly.
    enum XValues: Equatable {
        case dates([Date])
        case categories([String])

        var count: Int {
            switch self {
            case .dates(let dates): dates.count
            case .categories(let categories): categories.count
            }
        }
    }

    /// A series ready to draw: values matched to the x values and a colour slot.
    struct Series: Equatable, Identifiable {
        let key: String
        let label: String
        let values: [Double?]
        let color: MonarchChartColor
        let isDashed: Bool

        var id: String { key }
        var role: MonarchColorRole { MonarchChartLegendTokens.role(color) }
    }

    /// The series with one value per x value: missing values and values
    /// that are not finite are gaps, extra values are dropped. Colour slots
    /// follow the position in the list, so hiding one never repaints the others.
    static func resolve(_ series: [MonarchLineChartSeries], count: Int) -> [Series] {
        series.enumerated().map { index, item in
            Series(
                key: item.key,
                label: item.label,
                values: (0..<max(0, count)).map { position in
                    guard item.values.indices.contains(position), let value = item.values[position], value.isFinite
                    else { return nil }
                    return value
                },
                color: MonarchChartLegendTokens.color(item.color, index: index),
                isDashed: item.isDashed
            )
        }
    }

    /// The runs of consecutive values, as indices. Each run is drawn as its
    /// own line, so a gap breaks the line instead of bridging it.
    static func runs(_ values: [Double?]) -> [[Int]] {
        var runs: [[Int]] = []
        var current: [Int] = []
        for (index, value) in values.enumerated() {
            if value == nil {
                if !current.isEmpty { runs.append(current) }
                current = []
            } else {
                current.append(index)
            }
        }
        if !current.isEmpty { runs.append(current) }
        return runs
    }

    /// The indices that get a point: every value with `showsPoints`, else
    /// only lone values between gaps, which no line can show.
    static func pointIndices(_ values: [Double?], showsPoints: Bool) -> [Int] {
        if showsPoints { return values.indices.filter { values[$0] != nil } }
        return runs(values).filter { $0.count == 1 }.flatMap { $0 }
    }

    /// The index of the date nearest to `date`.
    static func nearestIndex(_ dates: [Date], to date: Date) -> Int? {
        dates.indices.min { abs(dates[$0].timeIntervalSince(date)) < abs(dates[$1].timeIntervalSince(date)) }
    }

    /// Where an area's fill stops: zero inside a fixed domain, zero when
    /// zero is kept, otherwise the lowest visible value.
    static func areaBaseline(_ values: [Double], domain: ClosedRange<Double>?, includesZero: Bool) -> Double {
        if let domain { return min(max(0, domain.lowerBound), domain.upperBound) }
        if includesZero { return 0 }
        return values.min() ?? 0
    }

    /// The frame's status: empty when a ready chart has no x values.
    static func status(_ status: MonarchChartFrameStatus, count: Int) -> MonarchChartFrameStatus {
        status == .ready && count == 0 ? .empty : status
    }

    /// A value as the chart writes it.
    static func valueText(_ value: Double, locale: Locale, formatValue: ((Double) -> String)?) -> String {
        formatValue?(value) ?? value.formatted(.number.locale(locale))
    }

    /// A day as the web writes it on a chart: the abbreviated month, then the day ("Mar 1").
    static func dayLabel(_ date: Date, locale: Locale) -> String {
        date.formatted(
            .verbatim(
                "\(month: .abbreviated) \(day: .defaultDigits)",
                locale: locale,
                timeZone: .autoupdatingCurrent,
                calendar: .autoupdatingCurrent
            )
        )
    }

    /// The x values as text, for the popover and the data table.
    static func xLabels(_ xs: XValues, dateFormat: Date.FormatStyle, locale: Locale) -> [String] {
        switch xs {
        case .dates(let dates):
            // The default is the web's "Mar 1": month first, whatever the locale's own order.
            dates.map {
                dateFormat == defaultDateFormat ? dayLabel($0, locale: locale) : $0.formatted(dateFormat.locale(locale))
            }
        case .categories(let categories): categories
        }
    }

    /// The data table: every series, hidden ones included, with the
    /// chart's own text for each value.
    static func table(
        _ series: [Series],
        xLabels: [String],
        xTitle: String,
        valueTitle: String?,
        locale: Locale,
        formatValue: ((Double) -> String)?
    ) -> MonarchChartTable {
        MonarchChartTable(
            categoryTitle: xTitle,
            categories: xLabels,
            series: series.map { item in
                MonarchChartTableSeries(
                    item.label,
                    values: item.values,
                    formattedValues: item.values.map { value in
                        value.map { valueText($0, locale: locale, formatValue: formatValue) } ?? ""
                    }
                )
            },
            valueTitle: valueTitle
        )
    }

    /// The legend entries: one per series, or none for a single series,
    /// whose name is the title.
    static func legend(_ series: [Series]) -> [MonarchChartLegendItem] {
        guard series.count > 1 else { return [] }
        return series.map { MonarchChartLegendItem($0.key, label: $0.label, color: $0.color, isDashed: $0.isDashed) }
    }

    /// The popover rows at an index: every visible series and its value, or
    /// the "No value" text for a gap.
    static func tooltipRows(
        _ series: [Series],
        at index: Int,
        noValue: String,
        locale: Locale,
        formatValue: ((Double) -> String)?
    ) -> [MonarchChartTooltip.Row] {
        series.map { item in
            let value = item.values.indices.contains(index) ? item.values[index] : nil
            return MonarchChartTooltip.Row(
                key: item.key,
                label: item.label,
                value: value.map { valueText($0, locale: locale, formatValue: formatValue) } ?? noValue,
                role: item.role,
                isDashed: item.isDashed
            )
        }
    }

    /// The text above a reference line: its label and value.
    static func referenceText(
        _ line: MonarchLineChartReferenceLine,
        locale: Locale,
        formatValue: ((Double) -> String)?
    ) -> String {
        // As the web: the target is short ("Target 1.2K") unless the app formats its values itself.
        let value =
            formatValue?(line.value) ?? line.value.formatted(.number.notation(.compactName).locale(locale))
        guard let label = line.label, !label.isEmpty else { return value }
        return "\(label) \(value)"
    }
}
