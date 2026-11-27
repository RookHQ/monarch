import SwiftUI

/// A first-run checklist: steps with a done state and an optional call to
/// action, a progress summary, and collapse and dismiss controls.
///
/// ```swift
/// MonarchOnboardingChecklist("Get started", steps: steps, onDismiss: { hideChecklist() }) { step in
///     if step.id == "invite" {
///         Button("Invite") { showInvite() }
///             .buttonStyle(.monarch(.primary, size: .sm))
///     }
/// }
/// ```
///
/// The checklist is a ``MonarchCard``. The progress row is the label of a
/// standard `DisclosureGroup` in the Monarch style, so it shows and hides
/// the steps. The app defines the steps, decides when each is done, and
/// keeps the expanded and dismissed states if they should last.
///
/// Accessibility: the title is a header. The progress row is one button
/// read as "Setup progress, 2 of 5 done" with the value "Expanded" or
/// "Collapsed"; without collapsing it is one element with the progress text
/// as its value. Each step is one element read as its title, with the value
/// "Done" or "Not done" (and "Current step" for the first open step) and the
/// description as its hint; its action stays a separate element. Done is
/// shown by a check, not by colour alone. The dismiss button is named
/// "Dismiss checklist".
public struct MonarchOnboardingChecklist<Action: View>: View {
    private let title: String
    private let steps: [MonarchOnboardingStep]
    private let description: String?
    private let completeMessage: String?
    private let isCollapsible: Bool
    private let externalIsExpanded: Binding<Bool>?
    private let labels: MonarchOnboardingChecklistLabels
    private let onDismiss: (@MainActor () -> Void)?
    private let action: (MonarchOnboardingStep) -> Action

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var internalIsExpanded = true
    @ScaledMetric(relativeTo: .subheadline) private var markerSize = MonarchOnboardingChecklistTokens.markerSize

    /// An onboarding checklist with a call to action for steps that are not done.
    ///
    /// - Parameters:
    ///   - title: The heading, such as "Get started". Already localised.
    ///   - steps: The steps, in order.
    ///   - description: Text under the heading.
    ///   - completeMessage: Replaces the built-in message shown when every
    ///     step is done, "All done. You are set up."
    ///   - isCollapsible: Whether the chevron beside the heading shows and hides the steps.
    ///   - isExpanded: Whether the steps are shown. Without it the component
    ///     keeps the state itself, starting expanded.
    ///   - labels: Replacements for the built-in strings.
    ///   - onDismiss: Shows a dismiss button that runs this. The app hides
    ///     the checklist and remembers it.
    ///   - action: The call to action of a step, such as a small button.
    ///     Shown only while the step is not done.
    public init(
        _ title: String,
        steps: [MonarchOnboardingStep],
        description: String? = nil,
        completeMessage: String? = nil,
        isCollapsible: Bool = true,
        isExpanded: Binding<Bool>? = nil,
        labels: MonarchOnboardingChecklistLabels = MonarchOnboardingChecklistLabels(),
        onDismiss: (@MainActor () -> Void)? = nil,
        @ViewBuilder action: @escaping (MonarchOnboardingStep) -> Action
    ) {
        self.title = title
        self.steps = steps
        self.description = description
        self.completeMessage = completeMessage
        self.isCollapsible = isCollapsible
        self.externalIsExpanded = isExpanded
        self.labels = labels
        self.onDismiss = onDismiss
        self.action = action
    }

    private var isExpanded: Binding<Bool> {
        externalIsExpanded ?? $internalIsExpanded
    }

    private var progressText: String {
        labels.progressText(done: MonarchOnboardingChecklistTokens.doneCount(steps), total: steps.count)
    }

    /// The content of the view.
    public var body: some View {
        MonarchCard {
            VStack(alignment: .leading, spacing: MonarchOnboardingChecklistTokens.sectionSpacing) {
                header
                progressRow
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(labels.resolvedProgressLabel)
                    .accessibilityValue(progressText)
                if !isCollapsible || isExpanded.wrappedValue {
                    stepList
                }
            }
        }
    }

