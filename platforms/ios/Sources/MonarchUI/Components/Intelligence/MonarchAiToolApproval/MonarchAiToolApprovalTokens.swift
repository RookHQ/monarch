import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiToolApproval``.
enum MonarchAiToolApprovalTokens {
    /// The gap between the parts of the card.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap inside a group, such as a key and its value.
    static let tightSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between buttons.
    static let buttonSpacing: CGFloat = MonarchTokens.Space.space2
    /// The size of every button.
    static let buttonSize: MonarchButtonSize = .sm
    /// The warning symbol of the header and the consequence line.
    static let warningSymbol = "exclamationmark.triangle"
    /// The risk when neither the card nor the call names one.
    static let defaultRisk: MonarchAiToolCallRisk = .medium
    /// The eyebrow and the keys.
    static var secondaryText: MonarchColorRole { \.textMuted }

    /// The outcome of a call that is no longer waiting.
    enum Outcome: Hashable { case approved, denied, cancelled }

    /// What approving with the editor open gives.
    enum EditedInput: Equatable {
        /// The arguments were not changed: approve without new input.
        case unchanged
        /// The arguments were changed into this value.
        case changed(MonarchAiJSON)
        /// The edited text is not JSON.
        case invalid
    }

    /// One argument as a readable pair.
    struct Argument: Hashable {
        let key: String
        let label: String
        let value: String
    }

    // MARK: Risk

    /// The risk used: the card's, else the call's, else medium.
    static func risk(_ risk: MonarchAiToolCallRisk?, part: MonarchAiToolCallPart) -> MonarchAiToolCallRisk {
        risk ?? part.risk ?? defaultRisk
    }

    /// The tone of the risk badge and the header icon.
    static func tone(_ risk: MonarchAiToolCallRisk) -> MonarchTone {
        switch risk {
        case .low: .neutral
        case .medium, .high: .warning
        case .destructive: .danger
        }
    }

    /// The colour of the header icon and the consequence line.
    static func accent(_ risk: MonarchAiToolCallRisk) -> MonarchColorRole {
        switch risk {
        case .low: \.textMuted
        case .medium, .high: \.warningStrong
        case .destructive: \.dangerStrong
        }
    }

    /// The badge text of a risk.
    static func riskName(_ risk: MonarchAiToolCallRisk, labels: MonarchAiToolApprovalLabels) -> String {
        labels.risk?(risk) ?? MonarchAiToolApprovalStrings.risk(risk)
    }

    /// The consequence line: the card's, else the labels', else the built-in one (destructive only).
    static func consequence(
        _ consequence: String?,
        risk: MonarchAiToolCallRisk,
        labels: MonarchAiToolApprovalLabels
    ) -> String? {
        if let consequence, !consequence.isEmpty { return consequence }
        let text = labels.consequence.map { $0(risk) } ?? MonarchAiToolApprovalStrings.consequence(risk)
        return text?.isEmpty == false ? text : nil
    }

    /// Whether "Always allow this tool" is offered: never for a destructive call.
    static func offersAlwaysAllow(hasCallback: Bool, risk: MonarchAiToolCallRisk) -> Bool {
        hasCallback && risk != .destructive
    }

    /// The approve button's variant: danger for a destructive call.
    static func approveVariant(_ risk: MonarchAiToolCallRisk) -> MonarchButtonVariant {
        risk == .destructive ? .danger : .primary
    }

    /// Whether VoiceOver focus goes to Deny rather than Approve.
    static func focusesDeny(_ risk: MonarchAiToolCallRisk) -> Bool {
        risk == .high || risk == .destructive
    }

    // MARK: Title and state

    /// The title: the summary, else the tool's label.
    static func title(summary: String?, part: MonarchAiToolCallPart, toolLabels: [String: String]) -> String {
        if let summary, !summary.isEmpty { return summary }
        return MonarchAiToolCallTokens.toolLabel(part.name, toolLabels: toolLabels)
    }

    /// Whether the decision is made: the call no longer waits, or someone decided here.
    static func isResolved(_ state: MonarchAiToolCallState, decision: Outcome?) -> Bool {
        state != .pendingApproval || decision != nil
    }

