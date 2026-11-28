import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchFunnelChart``.
enum MonarchFunnelChartTokens {
    /// The gap between two stages.
    static let stageSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the step line, the head and the bar of a stage.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the label and the value, and between the value and its share.
    static let headSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the value and its share of the first stage.
    static let shareSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the step arrow and its text.
    static let stepSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// Padding above and below a stage.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space1
    /// Padding before and after a stage.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The height of a bar before Dynamic Type: 20 points.
    static let barHeight: CGFloat = MonarchTokens.Space.space5
    /// The corner radius of a bar.
    static let barRadius: CGFloat = MonarchTokens.Radius.xs
    /// The least share of the widest bar drawn for a value above zero, so it stays visible.
    static let minimumBarShare = 0.005
    /// The least height kept for the chart: 120 points.
    static let minimumHeight: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space6
    /// The height planned per stage with a step line: 76 points.
    static let stageHeightWithStep: CGFloat = MonarchTokens.Space.space16 + MonarchTokens.Space.space3
    /// The height planned per stage without a step line: 56 points.
    static let stageHeight: CGFloat = MonarchTokens.Space.space12 + MonarchTokens.Space.space2
    /// The SF Symbol of the step line.
    static let stepSymbol = "arrowtriangle.down.fill"

    /// The numbers behind the chart.
    struct Model: Equatable {
        /// The values, with negative and non-finite values as zero.
        let values: [Double]
        /// The largest value; the full width of a bar.
        let widest: Double
        /// The share of the previous stage that reached each stage; `nil` for the first.
        let fromPrevious: [Double?]
        /// The share of the first stage that reached each stage.
        let ofFirst: [Double]
        /// The index of the stage before the largest drop, and the share lost, if any stage lost some.
        let largestDrop: Drop?
    }

    /// The largest drop between two stages.
    struct Drop: Equatable {
        /// The index of the stage before the drop.
        let from: Int
        /// The share lost, 0 through 1.
        let lost: Double
    }

    /// Computes the shares and the largest drop. Pure, so it is tested without a view.
    static func model(_ stages: [MonarchFunnelChartStage]) -> Model {
        let values = stages.map { $0.value.isFinite ? max(0, $0.value) : 0 }
        let first = values.first ?? 0
        var fromPrevious: [Double?] = []
        var drop: Drop?
        for index in values.indices {
            guard index > 0 else {
                fromPrevious.append(nil)
                continue
            }
            let previous = values[index - 1]
            let rate = previous > 0 ? values[index] / previous : 0
            fromPrevious.append(rate)
            let lost = 1 - rate
            if previous > 0, lost > 0, lost > (drop?.lost ?? 0) {
                drop = Drop(from: index - 1, lost: lost)
            }
        }
        return Model(
            values: values,
            widest: values.max() ?? 0,
            fromPrevious: fromPrevious,
            ofFirst: values.map { first > 0 ? $0 / first : 0 },
            largestDrop: drop
        )
    }

    /// The span of a bar on a scale from 0 to `widest`: centred, or from the leading edge.
    /// A value above zero spans at least `minimumBarShare` of the scale.
    static func barSpan(value: Double, widest: Double, alignment: MonarchFunnelChartAlignment) -> ClosedRange<Double> {
        guard widest > 0, value > 0 else { return 0...0 }
        let length = min(widest, max(value, widest * minimumBarShare))
        switch alignment {
        case .leading:
            return 0...length
        case .center:
            let start = (widest - length) / 2
            return start...(start + length)
        }
    }

    /// The scale of every bar: 0 to the widest value, or 0 to 1 when all values are zero.
    static func scale(widest: Double) -> ClosedRange<Double> {
        widest > 0 ? 0...widest : 0...1
    }

    /// The status shown: empty when the chart is ready but has no stages.
    static func status(_ status: MonarchChartFrameStatus, stageCount: Int) -> MonarchChartFrameStatus {
        status == .ready && stageCount == 0 ? .empty : status
    }

    /// The height kept for the chart, so the frame does not jump when data arrives.
    static func height(stageCount: Int, showsSteps: Bool) -> CGFloat {
        max(minimumHeight, CGFloat(stageCount) * (showsSteps ? stageHeightWithStep : stageHeight))
    }

