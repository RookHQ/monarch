import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiChainOfSteps``.
enum MonarchAiChainOfStepsTokens {
    /// The gap between steps.
    static let stepSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the icon and the text of a step.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the label, the state and the detail of a step.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the summary icon and the summary text.
    static let summarySpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The size of the icon column, before Dynamic Type.
    static let iconSize: CGFloat = MonarchTokens.Space.space4
    /// The SF Symbol of the folded summary when a step failed.
    static let failedSummarySymbol = "xmark.octagon"

    /// Whether a state is final: done, failed or skipped.
    static func isFinished(_ state: MonarchAiPlanStepState) -> Bool {
        state == .done || state == .failed || state == .skipped
    }

    /// Whether every step has finished. An empty list has not.
    static func allFinished(_ steps: [MonarchAiPlanStep]) -> Bool {
        !steps.isEmpty && steps.allSatisfy { isFinished($0.state) }
    }

    /// The number of failed steps.
    static func failedCount(_ steps: [MonarchAiPlanStep]) -> Int {
        steps.filter { $0.state == .failed }.count
    }

    /// Whether the list is folded under a summary: when it may fold and every step has finished.
    static func isFolded(_ steps: [MonarchAiPlanStep], isCollapsible: Bool) -> Bool {
        isCollapsible && allFinished(steps)
    }

    /// The SF Symbol of a state. The active step shows a progress indicator
    /// instead, and this still symbol when Reduce Motion is on.
    static func symbol(_ state: MonarchAiPlanStepState) -> String {
        switch state {
        case .todo: "circle.dotted"
        case .active: "circle.inset.filled"
        case .done: "checkmark.circle.fill"
        case .failed: "xmark.octagon.fill"
        case .skipped: "minus.circle"
        }
    }

    /// The colour role of the icon of a state.
    static func iconRole(_ state: MonarchAiPlanStepState) -> MonarchColorRole {
        switch state {
        case .todo, .skipped: \.textSubtle
        case .active: \.aiStrong
        case .done: \.successStrong
        case .failed: \.dangerStrong
        }
    }

    /// The colour role of the label of a state.
    static func labelRole(_ state: MonarchAiPlanStepState) -> MonarchColorRole {
        switch state {
        case .todo, .skipped: \.textMuted
        case .active, .done, .failed: \.textDefault
        }
    }

    /// Whether the state is written beside the label: only failed and skipped,
    /// which the icon alone would not make clear.
    static func showsStateText(_ state: MonarchAiPlanStepState) -> Bool {
        state == .failed || state == .skipped
    }

    /// The colour role of the written state.
    static func stateRole(_ state: MonarchAiPlanStepState) -> MonarchColorRole {
        state == .failed ? \.dangerStrong : \.textMuted
    }

    /// The name of a state.
    static func stateName(_ state: MonarchAiPlanStepState, labels: MonarchAiChainOfStepsLabels) -> String {
        labels.state?(state) ?? MonarchAiChainOfStepsStrings.state(state)
    }

    /// The summary of the folded list.
    static func summary(_ steps: [MonarchAiPlanStep], labels: MonarchAiChainOfStepsLabels) -> String {
        let failed = failedCount(steps)
        return labels.summary?(steps.count, failed)
            ?? MonarchAiChainOfStepsStrings.summary(count: steps.count, failed: failed)
    }

    /// What VoiceOver reads after the label of a step: its state, then its detail.
    static func accessibilityValue(stateName: String, detail: String?) -> String {
        guard let detail, !detail.isEmpty else { return stateName }
        return "\(stateName), \(detail)"
    }

    /// The announcements for a change from `previous` (state by step id) to
    /// `steps`. Nothing is announced for the first state, which is history,
    /// not news: a step that starts or fails, then completion once.
    static func announcements(
        previous: [String: MonarchAiPlanStepState]?,
        steps: [MonarchAiPlanStep],
        labels: MonarchAiChainOfStepsLabels
    ) -> [String] {
        guard let previous else { return [] }
        var lines: [String] = []
        let total = steps.count
        for (index, step) in steps.enumerated() where previous[step.id] != step.state {
            switch step.state {
            case .active:
                lines.append(
                    labels.started?(step.label, index + 1, total)
                        ?? MonarchAiChainOfStepsStrings.started(step.label, position: index + 1, total: total)
                )
            case .failed:
                lines.append(labels.failedStep?(step.label) ?? MonarchAiChainOfStepsStrings.failedStep(step.label))
            case .todo, .done, .skipped:
                break
            }
        }
        let wasFinished = previous.count == total && !previous.isEmpty && previous.values.allSatisfy(isFinished)
        if allFinished(steps) && !wasFinished {
            let failed = failedCount(steps)
            lines.append(
                labels.finished?(total, failed) ?? MonarchAiChainOfStepsStrings.finished(count: total, failed: failed)
            )
        }
        return lines
    }

    /// The state of every step by id, to compare with the next update.
    static func snapshot(_ steps: [MonarchAiPlanStep]) -> [String: MonarchAiPlanStepState] {
        Dictionary(steps.map { ($0.id, $0.state) }, uniquingKeysWith: { _, last in last })
    }
}
