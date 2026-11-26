import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAreaChart``. Series
/// resolution, gaps, x labels and value text are shared with
/// ``MonarchLineChart`` through `MonarchLineChartTokens`.
enum MonarchAreaChartTokens {
    /// The strength of an area's fill, as a share of the series colour.
    /// Translucent, so overlapping areas and the grid stay visible.
    static let overlapFillOpacity: Double = 0.14
    /// The strength of a stacked band's fill: stronger, because bands touch.
    static let stackedFillOpacity: Double = 0.32
    /// The strength of a dashed series' fill, so a comparison stays in the background.
    static let dashedFillOpacity: Double = 0.06
    /// The share ticks of a 100% stacked chart.
    static let shareTicks: [Double] = [0, 0.25, 0.5, 0.75, 1]

    /// The lower and upper edge of one series at one x value, in values or,
    /// when stacked to 100%, in shares of one.
    struct Band: Equatable {
        let low: Double
        let high: Double
    }

    /// A series' bands: `nil` where its value is a gap.
    struct Layer: Equatable, Identifiable {
        let series: MonarchLineChartTokens.Series
        let bands: [Band?]

        var id: String { series.key }
    }

    /// The bands of the visible series and the stack total at every x value.
    struct Stack: Equatable {
        let layers: [Layer]
        /// The sum of the visible series at each x value, negatives counted as zero.
        let totals: [Double]
    }

    /// The stack of the visible series, first series at the bottom.
    ///
    /// Overlapping areas run from `baseline` to the value. Stacked areas
    /// pile the values up; 100% stacks pile the shares of each x value's
    /// total. In both stacks a negative value counts as zero and a gap adds
    /// nothing, so the series above it keep their place.
    static func stack(
        _ series: [MonarchLineChartTokens.Series],
        count: Int,
        stacking: MonarchAreaChartStacking,
        baseline: Double
    ) -> Stack {
        let positions = 0..<max(0, count)
        let totals = positions.map { index in
            series.reduce(0) { sum, item in sum + max(0, value(item, at: index) ?? 0) }
        }
        var base = positions.map { _ in 0.0 }
        let layers = series.map { item in
            Layer(
                series: item,
                bands: positions.map { index -> Band? in
                    guard let value = value(item, at: index) else { return nil }
                    switch stacking {
                    case .overlap:
                        return Band(low: baseline, high: value)
                    case .stacked, .expand:
                        let part = max(0, value)
                        let total = totals[index]
                        let scaled = stacking == .expand ? (total > 0 ? part / total : 0) : part
                        let low = base[index]
                        base[index] = low + scaled
                        return Band(low: low, high: low + scaled)
                    }
                }
            )
        }
        return Stack(layers: layers, totals: totals)
    }

    private static func value(_ series: MonarchLineChartTokens.Series, at index: Int) -> Double? {
        series.values.indices.contains(index) ? series.values[index] : nil
    }

    /// Where an overlapping area's fill stops. Stacks always start at zero.
    static func baseline(
        _ series: [MonarchLineChartTokens.Series],
        stacking: MonarchAreaChartStacking,
        domain: ClosedRange<Double>?,
        includesZero: Bool
    ) -> Double {
        guard stacking == .overlap else { return 0 }
        return MonarchLineChartTokens.areaBaseline(
            series.flatMap { $0.values.compactMap { $0 } },
            domain: domain,
            includesZero: includesZero
        )
    }

    /// The fixed value axis: 0 to 1 for a 100% stack, the app's domain
    /// otherwise, or `nil` to let Swift Charts fit the data.
    static func valueDomain(_ domain: ClosedRange<Double>?, stacking: MonarchAreaChartStacking) -> ClosedRange<Double>?
    {
        stacking == .expand ? 0...1 : domain
    }

    /// Whether the value axis keeps zero: always for a stack.
    static func includesZero(_ includesZero: Bool, stacking: MonarchAreaChartStacking) -> Bool {
        stacking != .overlap || includesZero
    }

    /// The reference lines that are drawn: none in a 100% stack, whose axis is shares.
    static func referenceLines(
        _ lines: [MonarchLineChartReferenceLine],
        stacking: MonarchAreaChartStacking
    ) -> [MonarchLineChartReferenceLine] {
        stacking == .expand ? [] : lines
    }

    /// Whether a total is shown: in a stack of two or more series.
    static func showsTotal(stacking: MonarchAreaChartStacking, seriesCount: Int) -> Bool {
        stacking != .overlap && seriesCount > 1
    }

    /// The fill strength of an area: faint for a dashed series, stronger in a stack.
    static func fillOpacity(stacking: MonarchAreaChartStacking, isDashed: Bool) -> Double {
        if isDashed { return dashedFillOpacity }
        return stacking == .overlap ? overlapFillOpacity : stackedFillOpacity
    }

    /// A share of one as a percentage, with up to one decimal.
    static func shareText(_ share: Double, locale: Locale) -> String {
        share.formatted(.percent.precision(.fractionLength(0...1)).locale(locale))
    }

    /// A share tick of the value axis, as a whole percentage.
    static func tickText(_ share: Double, locale: Locale) -> String {
        share.formatted(.percent.precision(.fractionLength(0)).locale(locale))
    }

