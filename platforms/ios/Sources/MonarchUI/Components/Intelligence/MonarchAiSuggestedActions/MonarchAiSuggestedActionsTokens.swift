import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiSuggestedActions``.
enum MonarchAiSuggestedActionsTokens {
    /// The gap between the heading and the rows.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap inside a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the tags and between the buttons.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space2
    /// The vertical padding of a row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space3
    /// The SF Symbols of the list.
    static let aiSymbol = "cpu"
    static let ruleSymbol = "list.bullet.rectangle"
    static let approvalSymbol = "lock"
    /// The warning triangle in a risk tag, so the risk does not rest on colour alone.
    static let riskSymbol = "exclamationmark.triangle"
    static let doneSymbol = "checkmark.circle"
    static let failedSymbol = "exclamationmark.circle"
    static let dismissSymbol = "xmark"

    /// The status shown: the action's own, else the one the list tracked, else ready.
    static func status(
        _ action: MonarchAiSuggestedAction,
        tracked: [String: MonarchAiSuggestedActionStatus]
    ) -> MonarchAiSuggestedActionStatus {
        action.status ?? tracked[action.id] ?? .ready
    }

    /// The origin label.
    static func originText(_ origin: MonarchAiSuggestedActionsOrigin, labels: MonarchAiSuggestedActionsLabels) -> String
    {
        switch origin {
        case .ai: labels.originAi ?? MonarchAiSuggestedActionsStrings.originAi
        case .rule: labels.originRule ?? MonarchAiSuggestedActionsStrings.originRule
        }
    }

    /// The origin symbol.
    static func originSymbol(_ origin: MonarchAiSuggestedActionsOrigin) -> String {
        origin == .ai ? aiSymbol : ruleSymbol
    }

    /// The origin colour: the assistant colour for AI, muted for rules.
    static func originColor(_ origin: MonarchAiSuggestedActionsOrigin) -> MonarchColorRole {
        origin == .ai ? \.aiStrong : \.textMuted
    }

    /// The risk tag's text and tone, or `nil` for low risk.
    static func riskTag(
        _ risk: MonarchAiToolCallRisk,
        labels: MonarchAiSuggestedActionsLabels
    ) -> (text: String, tone: MonarchTone)? {
        guard let text = labels.risk?(risk) ?? MonarchAiSuggestedActionsStrings.risk(risk) else { return nil }
        switch risk {
        case .low: return nil
        case .medium, .high: return (text, .warning)
        case .destructive: return (text, .danger)
        }
    }

    /// The spoken name of the run button.
    static func runLabel(_ action: MonarchAiSuggestedAction, labels: MonarchAiSuggestedActionsLabels) -> String {
        labels.runAction?(action.label) ?? MonarchAiSuggestedActionsStrings.runAction(action.label)
    }

    /// The spoken name of the dismiss button.
    static func dismissLabel(_ action: MonarchAiSuggestedAction, labels: MonarchAiSuggestedActionsLabels) -> String {
        labels.dismiss?(action.label) ?? MonarchAiSuggestedActionsStrings.dismiss(action.label)
    }

    /// The approval dialog's title.
    static func approvalTitle(_ action: MonarchAiSuggestedAction, labels: MonarchAiSuggestedActionsLabels) -> String {
        labels.approvalTitle?(action.label) ?? MonarchAiSuggestedActionsStrings.approvalTitle(action.label)
    }

    /// The approval dialog's text: the description, or the built-in text,
    /// then each consequence on its own line.
    static func approvalMessage(_ action: MonarchAiSuggestedAction, labels: MonarchAiSuggestedActionsLabels) -> String {
        let lead =
            action.description.flatMap { $0.isEmpty ? nil : $0 }
            ?? labels.approvalDescription ?? MonarchAiSuggestedActionsStrings.approvalDescription
        return ([lead] + action.consequences.map { "• \($0)" }).joined(separator: "\n")
    }

    /// The approval dialog's confirm button.
    static func confirmTitle(_ action: MonarchAiSuggestedAction, labels: MonarchAiSuggestedActionsLabels) -> String {
        action.confirmLabel ?? labels.approve ?? MonarchAiSuggestedActionsStrings.approve
    }

    /// The role of the confirm button: destructive for a destructive action.
    static func confirmRole(_ action: MonarchAiSuggestedAction) -> ButtonRole? {
        action.risk == .destructive ? .destructive : nil
    }

    /// The announcement after a run.
    static func doneAnnouncement(_ action: MonarchAiSuggestedAction, labels: MonarchAiSuggestedActionsLabels) -> String
    {
        labels.doneAnnouncement?(action.label) ?? MonarchAiSuggestedActionsStrings.doneAnnouncement(action.label)
    }

    /// The colour of an action's label: muted once it is done.
    static func labelColor(_ status: MonarchAiSuggestedActionStatus) -> MonarchColorRole {
        status == .done ? \.textMuted : \.textDefault
    }

    /// Whether Run can be pressed: not while running and not after it is done.
    static func canRun(_ status: MonarchAiSuggestedActionStatus) -> Bool {
        status == .ready || status == .failed
    }
}
