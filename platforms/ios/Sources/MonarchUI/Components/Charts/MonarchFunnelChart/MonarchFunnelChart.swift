import Charts
import SwiftUI

/// A conversion funnel: how many reach each step, the share that continues
/// from one step to the next and how many drop off.
///
/// ```swift
/// MonarchFunnelChart(
///     "Sign-up funnel",
///     stages: [
///         MonarchFunnelChartStage("Visited pricing", value: 4_800),
///         MonarchFunnelChartStage("Started sign-up", value: 1_900),
///         MonarchFunnelChartStage("Paid", value: 310),
///     ]
/// )
/// ```
///
/// The chart sits in a ``MonarchChartFrame``: a title, loading, empty and
/// error states, and "View as table" with every count and share. Each bar is
/// a Swift Charts `BarMark` on one scale, the widest stage filling the width.
/// Numbers are written in the environment's locale unless `formatValue` or
/// `formatRate` is given.
///
/// Accessibility: the stages are a container named by the generated summary
/// (the stages, the overall conversion and the largest drop) with an
/// accessibility chart descriptor for Audio Graphs. Each stage is one element
/// read as its label, value and shares; with `onSelect` it is a button with a
/// 44-point target. The bars and step arrows are hidden.
public struct MonarchFunnelChart: View {
    private let title: String
    private let stages: [MonarchFunnelChartStage]
    private let description: String?
    private let status: MonarchChartFrameStatus
    private let alignment: MonarchFunnelChartAlignment
    private let showsSteps: Bool
    private let summary: String?
    private let emptyMessage: String?
    private let footer: String?
    private let labels: MonarchFunnelChartLabels
    private let formatValue: ((Double) -> String)?
    private let formatRate: ((Double) -> String)?
    private let onRetry: (@MainActor () -> Void)?
    private let onSelect: (@MainActor (MonarchFunnelChartStage) -> Void)?

    @Environment(\.locale) private var locale

    /// A funnel chart.
    ///
    /// - Parameters:
    ///   - title: The name of the chart, already localised.
    ///   - stages: The steps, first (widest) to last.
    ///   - description: One line under the title, such as the period.
    ///   - status: Whether the chart, a placeholder, an empty state or an error shows.
    ///     A ready chart without stages shows the empty state.
    ///   - alignment: Centred bars, or bars from the leading edge.
    ///   - showsSteps: Whether the line between stages shows the share that continued and the number dropped.
    ///   - summary: Replaces the generated summary that VoiceOver reads for the chart.
    ///   - emptyMessage: The empty-state message.
    ///   - footer: Notes under the chart, such as the source.
    ///   - labels: Replacements for the built-in strings.
    ///   - formatValue: Writes a count. The default is a number in the environment's locale.
    ///   - formatRate: Writes a share from 0 to 1. The default is a percentage with at most one decimal.
    ///   - onRetry: Shows Try again in the error status and runs when it is pressed.
    ///   - onSelect: Makes every stage a button, for example to list the people who dropped.
    public init(
        _ title: String,
        stages: [MonarchFunnelChartStage],
        description: String? = nil,
        status: MonarchChartFrameStatus = .ready,
        alignment: MonarchFunnelChartAlignment = .center,
        showsSteps: Bool = true,
        summary: String? = nil,
        emptyMessage: String? = nil,
        footer: String? = nil,
        labels: MonarchFunnelChartLabels = MonarchFunnelChartLabels(),
        formatValue: ((Double) -> String)? = nil,
        formatRate: ((Double) -> String)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onSelect: (@MainActor (MonarchFunnelChartStage) -> Void)? = nil
    ) {
        self.title = title
        self.stages = stages
        self.description = description
        self.status = status
        self.alignment = alignment
        self.showsSteps = showsSteps
        self.summary = summary
        self.emptyMessage = emptyMessage
        self.footer = footer
        self.labels = labels
        self.formatValue = formatValue
        self.formatRate = formatRate
        self.onRetry = onRetry
        self.onSelect = onSelect
    }

