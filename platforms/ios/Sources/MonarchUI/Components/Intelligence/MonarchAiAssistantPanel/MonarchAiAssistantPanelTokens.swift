import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchAiAssistantPanel``.
enum MonarchAiAssistantPanelTokens {
    /// The padding around the header, the welcome and the input area.
    static let padding: CGFloat = MonarchTokens.Space.space4
    /// The gap between header items and around the context chips.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the title and the agent line.
    static let titleSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The height of the pane in the preview.
    static let previewHeight: CGFloat = MonarchTokens.Space.space24 * 4
    /// The SF Symbol of the close button.
    static let closeSymbol = "xmark"
    /// The SF Symbols of the collapse button: down while the panel is open, up while it is collapsed.
    static let collapseSymbol = "chevron.down"
    static let expandSymbol = "chevron.up"

    /// The background of the panel and of the input area.
    static var backgroundRole: MonarchColorRole { \.bgSurface }
    /// The title.
    static var titleRole: MonarchColorRole { \.textDefault }
    /// The agent line under the title.
    static var secondaryRole: MonarchColorRole { \.textMuted }

    /// The placement to draw: an explicit placement wins, then the one the
    /// presenting modifier set, then the width.
    static func resolvedPlacement(
        _ placement: MonarchAiAssistantPanelPlacement,
        presentedAs: MonarchAiAssistantPanelPlacement?,
        isCompactWidth: Bool
    ) -> MonarchAiAssistantPanelPlacement {
        if placement != .automatic { return placement }
        if let presentedAs, presentedAs != .automatic { return presentedAs }
        return isCompactWidth ? .sheet : .inline
    }

    /// Whether a reply is running, so Send becomes Stop.
    static func isBusy(_ status: MonarchAiConversationStatus) -> Bool {
        status == .submitted || status == .streaming
    }

    /// Whether the starters are disabled: while a reply runs or a tool call waits for approval.
    static func areStartersDisabled(_ status: MonarchAiConversationStatus) -> Bool {
        isBusy(status) || status == .awaitingApproval
    }

    /// The avatar's state: working while a reply runs.
    static func avatarState(_ status: MonarchAiConversationStatus) -> MonarchAiAgentState {
        isBusy(status) ? .working : .idle
    }

    /// Whether the welcome and the starters show instead of the thread.
    static func showsWelcome(messageCount: Int) -> Bool {
        messageCount == 0
    }

    /// The agent in the header and the welcome; the built-in name stands in without one.
    static func headerAgent(_ agent: MonarchAiAgentRef?, labels: MonarchAiAssistantPanelLabels) -> MonarchAiAgentRef {
        agent ?? MonarchAiAgentRef(name: labels.assistant ?? MonarchAiAssistantPanelStrings.assistant)
    }

    /// The line under the title: the agent's name and role, or nothing without an agent.
    static func subtitle(_ agent: MonarchAiAgentRef?) -> String? {
        guard let agent else { return nil }
        let parts = [agent.name, agent.role ?? ""]
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    /// Whether Escape closes the panel: only while no reply runs, because then it stops the reply.
    static func closesOnEscape(_ status: MonarchAiConversationStatus) -> Bool {
        !isBusy(status)
    }
}
