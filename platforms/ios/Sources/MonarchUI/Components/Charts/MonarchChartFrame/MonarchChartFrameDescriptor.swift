import Accessibility
import Foundation
import SwiftUI

/// Gives VoiceOver a chart's data table as an audio graph and a data table,
/// through the accessibility chart descriptor.
struct MonarchChartFrameDescriptor: AXChartDescriptorRepresentable {
    let title: String
    let summary: String?
    let table: MonarchChartTable

    func makeChartDescriptor() -> AXChartDescriptor {
        let lookup = Self.formattedLookup(table)
        let xAxis = AXCategoricalDataAxisDescriptor(title: table.categoryTitle, categoryOrder: table.categories)
        let yAxis = AXNumericDataAxisDescriptor(
            title: table.valueTitle ?? "",
            range: MonarchChartFrameTokens.valueRange(table),
            gridlinePositions: []
        ) { value in
            lookup[value] ?? value.formatted()
        }
        let series = table.series.filter { !MonarchChartFrameTokens.isTextOnly($0) }.map { series in
            AXDataSeriesDescriptor(
                name: series.name,
                isContinuous: false,
                dataPoints: Self.points(series, categories: table.categories)
            )
        }
        return AXChartDescriptor(
            title: title,
            summary: summary,
            xAxis: xAxis,
            yAxis: yAxis,
            additionalAxes: [],
            series: series
        )
    }

    /// The points of a series: one per category with a value; gaps are left out.
    static func points(_ series: MonarchChartTableSeries, categories: [String]) -> [AXDataPoint] {
        categories.indices.compactMap { index in
            guard series.values.indices.contains(index), let value = series.values[index], value.isFinite else {
                return nil
            }
            return AXDataPoint(
                x: categories[index],
                y: value,
                additionalValues: [],
                label: MonarchChartFrameTokens.cellText(series, at: index)
            )
        }
    }

    /// The app's formatted text for each value, so VoiceOver reads "$1,200"
    /// rather than "1200" where the app supplied it.
    static func formattedLookup(_ table: MonarchChartTable) -> [Double: String] {
        var lookup: [Double: String] = [:]
        for series in table.series {
            guard let formatted = series.formattedValues else { continue }
            for (value, text) in zip(series.values, formatted) {
                if let value, lookup[value] == nil {
                    lookup[value] = text
                }
            }
        }
        return lookup
    }
}