    /// The content of the view.
    public var body: some View {
        let resolved = MonarchFunnelChartTokens.status(status, stageCount: stages.count)
        let model = MonarchFunnelChartTokens.model(stages)
        let table = MonarchFunnelChartTokens.table(
            stages,
            model: model,
            value: value,
            rate: rate,
            labels: labels
        )
        MonarchChartFrame(
            title,
            description: description,
            status: resolved,
            table: table,
            emptyMessage: emptyMessage,
            footer: footer,
            height: MonarchFunnelChartTokens.height(stageCount: stages.count, showsSteps: showsSteps),
            contentAccessibility: .contain,
            labels: labels.frame,
            onRetry: onRetry
        ) {
            plot(model: model, table: table)
        }
    }

    private func value(_ number: Double) -> String {
        MonarchFunnelChartTokens.valueText(number, locale: locale, format: formatValue)
    }

    private func rate(_ number: Double) -> String {
        MonarchFunnelChartTokens.rateText(number, locale: locale, format: formatRate)
    }

    private func plot(model: MonarchFunnelChartTokens.Model, table: MonarchChartTable) -> some View {
        let spoken =
            summary
            ?? MonarchFunnelChartTokens.summary(
                stages,
                model: model,
                count: stages.count.formatted(.number.locale(locale)),
                rate: rate,
                labels: labels
            )
        let scale = MonarchFunnelChartTokens.scale(widest: model.widest)
        return VStack(alignment: .leading, spacing: MonarchFunnelChartTokens.stageSpacing) {
            ForEach(Array(stages.enumerated()), id: \.element.id) { index, stage in
                let row = MonarchFunnelChartRow(
                    stage: stage,
                    value: value(model.values[index]),
                    share: index > 0 ? rate(model.ofFirst[index]) : nil,
                    step: stepText(model: model, index: index),
                    span: MonarchFunnelChartTokens.barSpan(
                        value: model.values[index],
                        widest: model.widest,
                        alignment: alignment
                    ),
                    scale: scale
                )
                let label = MonarchFunnelChartTokens.describe(
                    stage,
                    value: value(model.values[index]),
                    fromPrevious: model.fromPrevious[index].map(rate),
                    ofFirst: rate(model.ofFirst[index]),
                    labels: labels
                )
                if let onSelect {
                    Button {
                        onSelect(stage)
                    } label: {
                        row
                    }
                    .buttonStyle(MonarchFunnelChartPressStyle())
                    .accessibilityLabel(label)
                } else {
                    row
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel(label)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(spoken)
        .accessibilityChartDescriptor(
            MonarchChartFrameDescriptor(
                title: title,
                summary: spoken,
                table: MonarchFunnelChartTokens.audioGraphTable(table)
            )
        )
    }

    private func stepText(model: MonarchFunnelChartTokens.Model, index: Int) -> String? {
        guard showsSteps, index > 0, let fromPrevious = model.fromPrevious[index] else { return nil }
        let dropped = max(0, model.values[index - 1] - model.values[index])
        return MonarchFunnelChartTokens.stepText(rate: rate(fromPrevious), dropped: value(dropped), labels: labels)
    }
}

#if DEBUG
    #Preview("MonarchFunnelChart") {
        let stages = [
            MonarchFunnelChartStage("Visited pricing", value: 4_800),
            MonarchFunnelChartStage("Started sign-up", value: 1_900),
            MonarchFunnelChartStage("Verified email", value: 1_200),
            MonarchFunnelChartStage("Paid", value: 310),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchFunnelChart("Sign-up funnel", stages: stages, description: "Last 30 days")
                MonarchFunnelChart("Hiring", stages: stages, alignment: .leading, showsSteps: false)
                MonarchFunnelChart("Empty", stages: [])
            }
        }
    }
#endif
