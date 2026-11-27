import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchAiConversationHeader``.
enum MonarchAiConversationHeaderTokens {
    /// The gap between the title line and the agent and model line.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the parts of a line.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space1
    /// The most lines the title takes before it is truncated.
    static let titleLineLimit = 2
    /// The SF Symbol after the title when it opens a menu.
    static let menuSymbol = "ellipsis"
    /// The side of the agent's tile beside its name.
    static let agentTile: CGFloat = MonarchTokens.Space.space6
    /// The SF Symbol of the rename menu item.
    static let renameSymbol = "pencil"
    /// The separator between the agent and the model.
    static let separator = " · "

    /// The roles of the two lines.
    static var titleRole: MonarchColorRole { \.textDefault }
    /// The role of the agent and model line, and of the naming placeholder.
    static var secondaryRole: MonarchColorRole { \.textMuted }

    /// The visible title: the generating placeholder, a rename on its way,
    /// the trimmed title, or "New conversation" when it is empty.
    static func displayTitle(
        _ title: String,
        isNaming: Bool,
        pendingTitle: String?,
        labels: MonarchAiConversationHeaderLabels
    ) -> String {
        if isNaming { return labels.naming ?? MonarchAiConversationHeaderStrings.naming }
        if let pendingTitle { return pendingTitle }
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? (labels.untitled ?? MonarchAiConversationHeaderStrings.untitled) : trimmed
    }

    /// The title to save from a draft: trimmed, or nil when it is empty or unchanged.
    static func renamedTitle(_ draft: String, current: String) -> String? {
        let next = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        let now = current.trimmingCharacters(in: .whitespacesAndNewlines)
        return next.isEmpty || next == now ? nil : next
    }

    /// The visible line under the title, such as "Research agent · Fast model", or nil.
    static func subtitle(agent: MonarchAiAgentRef?, model: String?) -> String? {
        let parts = [agent?.name, model]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        return parts.isEmpty ? nil : parts.joined(separator: separator)
    }

    /// What VoiceOver reads after the title: "AI assistant: Research agent. Model: Fast model".
    static func accessibilityValue(
        agent: MonarchAiAgentRef?,
        model: String?,
        labels: MonarchAiConversationHeaderLabels
    ) -> String {
        var parts: [String] = []
        if let name = agent?.name.trimmingCharacters(in: .whitespacesAndNewlines), !name.isEmpty {
            parts.append("\(labels.agentPrefix ?? MonarchAiConversationHeaderStrings.agentPrefix): \(name)")
        }
        if let model = model?.trimmingCharacters(in: .whitespacesAndNewlines), !model.isEmpty {
            parts.append("\(labels.modelPrefix ?? MonarchAiConversationHeaderStrings.modelPrefix): \(model)")
        }
        return parts.joined(separator: ". ")
    }

    /// Whether the title opens a menu: there is something to rename or an app action.
    static func hasMenu(canRename: Bool, hasActions: Bool) -> Bool {
        canRename || hasActions
    }
}
