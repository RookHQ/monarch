import Foundation

/// Pure logic for ``MonarchStackedBarChart``. Drawing, colours and sizes
/// are ``MonarchBarChart``'s.
enum MonarchStackedBarChartTokens {
    /// The value axis of the 100% offset: no share to every share.
    static let expandedRange: ClosedRange<Double> = 0...1
    /// The most fraction digits of a share, as on the web: "33.3%".
    static let shareFractionDigits = 1

    /// Whether the chart draws shares: only stacked bars with the 100% offset.
    static func isExpanded(layout: MonarchBarChartLayout, offset: MonarchStackedBarChartOffset) -> Bool {
        layout == .stacked && offset == .expand
    }

    /// The series as shares of each category's total. The total counts the
    /// positive values of every series, hidden ones included, so hiding a
    /// series leaves a gap rather than rescaling the others. Negative values
    /// count as zero; gaps stay gaps; a category without a positive total
    /// has zero shares.
    static func shares(categories: [String], series: [MonarchBarChartSeries]) -> [MonarchBarChartSeries] {
        let totals = categories.indices.map { index in
            series.reduce(0.0) { $0 + max(0, MonarchBarChartTokens.value($1, at: index) ?? 0) }
        }
        return series.map { entry in
            let values: [Double?] = categories.indices.map { index in
                guard let value = MonarchBarChartTokens.value(entry, at: index) else { return nil }
                return totals[index] > 0 ? max(0, value) / totals[index] : 0
            }
            return MonarchBarChartSeries(entry.key, label: entry.label, values: values, color: entry.color)
        }
    }

    /// A share as a percentage in the locale: "25%", "33.3%".
    static func percentText(_ share: Double, locale: Locale) -> String {
        share.formatted(.percent.precision(.fractionLength(0...shareFractionDigits)).locale(locale))
    }

    /// The spoken summary of the 100% offset: the number and range of
    /// categories, then each shown series' average share and its largest
    /// share. `shares` are the values from ``shares(categories:series:)``.
    static func shareSummary(
        categories: [String],
        shares: [MonarchBarChartSeries],
        hidden: Set<String>,
        format: (Double) -> String,
        seriesFormat: String,
        noData: String
    ) -> String {
        guard let first = categories.first, let last = categories.last else { return noData }
        var sentences = [
            categories.count == 1
                ? MonarchBarChartStrings.onePoint(first)
                : MonarchBarChartStrings.points(count: categories.count.formatted(), first: first, last: last)
        ]
        for series in shares where !hidden.contains(series.key) {
            let values = categories.indices.compactMap { index in
                MonarchBarChartTokens.value(series, at: index).map { (index, $0) }
            }
            guard let largest = values.max(by: { $0.1 < $1.1 }) else {
                sentences.append("\(series.label): \(noData)")
                continue
            }
            let average = values.reduce(0) { $0 + $1.1 } / Double(values.count)
            sentences.append(
                MonarchStackedBarChartStrings.fill(
                    seriesFormat,
                    series.label,
                    format(average),
                    categories[largest.0],
                    format(largest.1)
                )
            )
        }
        return sentences.joined(separator: ". ")
    }
}