    /// The text of one series at one x value: its value, with its share of
    /// the total in a 100% stack, or `noValue` for a gap. The real value is
    /// kept, so a negative value stays negative in the text.
    static func valueText(
        _ value: Double?,
        total: Double,
        stacking: MonarchAreaChartStacking,
        noValue: String,
        locale: Locale,
        formatValue: ((Double) -> String)?,
        share: (String, String) -> String
    ) -> String {
        guard let value else { return noValue }
        let text = MonarchLineChartTokens.valueText(value, locale: locale, formatValue: formatValue)
        guard stacking == .expand else { return text }
        let part = total > 0 ? max(0, value) / total : 0
        return share(text, shareText(part, locale: locale))
    }

    /// The popover rows at an index: the visible series top of the stack
    /// first, so they read in the order they are drawn.
    static func tooltipRows(
        _ stack: Stack,
        at index: Int,
        stacking: MonarchAreaChartStacking,
        noValue: String,
        locale: Locale,
        formatValue: ((Double) -> String)?,
        share: (String, String) -> String
    ) -> [MonarchChartTooltip.Row] {
        let total = stack.totals.indices.contains(index) ? stack.totals[index] : 0
        let rows = stack.layers.map { layer in
            let value = layer.series.values.indices.contains(index) ? layer.series.values[index] : nil
            return MonarchChartTooltip.Row(
                key: layer.series.key,
                label: layer.series.label,
                value: valueText(
                    value,
                    total: total,
                    stacking: stacking,
                    noValue: noValue,
                    locale: locale,
                    formatValue: formatValue,
                    share: share
                ),
                role: layer.series.role,
                isDashed: layer.series.isDashed
            )
        }
        return stacking == .overlap ? rows : rows.reversed()
    }

    /// The popover footer: the total at an index, in a stack of two or more series.
    static func totalText(
        _ stack: Stack,
        at index: Int,
        stacking: MonarchAreaChartStacking,
        totalLabel: String,
        locale: Locale,
        formatValue: ((Double) -> String)?
    ) -> String? {
        guard showsTotal(stacking: stacking, seriesCount: stack.layers.count), stack.totals.indices.contains(index)
        else { return nil }
        let value = MonarchLineChartTokens.valueText(stack.totals[index], locale: locale, formatValue: formatValue)
        return "\(totalLabel) \(value)"
    }

    /// The data table: every series with its real value, hidden ones
    /// included, and in a stack of two or more series a total column.
    static func table(
        _ series: [MonarchLineChartTokens.Series],
        xLabels: [String],
        xTitle: String,
        valueTitle: String?,
        stacking: MonarchAreaChartStacking,
        totalLabel: String,
        locale: Locale,
        formatValue: ((Double) -> String)?
    ) -> MonarchChartTable {
        let base = MonarchLineChartTokens.table(
            series,
            xLabels: xLabels,
            xTitle: xTitle,
            valueTitle: valueTitle,
            locale: locale,
            formatValue: formatValue
        )
        guard showsTotal(stacking: stacking, seriesCount: series.count) else { return base }
        let totals: [Double?] = xLabels.indices.map { index in
            series.reduce(0) { sum, item in
                sum + max(0, item.values.indices.contains(index) ? item.values[index] ?? 0 : 0)
            }
        }
        let column = MonarchChartTableSeries(
            totalLabel,
            values: totals,
            formattedValues: totals.map { value in
                value.map { MonarchLineChartTokens.valueText($0, locale: locale, formatValue: formatValue) } ?? ""
            }
        )
        return MonarchChartTable(
            categoryTitle: base.categoryTitle,
            categories: base.categories,
            series: base.series + [column],
            valueTitle: base.valueTitle
        )
    }

    /// The runs of x values where a layer is drawn, as indices: a gap breaks the band.
    static func runs(_ layer: Layer) -> [[Int]] {
        MonarchLineChartTokens.runs(layer.bands.map { $0?.high })
    }

    /// The legend entries: one per series, or none for a single series,
    /// whose name is the title.
    static func legend(_ series: [MonarchLineChartTokens.Series]) -> [MonarchChartLegendItem] {
        MonarchLineChartTokens.legend(series)
    }

    /// The positions labelled on the x axis, as the web's `labelIndices` chooses them for a phone-wide plot: at
    /// most `fit` labels, evenly spaced, the last position in place of a label that would sit next to it.
    static func labelIndices(count: Int, fit: Int = 5) -> [Int] {
        guard count > 1 else { return count == 1 ? [0] : [] }
        if count <= fit { return Array(0..<count) }
        let every = Int((Double(count - 1) / Double(fit - 1)).rounded(.up))
        var out = Array(stride(from: 0, to: count, by: every))
        let last = count - 1
        if let previous = out.last, previous != last {
            if last - previous < every {
                if out.count > 1 { out[out.count - 1] = last }
            } else {
                out.append(last)
            }
        }
        return out
    }

    /// Where an x label sits against its tick: the first starts there, the last ends there, the rest centre.
    static func labelAnchor(_ index: Int?, count: Int) -> UnitPoint {
        guard let index, count > 1 else { return .top }
        if index == 0 { return .topLeading }
        if index == count - 1 { return .topTrailing }
        return .top
    }
}
