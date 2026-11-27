import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchApprovalChainEditor``.
enum MonarchApprovalChainEditorTokens {
    /// The gap between steps and around the list.
    static let listSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the fields of a step.
    static let stepSpacing: CGFloat = MonarchTokens.Space.space3
    /// The longest step label, as on the web.
    static let maxLabelLength = 60
    /// The add button's symbol.
    static let addSymbol = "plus"
    /// The remove button's symbol.
    static let removeSymbol = "xmark"
    /// The reorder menu's symbol.
    static let reorderSymbol = "arrow.up.arrow.down"
    /// Muted text: the description and the max note.
    static var mutedText: MonarchColorRole { \.textMuted }

    /// The approver type of a step.
    static func type(of step: MonarchApprovalChainStep, in types: [MonarchApprovalApproverType])
        -> MonarchApprovalApproverType?
    {
        types.first { $0.id == step.approver }
    }

    /// The ids of steps whose approver type needs a target and has none of its options chosen.
    static func invalidSteps(_ steps: [MonarchApprovalChainStep], types: [MonarchApprovalApproverType]) -> Set<String> {
        Set(
            steps.filter { step in
                guard let targets = type(of: step, in: types)?.targets else { return false }
                return !targets.contains { $0.id == step.target }
            }
            .map(\.id)
        )
    }

    /// One step in words: "Finance check (Role: Finance team)", "Reporting manager".
    static func stepSummary(_ step: MonarchApprovalChainStep, types: [MonarchApprovalApproverType]) -> String {
        let type = type(of: step, in: types)
        let target = type?.targets?.first { $0.id == step.target }?.label
        let base = type.map { type in target.map { "\(type.label): \($0)" } ?? type.label } ?? step.approver
        if let label = step.label, !label.isEmpty { return "\(label) (\(base))" }
        return base
    }

    /// The chain in words, steps joined by `separator`.
    static func summary(
        _ steps: [MonarchApprovalChainStep],
        types: [MonarchApprovalApproverType],
        separator: String
    ) -> String {
        steps.map { stepSummary($0, types: types) }.joined(separator: separator)
    }

    /// A step after its approver changes: a target the new type does not offer is dropped.
    static func changingApprover(
        _ step: MonarchApprovalChainStep,
        to approver: String,
        types: [MonarchApprovalApproverType]
    ) -> MonarchApprovalChainStep {
        var next = step
        next.approver = approver
        let targets = types.first { $0.id == approver }?.targets
        if let target = step.target, targets?.contains(where: { $0.id == target }) == true {
            next.target = target
        } else {
            next.target = nil
        }
        return next
    }

    /// A step label as typed: cut to the longest length; empty becomes `nil`.
    static func cleanedLabel(_ text: String) -> String? {
        let cut = String(text.prefix(maxLabelLength))
        return cut.isEmpty ? nil : cut
    }

    /// The steps with one moved by `offset` places, or unchanged at the ends.
    static func moving(_ steps: [MonarchApprovalChainStep], id: String, by offset: Int) -> [MonarchApprovalChainStep] {
        guard let index = steps.firstIndex(where: { $0.id == id }) else { return steps }
        let target = index + offset
        guard steps.indices.contains(target) else { return steps }
        var next = steps
        next.swapAt(index, target)
        return next
    }

    /// Whether a step can be removed: only above the fewest steps.
    static func canRemove(count: Int, minSteps: Int) -> Bool { count > minSteps }

    /// Whether a step can be added: only below the most steps and with an approver type.
    static func canAdd(count: Int, maxSteps: Int) -> Bool { count < maxSteps }

    /// The new step: the app's, else the first approver type with the given id.
    static func newStep(types: [MonarchApprovalApproverType], id: String) -> MonarchApprovalChainStep {
        MonarchApprovalChainStep(id: id, approver: types.first?.id ?? "")
    }

    /// What VoiceOver reads for a step: "Step 1, Finance check (Role: Finance team)".
    static func accessibilityLabel(
        position: Int,
        step: MonarchApprovalChainStep,
        types: [MonarchApprovalApproverType],
        labels: MonarchApprovalChainEditorLabels
    ) -> String {
        "\(stepTitle(position, labels: labels)), \(stepSummary(step, types: types))"
    }

    /// The title of a step.
    static func stepTitle(_ position: Int, labels: MonarchApprovalChainEditorLabels) -> String {
        labels.step?(position) ?? MonarchApprovalChainEditorStrings.step(position)
    }
}