    /// The outcome shown once resolved: the decision made here, else what the state says.
    static func outcome(_ state: MonarchAiToolCallState, decision: Outcome?) -> Outcome {
        if let decision { return decision }
        switch state {
        case .denied: return .denied
        case .cancelled: return .cancelled
        default: return .approved
        }
    }

    /// The text of an outcome.
    static func outcomeText(_ outcome: Outcome, labels: MonarchAiToolApprovalLabels) -> String {
        switch outcome {
        case .approved: labels.approved ?? MonarchAiToolApprovalStrings.approved
        case .denied: labels.denied ?? MonarchAiToolApprovalStrings.denied
        case .cancelled: labels.cancelled ?? MonarchAiToolApprovalStrings.cancelled
        }
    }

    /// The symbol of an outcome, so it is not told by colour alone.
    static func outcomeSymbol(_ outcome: Outcome) -> String {
        switch outcome {
        case .approved: "checkmark.circle"
        case .denied: "xmark.circle"
        case .cancelled: "minus.circle"
        }
    }

    /// The colour of an outcome.
    static func outcomeColor(_ outcome: Outcome) -> MonarchColorRole {
        switch outcome {
        case .approved: \.successStrong
        case .denied: \.dangerStrong
        case .cancelled: \.textMuted
        }
    }

    /// The announcement when the card appears.
    static func announcement(_ title: String, labels: MonarchAiToolApprovalLabels) -> String {
        labels.announce?(title) ?? MonarchAiToolApprovalStrings.announce(title)
    }

    /// What VoiceOver reads for the header: the eyebrow, the title and the risk.
    static func headerLabel(eyebrow: String, title: String, risk: String) -> String {
        [eyebrow, title, risk].joined(separator: ", ")
    }

    // MARK: Arguments

    /// A value as readable text: strings as they are, numbers and booleans
    /// as text, flat arrays joined with commas, anything else as JSON, null as a dash.
    static func readable(_ value: MonarchAiJSON) -> String {
        switch value {
        case .null: return "—"
        case .string(let text): return text
        case .bool(let flag): return flag ? "true" : "false"
        case .number(let number):
            return number.formatted(.number.grouping(.never).locale(Locale(identifier: "en_US_POSIX")))
        case .array(let items):
            let isFlat = items.allSatisfy {
                switch $0 {
                case .array, .object: false
                default: true
                }
            }
            if isFlat { return items.map(readable).joined(separator: ", ") }
            return MonarchAiToolCallTokens.formatted(value).text
        case .object: return MonarchAiToolCallTokens.formatted(value).text
        }
    }

    /// The arguments as readable pairs, when the input is an object; `nil` when it is not. The keys in `order`
    /// come first in that order, the others follow sorted by key.
    static func arguments(
        _ input: MonarchAiJSON,
        argumentLabels: [String: String],
        order: [String] = []
    ) -> [Argument]? {
        guard case .object(let members) = input else { return nil }
        let first = order.filter { members[$0] != nil }
        let rest = members.keys.filter { !first.contains($0) }.sorted()
        return (first + rest).map { key in
            Argument(
                key: key,
                label: argumentLabels[key] ?? MonarchAiToolCallTokens.humanizedName(key),
                value: readable(members[key] ?? .null)
            )
        }
    }

    /// Whether there is nothing to show: no input, or an empty object.
    static func hasNoArguments(_ input: MonarchAiJSON) -> Bool {
        switch input {
        case .null: true
        case .object(let members): members.isEmpty
        default: false
        }
    }

    /// The input as the editor's text: pretty JSON with sorted keys.
    static func editorText(_ input: MonarchAiJSON) -> String {
        MonarchAiToolCallTokens.formatted(input).text
    }

    /// What approving with the edited text gives.
    static func edited(_ text: String, original: String) -> EditedInput {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed == original.trimmingCharacters(in: .whitespacesAndNewlines) { return .unchanged }
        guard let data = trimmed.data(using: .utf8),
            let value = try? JSONDecoder().decode(MonarchAiJSON.self, from: data)
        else { return .invalid }
        return .changed(value)
    }
}