    private var toggleButton: some View {
        Button {
            withAnimation(theme.motion.animation(.base, reduceMotion: reduceMotion)) {
                isExpanded.wrappedValue.toggle()
            }
        } label: {
            MonarchIcon(systemName: isExpanded.wrappedValue ? "chevron.down" : "chevron.right", size: .sm)
        }
        .buttonStyle(.monarch(.ghost, size: .sm))
        .accessibilityLabel(title)
        .accessibilityValue(
            MonarchDisclosureGroupTokens.accessibilityValue(
                isExpanded: isExpanded.wrappedValue,
                expandedLabel: labels.expanded,
                collapsedLabel: labels.collapsed
            )
        )
    }

    private var header: some View {
        HStack(alignment: .top, spacing: MonarchOnboardingChecklistTokens.sectionSpacing) {
            VStack(alignment: .leading, spacing: MonarchOnboardingChecklistTokens.headingSpacing) {
                Text(title)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                if let description, !description.isEmpty {
                    Text(description)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            HStack(spacing: MonarchOnboardingChecklistTokens.headingSpacing) {
                if isCollapsible { toggleButton }
                if let onDismiss {
                    Button {
                        onDismiss()
                    } label: {
                        MonarchIcon(systemName: MonarchOnboardingChecklistTokens.dismissSymbol, size: .sm)
                    }
                    .buttonStyle(.monarch(.ghost, size: .sm))
                    .accessibilityLabel(labels.resolvedDismiss)
                }
            }
        }
    }

    private var progressRow: some View {
        HStack(spacing: MonarchOnboardingChecklistTokens.progressSpacing) {
            ProgressView(
                value: Double(MonarchOnboardingChecklistTokens.doneCount(steps)),
                total: MonarchOnboardingChecklistTokens.progressTotal(steps)
            )
            .progressViewStyle(.monarchLinear)
            .frame(maxWidth: .infinity)
            Text(progressText)
                .monarchTextStyle(.caption)
                .monospacedDigit()
                .foregroundStyle(theme.colors.textMuted)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var stepList: some View {
        let current = MonarchOnboardingChecklistTokens.currentIndex(steps)
        return VStack(alignment: .leading, spacing: MonarchOnboardingChecklistTokens.stepSpacing) {
            if MonarchOnboardingChecklistTokens.isComplete(steps) {
                completeBanner
            }
            ForEach(Array(steps.enumerated()), id: \.element.id) { index, step in
                stepRow(step, index: index, isCurrent: index == current)
            }
        }
    }

    private var completeBanner: some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchOnboardingChecklistTokens.completeIconSpacing) {
            MonarchIcon(systemName: MonarchOnboardingChecklistTokens.doneSymbol, size: .sm)
            Text(completeMessage ?? labels.resolvedComplete)
                .monarchTextStyle(.label)
        }
        .foregroundStyle(theme.colors[role: MonarchOnboardingChecklistTokens.completeForeground])
        .padding(.vertical, MonarchOnboardingChecklistTokens.completeVerticalPadding)
        .padding(.horizontal, MonarchOnboardingChecklistTokens.completeHorizontalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: MonarchOnboardingChecklistTokens.cornerRadius, style: .continuous)
                .fill(theme.colors[role: MonarchOnboardingChecklistTokens.completeBackground])
        )
        .accessibilityElement(children: .combine)
    }

    private func stepRow(_ step: MonarchOnboardingStep, index: Int, isCurrent: Bool) -> some View {
        HStack(alignment: .center, spacing: MonarchOnboardingChecklistTokens.markerSpacing) {
            HStack(alignment: .top, spacing: MonarchOnboardingChecklistTokens.markerSpacing) {
                marker(step, index: index, isCurrent: isCurrent)
                VStack(alignment: .leading, spacing: MonarchOnboardingChecklistTokens.stepTextSpacing) {
                    Text(step.title)
                        .monarchTextStyle(.label)
                        .foregroundStyle(
                            theme.colors[role: MonarchOnboardingChecklistTokens.titleForeground(isDone: step.isDone)]
                        )
                    if !step.isDone, let description = step.description, !description.isEmpty {
                        Text(description)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(step.title)
            .accessibilityValue(
                MonarchOnboardingChecklistTokens.stepAccessibilityValue(step, isCurrent: isCurrent, labels: labels)
            )
            .accessibilityHint(step.isDone ? "" : (step.description ?? ""))
            if !step.isDone {
                action(step)
                    .fixedSize(horizontal: true, vertical: false)
            }
        }
        .padding(.vertical, MonarchOnboardingChecklistTokens.stepVerticalPadding)
        .padding(.horizontal, MonarchOnboardingChecklistTokens.stepHorizontalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(stepBackground(isCurrent: isCurrent))
    }

    private func marker(_ step: MonarchOnboardingStep, index: Int, isCurrent: Bool) -> some View {
        let foreground = MonarchOnboardingChecklistTokens.markerForeground(isDone: step.isDone, isCurrent: isCurrent)
        let border = MonarchOnboardingChecklistTokens.markerBorder(isDone: step.isDone, isCurrent: isCurrent)
        return ZStack {
            if let fill = MonarchOnboardingChecklistTokens.markerFill(isDone: step.isDone) {
                Circle().fill(theme.colors[role: fill])
            }
            Circle().strokeBorder(
                theme.colors[role: border],
                lineWidth: MonarchOnboardingChecklistTokens.markerBorderWidth
            )
            if step.isDone {
                MonarchIcon(systemName: MonarchOnboardingChecklistTokens.doneSymbol, size: .sm)
            } else {
                Text(MonarchOnboardingChecklistTokens.markerNumber(index))
                    .monarchTextStyle(.caption, weight: .semibold)
                    .monospacedDigit()
                    .fixedSize()
            }
        }
        .foregroundStyle(theme.colors[role: foreground])
        .frame(minWidth: markerSize, minHeight: markerSize)
        .fixedSize()
    }

    @ViewBuilder private func stepBackground(isCurrent: Bool) -> some View {
        if let role = MonarchOnboardingChecklistTokens.stepBackground(isCurrent: isCurrent) {
            RoundedRectangle(cornerRadius: MonarchOnboardingChecklistTokens.cornerRadius, style: .continuous)
                .fill(theme.colors[role: role])
        }
    }
}

extension MonarchOnboardingChecklist where Action == EmptyView {
    /// An onboarding checklist without calls to action.
    ///
    /// - Parameters:
    ///   - title: The heading, such as "Get started". Already localised.
    ///   - steps: The steps, in order.
    ///   - description: Text under the heading.
    ///   - completeMessage: Replaces the built-in message shown when every step is done.
    ///   - isCollapsible: Whether the chevron beside the heading shows and hides the steps.
    ///   - isExpanded: Whether the steps are shown. Without it the component
    ///     keeps the state itself, starting expanded.
    ///   - labels: Replacements for the built-in strings.
    ///   - onDismiss: Shows a dismiss button that runs this.
    public init(
        _ title: String,
        steps: [MonarchOnboardingStep],
        description: String? = nil,
        completeMessage: String? = nil,
        isCollapsible: Bool = true,
        isExpanded: Binding<Bool>? = nil,
        labels: MonarchOnboardingChecklistLabels = MonarchOnboardingChecklistLabels(),
        onDismiss: (@MainActor () -> Void)? = nil
    ) {
        self.init(
            title,
            steps: steps,
            description: description,
            completeMessage: completeMessage,
            isCollapsible: isCollapsible,
            isExpanded: isExpanded,
            labels: labels,
            onDismiss: onDismiss,
            action: { _ in EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchOnboardingChecklist") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchOnboardingChecklist(
                    "Get started",
                    steps: [
                        MonarchOnboardingStep(id: "profile", title: "Complete your profile", isDone: true),
                        MonarchOnboardingStep(
                            id: "invite",
                            title: "Invite your team",
                            description: "Work together on shared projects."
                        ),
                        MonarchOnboardingStep(id: "connect", title: "Connect a calendar"),
                    ],
                    description: "A few steps to set up your workspace.",
                    onDismiss: {},
                    action: { _ in
                        Button("Start") {}.buttonStyle(.monarch(.secondary, size: .sm))
                    }
                )
                MonarchOnboardingChecklist(
                    "Get started",
                    steps: [MonarchOnboardingStep(id: "a", title: "Done step", isDone: true)],
                    isCollapsible: false
                )
            }
        }
    }
#endif
