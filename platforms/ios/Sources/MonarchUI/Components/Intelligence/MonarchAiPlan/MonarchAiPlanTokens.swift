import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchAiPlan``.
enum MonarchAiPlanTokens {
    /// The gap between the header, the steps and the footer.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title and the description.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between steps.
    static let stepSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the parts of a step row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the footer buttons.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol of the add button.
    static let addSymbol = "plus"
    /// The SF Symbol of a step's remove button.
    static let removeSymbol = "xmark"
    /// The colour role of the title.
    static var title: MonarchColorRole { \.textDefault }
    /// The colour role of the description, the step numbers and the empty-plan note.
    static var muted: MonarchColorRole { \.textMuted }
    /// The colour role of a step row's background while it is dragged over.
    static var dropTarget: MonarchColorRole { \.aiSubtle }

    /// Whether the plan is read-only: once decided, or when it may not be edited.
    static func isReadOnly(decision: MonarchAiPlanDecision, isEditable: Bool) -> Bool {
        decision != .pending || !isEditable
    }

    /// Whether Approve and Reject are shown: while pending, when the app
    /// listens for a decision or leaves the decision to the view.
    static func showsDecision(decision: MonarchAiPlanDecision, isControlled: Bool, hasHandlers: Bool) -> Bool {
        decision == .pending && (hasHandlers || !isControlled)
    }

    /// The steps that can be approved: labels trimmed, empty steps dropped.
    static func cleaned(_ steps: [MonarchAiPlanStep]) -> [MonarchAiPlanStep] {
        steps.compactMap { step in
            let label = step.label.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !label.isEmpty else { return nil }
            var copy = step
            copy.label = label
            return copy
        }
    }

    /// The name of a step in spoken names: its label, or "step 2" when empty.
    static func name(of step: MonarchAiPlanStep, at index: Int, labels: MonarchAiPlanLabels) -> String {
        let label = step.label.trimmingCharacters(in: .whitespacesAndNewlines)
        guard label.isEmpty else { return label }
        return labels.untitledStep?(index + 1) ?? MonarchAiPlanStrings.untitledStep(index + 1)
    }

    /// The text under the title for a decision.
    static func description(decision: MonarchAiPlanDecision, isReadOnly: Bool, labels: MonarchAiPlanLabels) -> String {
        switch decision {
        case .approved: return labels.approvedNotice ?? MonarchAiPlanStrings.approvedNotice
        case .rejected: return labels.rejectedNotice ?? MonarchAiPlanStrings.rejectedNotice
        case .pending:
            let base = labels.description ?? MonarchAiPlanStrings.description
            return isReadOnly ? base : "\(base) \(labels.editHint ?? MonarchAiPlanStrings.editHint)"
        }
    }

    /// The announcement for a new decision, or `nil` for pending.
    static func announcement(for decision: MonarchAiPlanDecision, labels: MonarchAiPlanLabels) -> String? {
        switch decision {
        case .pending: nil
        case .approved: labels.approvedNotice ?? MonarchAiPlanStrings.approvedNotice
        case .rejected: labels.rejectedNotice ?? MonarchAiPlanStrings.rejectedNotice
        }
    }

    /// The steps the read-only list shows: the draft while pending, the
    /// cleaned steps once decided.
    static func shownSteps(_ draft: [MonarchAiPlanStep], decision: MonarchAiPlanDecision) -> [MonarchAiPlanStep] {
        decision == .pending ? draft : cleaned(draft)
    }

    /// The steps with the step `id` moved one place up or down, and its new
    /// index. `nil` when it cannot move that way.
    static func moving(
        _ steps: [MonarchAiPlanStep],
        id: String,
        direction: MonarchDragHandleDirection
    ) -> (steps: [MonarchAiPlanStep], index: Int)? {
        guard let index = steps.firstIndex(where: { $0.id == id }) else { return nil }
        let target: Int
        switch direction {
        case .up, .backward: target = index - 1
        case .down, .forward: target = index + 1
        }
        guard steps.indices.contains(target) else { return nil }
        var next = steps
        next.swapAt(index, target)
        return (next, target)
    }

    /// The steps with the step `id` dropped onto the step `targetID`: it takes
    /// the target's place. `nil` when nothing moves.
    static func dropping(
        _ steps: [MonarchAiPlanStep],
        id: String,
        onto targetID: String
    ) -> (steps: [MonarchAiPlanStep], index: Int)? {
        guard id != targetID,
            let from = steps.firstIndex(where: { $0.id == id }),
            let to = steps.firstIndex(where: { $0.id == targetID })
        else { return nil }
        var next = steps
        let step = next.remove(at: from)
        next.insert(step, at: to)
        return (next, to)
    }

    /// The steps with an empty step inserted after `index` (at the start when
    /// `index` is -1).
    static func inserting(_ steps: [MonarchAiPlanStep], newID: String, after index: Int) -> [MonarchAiPlanStep] {
        var next = steps
        next.insert(MonarchAiPlanStep(id: newID, label: ""), at: min(max(0, index + 1), next.count))
        return next
    }

    /// A step id for a new step that no step uses yet.
    static func newID(counter: Int, existing: [MonarchAiPlanStep]) -> String {
        var number = counter
        let ids = Set(existing.map(\.id))
        while ids.contains("new-\(number)") { number += 1 }
        return "new-\(number)"
    }
}
