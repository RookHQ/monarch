import MonarchTokens
import SwiftUI

/// The plan an agent proposes before it acts, for a person to review: they
/// can edit, reorder, add and remove steps, then approve or reject it. Once
/// approved it becomes the live, read-only list of the steps.
///
/// ```swift
/// MonarchAiPlan(
///     plan,
///     onReject: { agent.cancel() },
///     onApprove: { steps in agent.run(steps) }
/// )
/// ```
///
/// It edits a ``MonarchAiPlanPart`` from the message model. Each step is a
/// ``MonarchInlineEditText``; the row's ``MonarchDragHandle`` drags it onto
/// another step and offers Move up and Move down to VoiceOver and Switch
/// Control. A new plan part from the model replaces the draft. After a
/// decision, or with `isEditable` off, the steps show as a
/// ``MonarchAiChainOfSteps`` whose states follow the part.
///
/// Accessibility: the plan is a container named by its title. Each step's
/// text is a button named "Step 2" whose value is the step; the handle is
/// named "Reorder Draft the reply" and the remove button "Remove Draft the
/// reply". Moves, removals and the decision are announced. Approve is
/// disabled while every step is empty, and a note says why. Every control
/// has a 44-point touch target and the steps wrap at every text size.
@_spi(Experimental) public struct MonarchAiPlan: View {
    private let part: MonarchAiPlanPart
    private let decision: MonarchAiPlanDecision?
    private let isEditable: Bool
    private let labels: MonarchAiPlanLabels
    private let onChange: (@MainActor ([MonarchAiPlanStep]) -> Void)?
    private let onReject: (@MainActor () -> Void)?
    private let onApprove: (@MainActor ([MonarchAiPlanStep]) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @State private var draft: [MonarchAiPlanStep]
    @State private var emitted: [MonarchAiPlanStep]
    @State private var ownDecision = MonarchAiPlanDecision.pending
    @State private var counter = 0
    @State private var newStepID: String?
    @State private var dropTargetID: String?

    /// A plan to review.
    ///
    /// - Parameters:
    ///   - part: The plan part the agent proposed. Its steps seed the draft.
    ///   - decision: The decision, when the app owns it, for example from the
    ///     conversation state. When `nil` the view keeps its own after
    ///     Approve or Reject.
    ///   - isEditable: Whether the person can change the steps before deciding.
    ///     Once decided the plan is always read-only.
    ///   - labels: Replacements for the built-in strings.
    ///   - onChange: Called with the draft after every edit.
    ///   - onReject: Called when the person rejects the plan.
    ///   - onApprove: Called with the approved steps, trimmed, empty steps left out.
    public init(
        _ part: MonarchAiPlanPart,
        decision: MonarchAiPlanDecision? = nil,
        isEditable: Bool = true,
        labels: MonarchAiPlanLabels = MonarchAiPlanLabels(),
        onChange: (@MainActor ([MonarchAiPlanStep]) -> Void)? = nil,
        onReject: (@MainActor () -> Void)? = nil,
        onApprove: (@MainActor ([MonarchAiPlanStep]) -> Void)? = nil
    ) {
        self.part = part
        self.decision = decision
        self.isEditable = isEditable
        self.labels = labels
        self.onChange = onChange
        self.onReject = onReject
        self.onApprove = onApprove
        _draft = State(initialValue: part.steps)
        _emitted = State(initialValue: part.steps)
    }

    private var currentDecision: MonarchAiPlanDecision { decision ?? ownDecision }
    private var title: String { labels.title ?? MonarchAiPlanStrings.title }

    /// The content of the view.
    public var body: some View {
        let decided = currentDecision
        let isReadOnly = MonarchAiPlanTokens.isReadOnly(decision: decided, isEditable: isEditable)
        MonarchCard {
            VStack(alignment: .leading, spacing: MonarchAiPlanTokens.sectionSpacing) {
                VStack(alignment: .leading, spacing: MonarchAiPlanTokens.headerSpacing) {
                    Text(title)
                        .monarchTextStyle(.subheading)
                        .foregroundStyle(theme.colors[role: MonarchAiPlanTokens.title])
                        .accessibilityAddTraits(.isHeader)
                    Text(MonarchAiPlanTokens.description(decision: decided, isReadOnly: isReadOnly, labels: labels))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: MonarchAiPlanTokens.muted])
                }
                if isReadOnly {
                    MonarchAiChainOfSteps(
                        MonarchAiPlanTokens.shownSteps(draft, decision: decided),
                        isCollapsible: false,
                        announces: decided == .approved,
                        labels: MonarchAiChainOfStepsLabels(label: title)
                    )
                } else {
                    editor
                }
                if MonarchAiPlanTokens.showsDecision(
                    decision: decided,
                    isControlled: decision != nil,
                    hasHandlers: onApprove != nil || onReject != nil
                ) {
                    footer
                }
            }
        }
        .monarchCardOverride(.aiBorder)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title)
        .onChange(of: part.steps) { _, steps in
            // A new plan from the model, not an echo of the draft, replaces it.
            guard steps != emitted else { return }
            emitted = steps
            draft = steps
        }
        .onChange(of: currentDecision) { _, newDecision in
            if let text = MonarchAiPlanTokens.announcement(for: newDecision, labels: labels) {
                AccessibilityNotification.Announcement(text).post()
            }
        }
    }

    private var editor: some View {
        VStack(alignment: .leading, spacing: MonarchAiPlanTokens.stepSpacing) {
            ForEach(Array(draft.enumerated()), id: \.element.id) { index, step in
                stepRow(step, index: index)
            }
            MonarchButton(
                labels.addStep ?? MonarchAiPlanStrings.addStep,
                systemImage: MonarchAiPlanTokens.addSymbol,
                variant: .ghost,
                size: .sm
            ) {
                addStep()
            }
        }
    }

    private func stepRow(_ step: MonarchAiPlanStep, index: Int) -> some View {
        let name = MonarchAiPlanTokens.name(of: step, at: index, labels: labels)
        let handle = MonarchDragHandle(labels.reorder?(name) ?? MonarchAiPlanStrings.reorder(name)) { direction in
            apply(MonarchAiPlanTokens.moving(draft, id: step.id, direction: direction), name: name)
        }
        let number = Text("\(index + 1).")
            .monarchTextStyle(.label)
            .monarchTabularNumber()
            .foregroundStyle(theme.colors[role: MonarchAiPlanTokens.muted])
        let field = MonarchTextField(
            labels.stepLabel?(index + 1) ?? MonarchAiPlanStrings.stepLabel(index + 1),
            text: labelBinding(for: step.id),
            prompt: labels.stepPlaceholder ?? MonarchAiPlanStrings.stepPlaceholder,
            axis: .vertical,
            lineLimit: 1...4,
            labelVisibility: .hidden
        )
        let actions = HStack(spacing: 0) {
            Button {
                remove(step, name: name)
            } label: {
                Label(
                    labels.remove?(name) ?? MonarchAiPlanStrings.remove(name),
                    systemImage: MonarchAiPlanTokens.removeSymbol
                )
                .labelStyle(.iconOnly)
            }
            .buttonStyle(.monarch(.ghost, size: .sm))
            moveButton(.up, step: step, index: index, name: name)
            moveButton(.down, step: step, index: index, name: name)
        }
        // The step, its number and its actions share a row; on a width that would squeeze the text into a few
        // characters, the actions go on a line of their own under it.
        return ViewThatFits(in: .horizontal) {
            HStack(alignment: .center, spacing: MonarchAiPlanTokens.rowSpacing) {
                handle.draggable(step.id)
                number.accessibilityHidden(true)
                field.frame(minWidth: 180, idealWidth: 180, maxWidth: .infinity, alignment: .leading)
                actions
            }
            VStack(alignment: .trailing, spacing: 0) {
                HStack(alignment: .center, spacing: MonarchAiPlanTokens.rowSpacing) {
                    handle.draggable(step.id)
                    number.accessibilityHidden(true)
                    field.frame(maxWidth: .infinity, alignment: .leading)
                }
                actions
            }
        }
        .padding(.horizontal, MonarchTokens.Space.space2)
        .padding(.vertical, MonarchTokens.Space.space1)
        .overlay(
            MonarchShapes.control.strokeBorder(theme.colors.borderSubtle, lineWidth: MonarchShapes.borderWidth)
        )
        .background(
            MonarchShapes.control.fill(
                dropTargetID == step.id ? theme.colors[role: MonarchAiPlanTokens.dropTarget] : .clear
            )
        )
        .dropDestination(
            for: String.self,
            action: { ids, _ in
                guard let id = ids.first, let moved = MonarchAiPlanTokens.dropping(draft, id: id, onto: step.id) else {
                    return false
                }
                let movedName = draft.firstIndex { $0.id == id }.map {
                    MonarchAiPlanTokens.name(of: draft[$0], at: $0, labels: labels)
                }
                apply(moved, name: movedName ?? name)
                return true
            },
            isTargeted: { isTargeted in
                dropTargetID = isTargeted ? step.id : (dropTargetID == step.id ? nil : dropTargetID)
            }
        )
    }

    /// One place earlier or later, for people who do not drag; it is off at the first or last step.
    private func moveButton(_ direction: MonarchDragHandleDirection, step: MonarchAiPlanStep, index: Int, name: String)
        -> some View
    {
        let isUp = direction == .up
        return Button {
            apply(MonarchAiPlanTokens.moving(draft, id: step.id, direction: direction), name: name)
        } label: {
            Label(
                isUp ? MonarchDragHandleStrings.moveUp : MonarchDragHandleStrings.moveDown,
                systemImage: isUp ? "chevron.up" : "chevron.down"
            )
            .labelStyle(.iconOnly)
        }
        .buttonStyle(.monarch(.ghost, size: .sm))
        .disabled(isUp ? index == 0 : index == draft.count - 1)
    }

    private var footer: some View {
        let approvable = MonarchAiPlanTokens.cleaned(draft)
        return VStack(alignment: .leading, spacing: MonarchAiPlanTokens.actionSpacing) {
            if approvable.isEmpty {
                Text(labels.emptyPlan ?? MonarchAiPlanStrings.emptyPlan)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchAiPlanTokens.muted])
            }
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchAiPlanTokens.actionSpacing) { decisionButtons(approvable) }
                VStack(alignment: .leading, spacing: MonarchAiPlanTokens.actionSpacing) { decisionButtons(approvable) }
            }
            .frame(maxWidth: .infinity, alignment: .trailing)
        }
    }

    @ViewBuilder private func decisionButtons(_ approvable: [MonarchAiPlanStep]) -> some View {
        MonarchButton(labels.reject ?? MonarchAiPlanStrings.reject, variant: .secondary, size: .sm) {
            if decision == nil { ownDecision = .rejected }
            onReject?()
        }
        MonarchButton(labels.approve ?? MonarchAiPlanStrings.approve, variant: .primary, size: .sm) {
            if decision == nil { ownDecision = .approved }
            onApprove?(approvable)
        }
        .disabled(approvable.isEmpty)
    }

    private func labelBinding(for id: String) -> Binding<String> {
        Binding(
            get: { draft.first { $0.id == id }?.label ?? "" },
            set: { newValue in
                guard let index = draft.firstIndex(where: { $0.id == id }) else { return }
                var next = draft
                next[index].label = newValue
                update(next)
            }
        )
    }

    private func update(_ steps: [MonarchAiPlanStep]) {
        draft = steps
        emitted = steps
        onChange?(steps)
    }

    private func apply(_ moved: (steps: [MonarchAiPlanStep], index: Int)?, name: String) {
        guard let moved else { return }
        update(moved.steps)
        let text =
            labels.moved?(name, moved.index + 1, moved.steps.count)
            ?? MonarchAiPlanStrings.moved(name, position: moved.index + 1, total: moved.steps.count)
        AccessibilityNotification.Announcement(text).post()
    }

    private func addStep() {
        counter += 1
        let id = MonarchAiPlanTokens.newID(counter: counter, existing: draft)
        newStepID = id
        update(MonarchAiPlanTokens.inserting(draft, newID: id, after: draft.count - 1))
    }

    private func remove(_ step: MonarchAiPlanStep, name: String) {
        update(draft.filter { $0.id != step.id })
        AccessibilityNotification.Announcement(labels.removed?(name) ?? MonarchAiPlanStrings.removed(name)).post()
    }
}

#if DEBUG
    #Preview("MonarchAiPlan") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiPlan(
                    MonarchAiPlanPart(steps: [
                        MonarchAiPlanStep(id: "find", label: "Find the three open invoices"),
                        MonarchAiPlanStep(id: "draft", label: "Draft a reminder for each customer"),
                        MonarchAiPlanStep(id: "send", label: "Send the reminders"),
                    ])
                )
                MonarchAiPlan(
                    MonarchAiPlanPart(steps: [
                        MonarchAiPlanStep(id: "find", label: "Find the three open invoices", state: .done),
                        MonarchAiPlanStep(id: "draft", label: "Draft a reminder for each customer", state: .active),
                    ]),
                    decision: .approved
                )
            }
        }
    }
#endif
