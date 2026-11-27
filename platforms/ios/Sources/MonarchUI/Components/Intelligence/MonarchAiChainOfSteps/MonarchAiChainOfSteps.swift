import SwiftUI

/// The steps an assistant is working through, as an ordered list that
/// updates live and folds under a summary such as "4 steps" when every step
/// has finished.
///
/// ```swift
/// MonarchAiChainOfSteps(plan.steps, details: ["search": "12 results"])
/// ```
///
/// It shows the steps of a ``MonarchAiPlanPart``: update each step's
/// `state` as the assistant works. Each step shows its state with an icon;
/// the running step shows a progress indicator, and failed and skipped steps
/// also say so in words. Once every step is done, failed or skipped, the list
/// folds into a standard `DisclosureGroup` the person can open.
///
/// Accessibility: the list is a group named "Steps". Each step is one
/// element read as its label, with its state and detail as the value, so the
/// state never depends on the icon or colour. A step that starts or fails,
/// and the end of the run, are announced; the first state is not. The
/// summary is a disclosure button with an expanded or collapsed value. With
/// Reduce Motion the running step shows a still icon instead of a spinner.
public struct MonarchAiChainOfSteps: View {
    private let steps: [MonarchAiPlanStep]
    private let details: [String: String]
    private let isCollapsible: Bool
    private let announces: Bool
    private let labels: MonarchAiChainOfStepsLabels

    @Environment(\.monarchTheme) private var theme
    @State private var isExpanded: Bool
    @State private var previous: [String: MonarchAiPlanStepState]?

    /// A chain of steps.
    ///
    /// - Parameters:
    ///   - steps: The steps, in order, from a plan part.
    ///   - details: Optional detail under a step, such as a result or an error, by step id. Already localised.
    ///   - isCollapsible: Whether the list folds under a summary once every step has finished.
    ///   - startsExpanded: Whether the folded list starts open.
    ///   - announces: Whether starts, failures and completion are announced. Turn it off when
    ///     the surrounding thread announces progress itself.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ steps: [MonarchAiPlanStep],
        details: [String: String] = [:],
        isCollapsible: Bool = true,
        startsExpanded: Bool = false,
        announces: Bool = true,
        labels: MonarchAiChainOfStepsLabels = MonarchAiChainOfStepsLabels()
    ) {
        self.steps = steps
        self.details = details
        self.isCollapsible = isCollapsible
        self.announces = announces
        self.labels = labels
        _isExpanded = State(initialValue: startsExpanded)
    }

    /// The content of the view.
    public var body: some View {
        Group {
            if MonarchAiChainOfStepsTokens.isFolded(steps, isCollapsible: isCollapsible) {
                DisclosureGroup(isExpanded: $isExpanded) {
                    list
                } label: {
                    summary
                }
                .disclosureGroupStyle(.monarch)
            } else {
                list
            }
        }
        .onAppear { previous = MonarchAiChainOfStepsTokens.snapshot(steps) }
        .onChange(of: steps) { _, newSteps in
            announce(newSteps)
        }
    }

    private var summary: some View {
        HStack(spacing: MonarchAiChainOfStepsTokens.summarySpacing) {
            if MonarchAiChainOfStepsTokens.failedCount(steps) > 0 {
                MonarchIcon(systemName: MonarchAiChainOfStepsTokens.failedSummarySymbol, size: .sm)
                    .foregroundStyle(theme.colors.dangerStrong)
            }
            Text(MonarchAiChainOfStepsTokens.summary(steps, labels: labels))
        }
    }

    private var list: some View {
        VStack(alignment: .leading, spacing: MonarchAiChainOfStepsTokens.stepSpacing) {
            ForEach(steps) { step in
                MonarchAiChainStepRow(
                    step: step,
                    detail: details[step.id],
                    stateName: MonarchAiChainOfStepsTokens.stateName(step.state, labels: labels)
                )
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.label ?? MonarchAiChainOfStepsStrings.label)
    }

    private func announce(_ newSteps: [MonarchAiPlanStep]) {
        let lines = MonarchAiChainOfStepsTokens.announcements(previous: previous, steps: newSteps, labels: labels)
        previous = MonarchAiChainOfStepsTokens.snapshot(newSteps)
        guard announces, !lines.isEmpty else { return }
        AccessibilityNotification.Announcement(lines.joined(separator: ". ")).post()
    }
}

/// One step: the state icon, the label, the written state and the detail.
private struct MonarchAiChainStepRow: View {
    let step: MonarchAiPlanStep
    let detail: String?
    let stateName: String

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric(relativeTo: .body) private var iconSize = MonarchAiChainOfStepsTokens.iconSize

    var body: some View {
        let state = step.state
        let size = iconSize
        HStack(alignment: .firstTextBaseline, spacing: MonarchAiChainOfStepsTokens.iconSpacing) {
            icon
                .foregroundStyle(theme.colors[role: MonarchAiChainOfStepsTokens.iconRole(state)])
                .frame(width: size, height: size)
                .alignmentGuide(.firstTextBaseline) { $0[VerticalAlignment.center] + size / 4 }
            VStack(alignment: .leading, spacing: MonarchAiChainOfStepsTokens.textSpacing) {
                Text(step.label)
                    .monarchTextStyle(.body)
                    .strikethrough(state == .skipped)
                    .foregroundStyle(theme.colors[role: MonarchAiChainOfStepsTokens.labelRole(state)])
                if MonarchAiChainOfStepsTokens.showsStateText(state) {
                    Text(stateName)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: MonarchAiChainOfStepsTokens.stateRole(state)])
                }
                if let detail, !detail.isEmpty {
                    Text(detail)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(step.label)
        .accessibilityValue(MonarchAiChainOfStepsTokens.accessibilityValue(stateName: stateName, detail: detail))
    }

    @ViewBuilder private var icon: some View {
        if step.state == .active && !reduceMotion {
            ProgressView()
                .controlSize(.small)
                .tint(theme.colors.aiStrong)
        } else {
            MonarchIcon(systemName: MonarchAiChainOfStepsTokens.symbol(step.state), size: .sm)
        }
    }
}

#if DEBUG
    #Preview("MonarchAiChainOfSteps") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiChainOfSteps(
                    [
                        MonarchAiPlanStep(id: "read", label: "Read the attached contract", state: .done),
                        MonarchAiPlanStep(id: "search", label: "Search past invoices", state: .active),
                        MonarchAiPlanStep(id: "draft", label: "Draft the summary"),
                    ],
                    details: ["read": "14 pages"]
                )
                MonarchAiChainOfSteps(
                    [
                        MonarchAiPlanStep(id: "a", label: "Look up the customer", state: .done),
                        MonarchAiPlanStep(id: "b", label: "Check the payment", state: .failed),
                    ],
                    startsExpanded: true
                )
            }
        }
    }
#endif