    /// The default number format: the locale's number.
    static func valueText(_ value: Double, locale: Locale, format: ((Double) -> String)?) -> String {
        format?(value) ?? value.formatted(.number.locale(locale))
    }

    /// The default rate format: a percentage with at most one decimal.
    static func rateText(_ rate: Double, locale: Locale, format: ((Double) -> String)?) -> String {
        format?(rate) ?? rate.formatted(.percent.precision(.fractionLength(0...1)).locale(locale))
    }

    /// The line drawn above a stage: the share that continued and the number dropped.
    static func stepText(rate: String, dropped: String, labels: MonarchFunnelChartLabels) -> String {
        MonarchFunnelChartStrings.fill(labels.step ?? MonarchFunnelChartStrings.step, rate, dropped)
    }

    /// The generated summary: the stages, the overall conversion and the largest drop.
    static func summary(
        _ stages: [MonarchFunnelChartStage],
        model: Model,
        count: String,
        rate: (Double) -> String,
        labels: MonarchFunnelChartLabels
    ) -> String {
        guard let first = stages.first, let last = stages.last else { return "" }
        var parts: [String] = []
        if stages.count == 1 {
            parts.append(
                MonarchFunnelChartStrings.fill(labels.oneStage ?? MonarchFunnelChartStrings.oneStage, first.label)
            )
            return parts.joined(separator: ". ")
        }
        parts.append(
            MonarchFunnelChartStrings.fill(
                labels.stages ?? MonarchFunnelChartStrings.stages,
                count,
                first.label,
                last.label
            )
        )
        parts.append(
            MonarchFunnelChartStrings.fill(
                labels.overall ?? MonarchFunnelChartStrings.overall,
                rate(model.ofFirst.last ?? 0)
            )
        )
        if let drop = model.largestDrop, stages.indices.contains(drop.from + 1) {
            parts.append(
                MonarchFunnelChartStrings.fill(
                    labels.largestDrop ?? MonarchFunnelChartStrings.largestDrop,
                    stages[drop.from].label,
                    stages[drop.from + 1].label,
                    rate(drop.lost)
                )
            )
        }
        return parts.joined(separator: ". ")
    }

    /// What VoiceOver reads for one stage: its label and value, then its shares.
    static func describe(
        _ stage: MonarchFunnelChartStage,
        value: String,
        fromPrevious: String?,
        ofFirst: String,
        labels: MonarchFunnelChartLabels
    ) -> String {
        guard let fromPrevious else {
            return MonarchFunnelChartStrings.fill(
                labels.describeFirst ?? MonarchFunnelChartStrings.describeFirst,
                stage.label,
                value
            )
        }
        return MonarchFunnelChartStrings.fill(
            labels.describe ?? MonarchFunnelChartStrings.describe,
            stage.label,
            value,
            fromPrevious,
            ofFirst
        )
    }

    /// The data table: one row per stage with its count and both shares.
    static func table(
        _ stages: [MonarchFunnelChartStage],
        model: Model,
        value: (Double) -> String,
        rate: (Double) -> String,
        labels: MonarchFunnelChartLabels
    ) -> MonarchChartTable {
        MonarchChartTable(
            categoryTitle: labels.stage ?? MonarchFunnelChartStrings.stage,
            categories: stages.map(\.label),
            series: [
                MonarchChartTableSeries(
                    labels.value ?? MonarchFunnelChartStrings.value,
                    values: model.values,
                    formattedValues: model.values.map(value)
                ),
                MonarchChartTableSeries(
                    labels.fromPrevious ?? MonarchFunnelChartStrings.fromPrevious,
                    values: model.fromPrevious,
                    formattedValues: model.fromPrevious.map { $0.map(rate) ?? "" }
                ),
                MonarchChartTableSeries(
                    labels.ofFirst ?? MonarchFunnelChartStrings.ofFirst,
                    values: model.ofFirst,
                    formattedValues: model.ofFirst.map(rate)
                ),
            ]
        )
    }

    /// The table given to VoiceOver as an audio graph: the counts only, so
    /// the value axis is one measure.
    static func audioGraphTable(_ table: MonarchChartTable) -> MonarchChartTable {
        MonarchChartTable(
            categoryTitle: table.categoryTitle,
            categories: table.categories,
            series: Array(table.series.prefix(1)),
            valueTitle: table.series.first?.name
        )
    }
}
