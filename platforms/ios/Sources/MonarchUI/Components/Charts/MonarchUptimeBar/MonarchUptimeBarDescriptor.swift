import Accessibility
import Foundation
import SwiftUI

/// Gives VoiceOver the periods of an uptime bar as an audio graph and a
/// data table: the uptime of each period, with its state as a second value.
struct MonarchUptimeBarDescriptor: AXChartDescriptorRepresentable {
    let title: String
    let summary: String
    let periods: [String]
    let uptimes: [Double?]
    let statusNames: [String]
    let allStatusNames: [String]
    let texts: [String]
    let digits: Int
    let labels: MonarchUptimeBarLabels

    func makeChartDescriptor() -> AXChartDescriptor {
        let uptimeName = labels.uptime ?? MonarchUptimeBarStrings.uptime
        let digits = digits
        let xAxis = AXCategoricalDataAxisDescriptor(
            title: labels.period ?? MonarchUptimeBarStrings.period,
            categoryOrder: periods
        )
        let yAxis = AXNumericDataAxisDescriptor(
            title: uptimeName,
            range: MonarchUptimeBarDescriptor.range,
            gridlinePositions: []
        ) { value in
            MonarchUptimeBarTokens.percent(value, digits: digits, locale: .current)
        }
        let statusAxis = AXCategoricalDataAxisDescriptor(
            title: labels.status ?? MonarchUptimeBarStrings.status,
            categoryOrder: allStatusNames
        )
        return AXChartDescriptor(
            title: title,
            summary: summary,
            xAxis: xAxis,
            yAxis: yAxis,
            additionalAxes: [statusAxis],
            series: [AXDataSeriesDescriptor(name: uptimeName, isContinuous: false, dataPoints: points())]
        )
    }

    /// The range of the uptime axis, in percent.
    static let range: ClosedRange<Double> = 0...100

    /// One point per period, with its state and its details as the label.
    func points() -> [AXDataPoint] {
        periods.indices.map { index in
            AXDataPoint(
                x: periods[index],
                y: uptimes.indices.contains(index) ? uptimes[index] : nil,
                additionalValues: statusNames.indices.contains(index) ? [.category(statusNames[index])] : [],
                label: texts.indices.contains(index) ? texts[index] : nil
            )
        }
    }
}
